package com.autoclicker.pro.automation

import com.autoclicker.pro.accessibility.AutoClickAccessibilityService
import com.autoclicker.pro.data.model.ClickPoint
import com.autoclicker.pro.utils.CoordinateManager
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/**
 * Thin translation layer between a [ClickPoint] (percent-based, device
 * independent) and an actual gesture dispatch on the current screen.
 */
@Singleton
class ClickExecutor @Inject constructor(private val coordinateManager: CoordinateManager) {

    /** Executes a single point's configured repeat count. Returns number of
     * successful taps performed.
     *
     * When [ClickPoint.positionJitterPx] is set, each repeat lands at a small
     * random offset from the configured coordinate instead of the exact same
     * pixel every time — useful for avoiding suspiciously identical taps in
     * games/UIs that watch for that, and generally more human-like. */
    suspend fun execute(point: ClickPoint): Int {
        val (baseX, baseY) = coordinateManager.percentToPixels(point.xPercent, point.yPercent)
        val screenInfo = coordinateManager.currentScreenInfo()
        var successCount = 0

        repeat(point.repeatCount.coerceAtLeast(1)) {
            val (x, y) = applyJitter(baseX, baseY, point.positionJitterPx, screenInfo.widthPx, screenInfo.heightPx)
            val success = AutoClickAccessibilityService.dispatchTap(x, y, point.clickDurationMs)
            if (success) successCount++
        }
        return successCount
    }

    private fun applyJitter(x: Float, y: Float, jitterPx: Float, maxWidth: Int, maxHeight: Int): Pair<Float, Float> {
        if (jitterPx <= 0f) return x to y
        val dx = Random.nextFloat() * 2f * jitterPx - jitterPx
        val dy = Random.nextFloat() * 2f * jitterPx - jitterPx
        return (x + dx).coerceIn(0f, maxWidth.toFloat()) to (y + dy).coerceIn(0f, maxHeight.toFloat())
    }
}
