package com.autoclicker.pro.data.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class ClickSequence(
    val id: String = UUID.randomUUID().toString(),
    val points: List<ClickPoint> = emptyList(),
    val mode: ExecutionMode = ExecutionMode.SEQUENCE,
    val intervalMs: Long = 100L,
    val startDelayMs: Long = 0L,
    val repeatCount: Int = 1,
    val isInfinite: Boolean = false
) {
    val orderedEnabledPoints: List<ClickPoint>
        get() = points.filter { it.isEnabled }.sortedBy { it.order }
}
