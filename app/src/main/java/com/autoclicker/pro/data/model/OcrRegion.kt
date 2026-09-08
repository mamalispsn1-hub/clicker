package com.autoclicker.pro.data.model

import kotlinx.serialization.Serializable

/** A rectangular screen region, stored as percentages (like [ClickPoint]) so it
 * stays valid across screen sizes, densities, and orientation changes. */
@Serializable
data class OcrRegion(
    val leftPercent: Float,
    val topPercent: Float,
    val rightPercent: Float,
    val bottomPercent: Float
) {
    fun toPixelRect(screenWidth: Int, screenHeight: Int): android.graphics.Rect {
        return android.graphics.Rect(
            (leftPercent * screenWidth).toInt(),
            (topPercent * screenHeight).toInt(),
            (rightPercent * screenWidth).toInt(),
            (bottomPercent * screenHeight).toInt()
        )
    }

    companion object {
        /** Whole-screen region, used as a sensible default before the user
         * narrows it down with the region picker. */
        val FULL_SCREEN = OcrRegion(0f, 0f, 1f, 1f)
    }
}
