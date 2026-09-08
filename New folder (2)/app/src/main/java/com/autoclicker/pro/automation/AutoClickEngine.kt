package com.autoclicker.pro.automation

import android.util.Log
import com.autoclicker.pro.data.model.ClickCondition
import com.autoclicker.pro.data.model.ClickPoint
import com.autoclicker.pro.data.model.ClickSequence
import com.autoclicker.pro.data.model.ConditionFailureAction
import com.autoclicker.pro.data.model.ExecutionMode
import com.autoclicker.pro.data.model.ExecutionState
import com.autoclicker.pro.data.model.ExecutionStatus
import com.autoclicker.pro.data.model.MIN_SAFE_INTERVAL_MS
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random
import kotlin.system.measureTimeMillis

/**
 * Owns the click-sequence execution loop. A single instance lives for the
 * process lifetime (Hilt @Singleton) so the overlay, the foreground service,
 * and the main UI all observe the exact same state.
 *
 * Threading: runs entirely on a dedicated [SupervisorJob] + [Dispatchers.Default]
 * scope so a slow/blocked UI never stalls click timing, and cancelling the job
 * (emergency stop) immediately halts any in-flight `delay`/loop — no polling
 * flags required for the stop path.
 */
@Singleton
class AutoClickEngine @Inject constructor(
    private val clickExecutor: ClickExecutor,
    private val conditionEvaluator: ConditionEvaluator
) {
    companion object {
        private const val TAG = "AutoClickEngine"
    }

    private val engineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var runJob: Job? = null

    private val _status = MutableStateFlow(ExecutionStatus())
    val status: StateFlow<ExecutionStatus> = _status.asStateFlow()

    private var pauseRequested = false

    fun start(sequence: ClickSequence) {
        if (sequence.orderedEnabledPoints.isEmpty()) {
            _status.value = _status.value.copy(lastAction = "No enabled click points")
            return
        }
        stop() // ensure any previous run is fully cancelled first
        pauseRequested = false
        _status.value = ExecutionStatus(state = ExecutionState.RUNNING, lastAction = "Starting")

        runJob = engineScope.launch {
            runSequence(sequence)
        }
    }

    fun pause() {
        if (_status.value.state != ExecutionState.RUNNING) return
        pauseRequested = true
        _status.value = _status.value.copy(state = ExecutionState.PAUSED, lastAction = "Paused")
    }

    fun resume() {
        if (_status.value.state != ExecutionState.PAUSED) return
        pauseRequested = false
        _status.value = _status.value.copy(state = ExecutionState.RUNNING, lastAction = "Resumed")
    }

    /** Emergency stop: cancels the run job outright so nothing further executes,
     * even mid-delay or mid-condition-wait. Safe to call repeatedly / when
     * already idle. */
    fun stop() {
        runJob?.cancel()
        runJob = null
        pauseRequested = false
        _status.value = _status.value.copy(state = ExecutionState.STOPPED, lastAction = "Stopped")
    }

    private suspend fun runSequence(sequence: ClickSequence) {
        val points = sequence.orderedEnabledPoints
        val interval = sequence.intervalMs.coerceAtLeast(MIN_SAFE_INTERVAL_MS)

        if (sequence.startDelayMs > 0) {
            _status.value = _status.value.copy(lastAction = "Start delay")
            delay(sequence.startDelayMs)
        }

        val runForever = sequence.isInfinite || sequence.mode == ExecutionMode.INFINITE
        val totalRounds = if (runForever) Int.MAX_VALUE else sequence.repeatCount.coerceAtLeast(1)

        var round = 0
        outer@ while (coroutineContext.isActive && round < totalRounds) {
            for ((index, point) in points.withIndex()) {
                waitWhilePaused()
                if (!coroutineContext.isActive) return

                _status.value = _status.value.copy(
                    currentPointIndex = index,
                    currentPointLabel = point.label.ifBlank { "Point ${index + 1}" },
                    lastAction = "Evaluating condition"
                )

                val shouldTap = awaitCondition(point)
                if (!shouldTap) {
                    // awaitCondition already updated lastAction/status; ABORT is
                    // signalled by clearing the run job's activity via stop()
                    // from inside awaitCondition, so just check again here.
                    if (!coroutineContext.isActive) return
                    continue // SKIP: move on to the next point
                }

                if (point.delayJitterMs > 0) {
                    delay(Random.nextLong(0, point.delayJitterMs + 1))
                }

                var successes = 0
                val latency = measureTimeMillis {
                    successes = clickExecutor.execute(point)
                }

                _status.value = _status.value.copy(
                    clickCount = _status.value.clickCount + successes,
                    lastExecutionLatencyMs = latency,
                    lastAction = if (successes > 0) "Tap dispatched" else "Tap failed"
                )

                if (point.delayMs > 0) delay(point.delayMs)
                delay(interval)
            }

            // Single/Multi click modes are effectively one round through the points.
            if (sequence.mode == ExecutionMode.SINGLE_CLICK || sequence.mode == ExecutionMode.MULTI_CLICK) {
                break@outer
            }
            round++
        }

        if (coroutineContext.isActive) {
            _status.value = _status.value.copy(state = ExecutionState.IDLE, lastAction = "Sequence complete")
        }
    }

    /** Returns true if [point] should be tapped now. Handles retry/skip/abort
     * per [ClickCondition.OcrText.onTimeout] when the condition times out. */
    private suspend fun awaitCondition(point: ClickPoint): Boolean {
        val condition = point.condition
        if (condition is ClickCondition.None) return true

        var attempt = 0
        val maxAttempts = if (condition is ClickCondition.OcrText) condition.maxRetries.coerceAtLeast(1) else 1

        while (attempt < maxAttempts) {
            val outcome = conditionEvaluator.evaluate(condition)
            _status.value = _status.value.copy(
                lastConditionLatencyMs = outcome.totalLatencyMs,
                lastOcrLatencyMs = outcome.ocrLatencyMs,
                lastConditionResult = outcome.description
            )
            if (outcome.satisfied) return true
            attempt++

            val onTimeout = (condition as? ClickCondition.OcrText)?.onTimeout ?: ConditionFailureAction.SKIP_POINT
            if (onTimeout != ConditionFailureAction.RETRY_THEN_SKIP || attempt >= maxAttempts) {
                if (onTimeout == ConditionFailureAction.ABORT_SEQUENCE) {
                    Log.i(TAG, "Condition failed, aborting sequence per configuration")
                    stop()
                }
                return false
            }
        }
        return false
    }

    private suspend fun waitWhilePaused() {
        while (pauseRequested) {
            delay(150)
        }
    }
}
