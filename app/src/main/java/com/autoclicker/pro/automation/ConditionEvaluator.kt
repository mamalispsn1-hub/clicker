package com.autoclicker.pro.automation

import com.autoclicker.pro.accessibility.AutoClickAccessibilityService
import com.autoclicker.pro.data.model.ClickCondition
import com.autoclicker.pro.data.model.TextMatchMode
import com.autoclicker.pro.ocr.OcrEngine
import com.autoclicker.pro.utils.CoordinateManager
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.system.measureTimeMillis

data class ConditionOutcome(
    val satisfied: Boolean,
    val totalLatencyMs: Long,
    val ocrLatencyMs: Long,
    val description: String
)

/**
 * Evaluates a [ClickCondition] by polling — never a tight busy-loop; each
 * failed attempt sleeps [ClickCondition.OcrText.pollIntervalMs] before trying
 * again, and the whole thing gives up after [ClickCondition.OcrText.timeoutMs].
 */
@Singleton
class ConditionEvaluator @Inject constructor(
    private val ocrEngine: OcrEngine,
    private val coordinateManager: CoordinateManager
) {

    suspend fun evaluate(condition: ClickCondition): ConditionOutcome {
        return when (condition) {
            is ClickCondition.None -> ConditionOutcome(true, 0L, 0L, "No condition")
            is ClickCondition.OcrText -> evaluateOcrText(condition)
        }
    }

    private suspend fun evaluateOcrText(condition: ClickCondition.OcrText): ConditionOutcome {
        if (!AutoClickAccessibilityService.isScreenshotCaptureSupported) {
            return ConditionOutcome(false, 0L, 0L, "Screenshot capture needs Android 11+")
        }

        var ocrLatencyTotal = 0L
        var lastFound = false
        var lastDescription = "Timed out"

        val totalLatency = measureTimeMillis {
            val deadline = System.currentTimeMillis() + condition.timeoutMs
            while (System.currentTimeMillis() < deadline) {
                var found = false
                val ocrTime = measureTimeMillis {
                    val bitmap = AutoClickAccessibilityService.captureScreenshot()
                    if (bitmap != null) {
                        val screenInfo = coordinateManager.currentScreenInfo()
                        val rect = condition.region.toPixelRect(screenInfo.widthPx, screenInfo.heightPx)
                        val result = ocrEngine.recognize(bitmap, rect)
                        found = textMatches(result.fullText, condition)
                        bitmap.recycle()
                    }
                }
                ocrLatencyTotal += ocrTime
                lastFound = found

                val presenceSatisfied = if (condition.expectPresent) found else !found
                if (presenceSatisfied) {
                    lastDescription = if (condition.expectPresent) "Text found" else "Text absent as expected"
                    return@measureTimeMillis
                }

                delay(condition.pollIntervalMs.coerceAtLeast(50L))
            }
        }

        val satisfied = if (condition.expectPresent) lastFound else !lastFound
        return ConditionOutcome(
            satisfied = satisfied,
            totalLatencyMs = totalLatency,
            ocrLatencyMs = ocrLatencyTotal,
            description = if (satisfied) lastDescription else "Timed out after ${condition.timeoutMs}ms"
        )
    }

    private fun textMatches(haystack: String, condition: ClickCondition.OcrText): Boolean {
        val hay = if (condition.caseSensitive) haystack else haystack.lowercase()
        val needle = if (condition.caseSensitive) condition.expectedText else condition.expectedText.lowercase()
        if (needle.isBlank()) return false
        return when (condition.matchMode) {
            TextMatchMode.CONTAINS -> hay.contains(needle)
            TextMatchMode.EXACT -> hay.trim() == needle.trim()
            TextMatchMode.STARTS_WITH -> hay.trim().startsWith(needle.trim())
        }
    }
}
