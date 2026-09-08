package com.autoclicker.pro.overlay

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.autoclicker.pro.automation.AutoClickEngine
import com.autoclicker.pro.data.model.ClickPoint
import com.autoclicker.pro.data.model.OcrRegion
import com.autoclicker.pro.data.repository.ProfileRepository
import com.autoclicker.pro.presentation.main.MainActivity
import com.autoclicker.pro.presentation.theme.AutoClickerTheme
import com.autoclicker.pro.service.ClickForegroundService
import com.autoclicker.pro.utils.CoordinateManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToInt

/**
 * Hosts every overlay window: the always-visible [FloatingPanel] and, while
 * the user is placing a point, the draggable [PointMarker]. One service keeps
 * both windows' lifecycles (and the single [OverlayManager]/WindowManager
 * handle) together instead of splitting across services.
 */
@AndroidEntryPoint
class OverlayService : Service() {

    @Inject lateinit var autoClickEngine: AutoClickEngine
    @Inject lateinit var profileRepository: ProfileRepository
    @Inject lateinit var coordinateManager: CoordinateManager

    private lateinit var overlayManager: OverlayManager
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val panelLifecycleOwner = OverlayLifecycleOwner()
    private var panelView: ComposeView? = null
    private var panelParams: android.view.WindowManager.LayoutParams? = null

    private val markerLifecycleOwner = OverlayLifecycleOwner()
    private var markerView: ComposeView? = null

    private val regionPickerLifecycleOwner = OverlayLifecycleOwner()
    private var regionPickerView: ComposeView? = null

    companion object {
        private const val ACTION_SHOW = "com.autoclicker.pro.overlay.SHOW"
        private const val ACTION_HIDE = "com.autoclicker.pro.overlay.HIDE"
        private const val ACTION_PICK_REGION = "com.autoclicker.pro.overlay.PICK_REGION"

        fun show(context: Context) {
            context.startService(Intent(context, OverlayService::class.java).setAction(ACTION_SHOW))
        }

        fun hide(context: Context) {
            context.startService(Intent(context, OverlayService::class.java).setAction(ACTION_HIDE))
        }

        /** Minimizes the app (implicitly, since the overlay sits above
         * whatever is currently on screen) and shows two draggable corner
         * handles the user positions over the text region they want the
         * "wait for text" condition to watch. Result is delivered via
         * [RegionPickerBus] and the caller is expected to bring the app back
         * to the foreground itself (e.g. the user taps the panel or switches
         * back manually) — see [ClickPointEditorScreen]. */
        fun pickRegion(context: Context, requestId: String) {
            context.startService(
                Intent(context, OverlayService::class.java)
                    .setAction(ACTION_PICK_REGION)
                    .putExtra(EXTRA_REQUEST_ID, requestId)
            )
        }

        private const val EXTRA_REQUEST_ID = "request_id"
    }

