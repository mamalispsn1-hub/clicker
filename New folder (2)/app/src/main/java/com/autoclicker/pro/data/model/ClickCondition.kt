package com.autoclicker.pro.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class TextMatchMode(val displayName: String) {
    CONTAINS("Contains"),
    EXACT("Exact"),
    STARTS_WITH("Starts with")
}

/** What the engine does with a point whose condition did not become true
 * within [ClickCondition.OcrText.timeoutMs]. */
@Serializable
enum class ConditionFailureAction(val displayName: String) {
    SKIP_POINT("Skip this point"),
    RETRY_THEN_SKIP("Retry, then skip"),
    ABORT_SEQUENCE("Abort sequence")
}

/**
 * Generic pre-conditions a [ClickPoint] can wait on before it is tapped.
 * Deliberately screen/app-agnostic: the user draws a region and types the
 * text themselves (see the region picker in the overlay + editor), the
 * engine never assumes anything about which app or UI it is looking at.
 */
@Serializable
sealed class ClickCondition {

    @Serializable
    data object None : ClickCondition()

    /** Wait until [region] contains (or stops containing, if [expectPresent]
     * is false) text matching [expectedText] under [matchMode]. */
    @Serializable
    data class OcrText(
        val region: OcrRegion,
        val expectedText: String,
        val matchMode: TextMatchMode = TextMatchMode.CONTAINS,
        val expectPresent: Boolean = true,
        val caseSensitive: Boolean = false,
        val timeoutMs: Long = 5000L,
        val pollIntervalMs: Long = 250L,
        val onTimeout: ConditionFailureAction = ConditionFailureAction.SKIP_POINT,
        val maxRetries: Int = 1
    ) : ClickCondition()
}
