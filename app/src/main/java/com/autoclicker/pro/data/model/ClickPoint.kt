package com.autoclicker.pro.data.model

import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * A single tap definition.
 *
 * Coordinates are stored BOTH as absolute pixels (captured at creation time on the
 * device/orientation the user drew them on) and as a percentage of screen width/height,
 * so [com.autoclicker.pro.utils.CoordinateManager] can rescale them correctly if the
 * point is replayed on a different screen size, density, or after a rotation.
 */
@Serializable
data class ClickPoint(
    val id: String = UUID.randomUUID().toString(),
    val label: String = "",
    val xPercent: Float,
    val yPercent: Float,
    val delayMs: Long = 100L,
    val clickDurationMs: Long = 50L,
    val repeatCount: Int = 1,
    val isEnabled: Boolean = true,
    val order: Int = 0,
    /** Optional randomization applied to [delayMs] and the tap coordinates so
     * repeated taps don't land on the exact same pixel/timing every time.
     * 0 (default) disables jitter entirely. */
    val delayJitterMs: Long = 0L,
    val positionJitterPx: Float = 0f,
    /** Optional pre-condition evaluated before this point is tapped. See
     * [ClickCondition] — screen/app-agnostic, defined entirely by the user. */
    val condition: ClickCondition = ClickCondition.None
) {
    fun toPixels(screenWidth: Int, screenHeight: Int): Pair<Float, Float> {
        return (xPercent * screenWidth) to (yPercent * screenHeight)
    }

    companion object {
        fun fromPixels(
            x: Float,
            y: Float,
            screenWidth: Int,
            screenHeight: Int,
            order: Int = 0
        ): ClickPoint = ClickPoint(
            xPercent = (x / screenWidth).coerceIn(0f, 1f),
            yPercent = (y / screenHeight).coerceIn(0f, 1f),
            order = order
        )
    }
}