    override fun onCreate() {
        super.onCreate()
        overlayManager = OverlayManager(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_HIDE -> {
                removeMarker()
                removeRegionPicker()
                removePanel()
                stopSelf()
            }
            ACTION_PICK_REGION -> {
                val requestId = intent.getStringExtra(EXTRA_REQUEST_ID) ?: return START_STICKY
                startForeground(
                    com.autoclicker.pro.service.NotificationHelper.OVERLAY_NOTIF_ID,
                    com.autoclicker.pro.service.NotificationHelper.buildOverlayNotification(this)
                )
                showRegionPicker(requestId)
            }
            else -> {
                startForeground(
                    com.autoclicker.pro.service.NotificationHelper.OVERLAY_NOTIF_ID,
                    com.autoclicker.pro.service.NotificationHelper.buildOverlayNotification(this)
                )
                if (panelView == null) showPanel()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        removeMarker()
        removeRegionPicker()
        removePanel()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ---------------------------------------------------------------------
    // Floating control panel
    // ---------------------------------------------------------------------

    private fun showPanel() {
        if (!overlayManager.hasOverlayPermission()) {
            stopSelf()
            return
        }

        panelLifecycleOwner.onCreate()
        panelLifecycleOwner.onStart()
        panelLifecycleOwner.onResume()

        val params = overlayManager.defaultLayoutParams(focusable = false)

        val view = ComposeView(this).apply {
            setViewTreeLifecycleOwner(panelLifecycleOwner)
            setViewTreeViewModelStoreOwner(panelLifecycleOwner)
            setViewTreeSavedStateRegistryOwner(panelLifecycleOwner)
            setContent {
                AutoClickerTheme {
                    val status by autoClickEngine.status.collectAsState()
                    Box(
                        modifier = Modifier.pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                movePanelBy(dragAmount.x, dragAmount.y)
                            }
                        }
                    ) {
                        FloatingPanel(
                            executionState = status.state,
                            onStart = { onStartRequested() },
                            onPause = { autoClickEngine.pause() },
                            onStop = { autoClickEngine.stop() },
                            onAddPoint = { showMarkerForNewPoint() },
                            onOpenApp = { openApp() },
                            onSettings = { openApp() }
                        )
                    }
                }
            }
        }

        overlayManager.addView(view, params)
        panelView = view
        panelParams = params
    }

    private fun removePanel() {
        panelView?.let { overlayManager.removeView(it) }
        panelLifecycleOwner.onDestroy()
        panelView = null
        panelParams = null
    }

    private fun movePanelBy(dx: Float, dy: Float) {
        val view = panelView ?: return
        val params = panelParams ?: return
        params.x += dx.roundToInt()
        params.y += dy.roundToInt()
        overlayManager.updateViewLayout(view, params)
    }

    private fun onStartRequested() {
        serviceScope.launch {
            val collection = profileRepository.profileCollectionFlow.first()
            val activeProfile = collection.profiles.firstOrNull { it.id == collection.activeProfileId }
                ?: collection.profiles.firstOrNull()
                ?: return@launch
            ClickForegroundService.start(this@OverlayService)
            autoClickEngine.start(activeProfile.sequence)
        }
    }

    private fun openApp() {
        startActivity(
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    // ---------------------------------------------------------------------
    // Add-point marker
    // ---------------------------------------------------------------------

    private fun showMarkerForNewPoint() {
        if (markerView != null) return

        val screenInfo = coordinateManager.currentScreenInfo()
        val startX = screenInfo.widthPx / 2f
        val startY = screenInfo.heightPx / 2f

        markerLifecycleOwner.onCreate()
        markerLifecycleOwner.onStart()
        markerLifecycleOwner.onResume()

        val params = overlayManager.fullScreenLayoutParams()

        val view = ComposeView(this).apply {
            setViewTreeLifecycleOwner(markerLifecycleOwner)
            setViewTreeViewModelStoreOwner(markerLifecycleOwner)
            setViewTreeSavedStateRegistryOwner(markerLifecycleOwner)
            setContent {
                AutoClickerTheme {
                    var x by remember { mutableStateOf(startX) }
                    var y by remember { mutableStateOf(startY) }

                    Box(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier.offset {
                                IntOffset((x - 28.dp.toPx()).roundToInt(), (y - 28.dp.toPx()).roundToInt())
                            }
                        ) {
                            PointMarker(
                                label = "X: ${x.roundToInt()}  Y: ${y.roundToInt()}",
                                onDrag = { dx, dy ->
                                    x = (x + dx).coerceIn(0f, screenInfo.widthPx.toFloat())
                                    y = (y + dy).coerceIn(0f, screenInfo.heightPx.toFloat())
                                },
                                onDragEnd = {}
                            )
                        }

                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 140.dp)
                        ) {
                            Button(onClick = { confirmNewPoint(x, y) }) { Text("Save point") }
                            Spacer(modifier = Modifier.width(16.dp))
                            OutlinedButton(onClick = { removeMarker() }) { Text("Cancel") }
                        }
                    }
                }
            }
        }

        overlayManager.addView(view, params)
        markerView = view
    }

    private fun confirmNewPoint(pixelX: Float, pixelY: Float) {
        val (xPercent, yPercent) = coordinateManager.pixelsToPercent(pixelX, pixelY)
        serviceScope.launch {
            val collection = profileRepository.profileCollectionFlow.first()
            val activeProfile = collection.profiles.firstOrNull { it.id == collection.activeProfileId }
                ?: collection.profiles.first()
            val newPoint = ClickPoint(
                xPercent = xPercent,
                yPercent = yPercent,
                order = activeProfile.sequence.points.size
            )
            val updatedSequence = activeProfile.sequence.copy(
                points = activeProfile.sequence.points + newPoint
            )
            profileRepository.upsertProfile(activeProfile.copy(sequence = updatedSequence))
            removeMarker()
        }
    }

