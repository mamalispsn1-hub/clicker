package com.autoclicker.pro.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityService.ScreenshotResult
import android.accessibilityservice.AccessibilityService.TakeScreenshotCallback
import android.accessibilityservice.GestureDescription
import android.graphics.Bitmap
import android.graphics.Path
import android.os.Build
import android.util.Log
import android.view.Display
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * The only component allowed to dispatch gestures. Kept intentionally "dumb":
 * it does not read screen content (accessibility event stream is unused for
 * automation logic) and only exposes a narrow API — dispatch a tap at (x, y) —
 * to [com.autoclicker.pro.automation.ClickExecutor].
 *
 * Lifecycle: Android binds/unbinds this service based on the user's
 * accessibility settings. We publish connection state via [connectionState] so
 * the rest of the app (ViewModel, overlay) can react — e.g. disable Start
 * until the service is connected — instead of guessing.
 */
class AutoClickAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "AutoClickA11yService"

        private val _connectionState = MutableStateFlow(false)
        val connectionState: StateFlow<Boolean> = _connectionState

        /** Foreground app package name, updated from TYPE_WINDOW_STATE_CHANGED
         * events. Generic OS-level signal (same one launchers/DND-per-app
         * features use) — used only for the optional "auto-switch profile per
         * app" feature; not tied to any specific target app. Null until the
         * first window-state event arrives. */
        private val _foregroundPackage = MutableStateFlow<String?>(null)
        val foregroundPackage: StateFlow<String?> = _foregroundPackage

        /** True on API 30+ where AccessibilityService.takeScreenshot() exists,
         * which the OCR click-condition feature depends on. On older devices
         * the condition feature is disabled (see OcrEngine). */
        val isScreenshotCaptureSupported: Boolean
            get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

        @Volatile
        private var instance: AutoClickAccessibilityService? = null

        fun isServiceEnabled(): Boolean = instance != null && _connectionState.value

        /**
         * Dispatches a single tap at [x], [y] held for [durationMs].
         * Returns true if the gesture was accepted by the system (completed
         * without being cancelled), false otherwise (e.g. service not
         * connected, or the gesture was interrupted).
         */
        suspend fun dispatchTap(x: Float, y: Float, durationMs: Long): Boolean {
            val service = instance ?: return false
            return service.performTap(x, y, durationMs)
        }

        /**
         * Captures the current screen as a [Bitmap] for the OCR click
         * condition. Returns null if unsupported (pre-API 30), the service
         * isn't connected, or the system declines the request (e.g. secure
         * window on screen, or called too frequently — the OS throttles this
         * API, which is exactly why conditions poll at a modest interval
         * rather than every frame).
         */
        suspend fun captureScreenshot(): Bitmap? {
            val service = instance ?: return null
            if (!isScreenshotCaptureSupported) return null
            return service.performScreenshotCapture()
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        _connectionState.value = true
        Log.i(TAG, "Accessibility service connected")
    }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        _connectionState.value = false
        instance = null
        Log.i(TAG, "Accessibility service unbound")
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        _connectionState.value = false
        instance = null
        super.onDestroy()
    }

    // The only accessibility event we act on is the window-state change,
    // purely to expose "what package is currently in the foreground" as a
    // generic signal (see [foregroundPackage]) for the optional per-app
    // profile switch. We deliberately do not read node content/text here —
    // this service does not perform UI scraping of any app.
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            event.packageName?.toString()?.let { _foregroundPackage.value = it }
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "Accessibility service interrupted")
    }

    @androidx.annotation.RequiresApi(Build.VERSION_CODES.R)
    private suspend fun performScreenshotCapture(): Bitmap? =
        suspendCancellableCoroutine { continuation ->
            try {
                takeScreenshot(
                    Display.DEFAULT_DISPLAY,
                    mainExecutor,
                    object : TakeScreenshotCallback {
                        override fun onSuccess(result: ScreenshotResult) {
                            val bitmap = try {
                                Bitmap.wrapHardwareBuffer(result.hardwareBuffer, result.colorSpace)
                            } catch (t: Throwable) {
                                Log.e(TAG, "Failed to wrap screenshot buffer", t)
                                null
                            }
                            result.hardwareBuffer.close()
                            if (continuation.isActive) continuation.resume(bitmap)
                        }

                        override fun onFailure(errorCode: Int) {
                            Log.w(TAG, "takeScreenshot failed, errorCode=$errorCode")
                            if (continuation.isActive) continuation.resume(null)
                        }
                    }
                )
            } catch (t: Throwable) {
                Log.e(TAG, "takeScreenshot threw", t)
                if (continuation.isActive) continuation.resume(null)
            }
        }

    private suspend fun performTap(x: Float, y: Float, durationMs: Long): Boolean =
        suspendCancellableCoroutine { continuation ->
            val safeDuration = durationMs.coerceAtLeast(1L)
            val path = Path().apply { moveTo(x, y) }
            val stroke = GestureDescription.StrokeDescription(path, 0L, safeDuration)
            val gesture = GestureDescription.Builder().addStroke(stroke).build()

            val callback = object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    if (continuation.isActive) continuation.resume(true)
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    if (continuation.isActive) continuation.resume(false)
                }
            }

            val accepted = try {
                dispatchGesture(gesture, callback, null)
            } catch (t: Throwable) {
                Log.e(TAG, "dispatchGesture threw", t)
                false
            }

            if (!accepted && continuation.isActive) {
                continuation.resume(false)
            }
        }
}
