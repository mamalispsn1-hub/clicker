package com.autoclicker.pro.utils

import android.content.Context
import android.content.res.Configuration
import android.graphics.Point
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.core.content.getSystemService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Central place for anything screen/DPI/orientation related so click points
 * (stored as percentages, see [com.autoclicker.pro.data.model.ClickPoint]) always
 * resolve to the correct pixel on the CURRENT screen configuration.
 */
@Singleton
class CoordinateManager @Inject constructor(@ApplicationContext private val context: Context) {

    data class ScreenInfo(
        val widthPx: Int,
        val heightPx: Int,
        val densityDpi: Int,
        val density: Float,
        val isPortrait: Boolean
    )

    fun currentScreenInfo(): ScreenInfo {
        val windowManager = context.getSystemService<WindowManager>()
        val metrics = DisplayMetrics()

        val size = Point()
        @Suppress("DEPRECATION")
        windowManager?.defaultDisplay?.getRealSize(size)
        @Suppress("DEPRECATION")
        windowManager?.defaultDisplay?.getRealMetrics(metrics)

        val width = if (size.x > 0) size.x else context.resources.displayMetrics.widthPixels
        val height = if (size.y > 0) size.y else context.resources.displayMetrics.heightPixels

        val isPortrait = context.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT

        return ScreenInfo(
            widthPx = width,
            heightPx = height,
            densityDpi = metrics.densityDpi.takeIf { it > 0 } ?: context.resources.displayMetrics.densityDpi,
            density = context.resources.displayMetrics.density,
            isPortrait = isPortrait
        )
    }

    fun percentToPixels(xPercent: Float, yPercent: Float): Pair<Float, Float> {
        val info = currentScreenInfo()
        return (xPercent * info.widthPx) to (yPercent * info.heightPx)
    }

    fun pixelsToPercent(x: Float, y: Float): Pair<Float, Float> {
        val info = currentScreenInfo()
        val px = if (info.widthPx > 0) (x / info.widthPx).coerceIn(0f, 1f) else 0f
        val py = if (info.heightPx > 0) (y / info.heightPx).coerceIn(0f, 1f) else 0f
        return px to py
    }
}