    private fun removeMarker() {
        markerView?.let { overlayManager.removeView(it) }
        if (markerView != null) markerLifecycleOwner.onDestroy()
        markerView = null
    }

    // ---------------------------------------------------------------------
    // OCR region picker (two draggable corners)
    // ---------------------------------------------------------------------

    private fun showRegionPicker(requestId: String) {
        if (regionPickerView != null) return

        val screenInfo = coordinateManager.currentScreenInfo()
        val startLeft = screenInfo.widthPx * 0.25f
        val startTop = screenInfo.heightPx * 0.35f
        val startRight = screenInfo.widthPx * 0.75f
        val startBottom = screenInfo.heightPx * 0.45f

        regionPickerLifecycleOwner.onCreate()
        regionPickerLifecycleOwner.onStart()
        regionPickerLifecycleOwner.onResume()

        val params = overlayManager.fullScreenLayoutParams()

        val view = ComposeView(this).apply {
            setViewTreeLifecycleOwner(regionPickerLifecycleOwner)
            setViewTreeViewModelStoreOwner(regionPickerLifecycleOwner)
            setViewTreeSavedStateRegistryOwner(regionPickerLifecycleOwner)
            setContent {
                AutoClickerTheme {
                    var left by remember { mutableStateOf(startLeft) }
                    var top by remember { mutableStateOf(startTop) }
                    var right by remember { mutableStateOf(startRight) }
                    var bottom by remember { mutableStateOf(startBottom) }

                    Box(modifier = Modifier.fillMaxSize()) {
                        // Semi-transparent rectangle showing the selected region.
                        Box(
                            modifier = Modifier
                                .offset { IntOffset(left.roundToInt(), top.roundToInt()) }
                                .size(
                                    width = with(androidx.compose.ui.platform.LocalDensity.current) { (right - left).toDp() },
                                    height = with(androidx.compose.ui.platform.LocalDensity.current) { (bottom - top).toDp() }
                                )
                                .background(androidx.compose.ui.graphics.Color(0x334F8CFF))
                        )

                        Box(
                            modifier = Modifier.offset {
                                IntOffset((left - 28.dp.toPx()).roundToInt(), (top - 28.dp.toPx()).roundToInt())
                            }
                        ) {
                            PointMarker(
                                label = "Top-left",
                                onDrag = { dx, dy ->
                                    left = (left + dx).coerceIn(0f, right - 20f)
                                    top = (top + dy).coerceIn(0f, bottom - 20f)
                                },
                                onDragEnd = {}
                            )
                        }

                        Box(
                            modifier = Modifier.offset {
                                IntOffset((right - 28.dp.toPx()).roundToInt(), (bottom - 28.dp.toPx()).roundToInt())
                            }
                        ) {
                            PointMarker(
                                label = "Bottom-right",
                                onDrag = { dx, dy ->
                                    right = (right + dx).coerceIn(left + 20f, screenInfo.widthPx.toFloat())
                                    bottom = (bottom + dy).coerceIn(top + 20f, screenInfo.heightPx.toFloat())
                                },
                                onDragEnd = {}
                            )
                        }

                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 140.dp)
                        ) {
                            Button(onClick = {
                                RegionPickerBus.publish(
                                    requestId,
                                    OcrRegion(
                                        leftPercent = left / screenInfo.widthPx,
                                        topPercent = top / screenInfo.heightPx,
                                        rightPercent = right / screenInfo.widthPx,
                                        bottomPercent = bottom / screenInfo.heightPx
                                    )
                                )
                                removeRegionPicker()
                                openApp()
                            }) { Text("Use this region") }
                            Spacer(modifier = Modifier.width(16.dp))
                            OutlinedButton(onClick = { removeRegionPicker() }) { Text("Cancel") }
                        }
                    }
                }
            }
        }

        overlayManager.addView(view, params)
        regionPickerView = view
    }

    private fun removeRegionPicker() {
        regionPickerView?.let { overlayManager.removeView(it) }
        if (regionPickerView != null) regionPickerLifecycleOwner.onDestroy()
        regionPickerView = null
    }
}
