package com.autoclicker.pro.overlay

import com.autoclicker.pro.data.model.OcrRegion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Tiny in-process handoff between [OverlayService] (which draws the region
 * picker over whatever app the user is configuring against) and the editor
 * screen back in the app (which is waiting for the result). Deliberately not
 * a database/DataStore entry — this is transient UI state for a single
 * pick-a-region interaction, not something that needs to survive process
 * death.
 */
object RegionPickerBus {

    data class PickedRegion(val requestId: String, val region: OcrRegion)

    private val _result = MutableStateFlow<PickedRegion?>(null)
    val result: StateFlow<PickedRegion?> = _result

    fun publish(requestId: String, region: OcrRegion) {
        _result.value = PickedRegion(requestId, region)
    }

    /** Reads and clears the pending result, but only if it was meant for
     * [requestId] — several point-editor cards can be expanded at once, and
     * without this check they'd all race to apply the same picked region to
     * themselves. */
    fun consumeIfMatches(requestId: String): OcrRegion? {
        val current = _result.value ?: return null
        if (current.requestId != requestId) return null
        _result.value = null
        return current.region
    }
}
