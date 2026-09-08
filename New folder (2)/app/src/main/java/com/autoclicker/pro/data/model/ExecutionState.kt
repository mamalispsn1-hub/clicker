package com.autoclicker.pro.data.model

enum class ExecutionState {
    IDLE,
    RUNNING,
    PAUSED,
    STOPPED
}

/** Snapshot of engine state exposed to the UI / overlay / debug screen. */
data class ExecutionStatus(
    val state: ExecutionState = ExecutionState.IDLE,
    val currentPointIndex: Int = -1,
    val currentPointLabel: String = "",
    val clickCount: Long = 0L,
    val lastAction: String = "Idle",
    val lastExecutionLatencyMs: Long = 0L,
    /** Time spent waiting on this point's [ClickCondition], if any. */
    val lastConditionLatencyMs: Long = 0L,
    /** Time spent inside the OCR call itself (screenshot + text recognition),
     * a subset of [lastConditionLatencyMs]. */
    val lastOcrLatencyMs: Long = 0L,
    val lastConditionResult: String = "-"
)
