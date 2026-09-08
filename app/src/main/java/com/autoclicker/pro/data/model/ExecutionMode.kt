package com.autoclicker.pro.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class ExecutionMode(val displayName: String) {
    SINGLE_CLICK("Single Click"),
    MULTI_CLICK("Multi Click"),
    AUTO_REPEAT("Auto Repeat"),
    SEQUENCE("Sequence"),
    TIMED("Timed"),
    INFINITE("Infinite")
}

/** Allowed inter-click intervals, in milliseconds. Kept off the extreme low end
 * to avoid saturating the main/gesture dispatch thread and to stay compatible
 * with the OS's gesture-dispatch throttling. */
val AVAILABLE_SPEEDS_MS = listOf(1L, 10L, 25L, 50L, 100L, 250L, 500L, 1000L)

/** Practical floor: AccessibilityService gesture dispatch and the OS input
 * pipeline cannot reliably sustain faster than this without dropped/queued
 * gestures on most devices. Values below this are clamped at runtime. */
const val MIN_SAFE_INTERVAL_MS = 10L
