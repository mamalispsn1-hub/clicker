package com.autoclicker.pro.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import com.autoclicker.pro.data.model.MIN_SAFE_INTERVAL_MS
import com.autoclicker.pro.data.repository.autoClickerDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val DEFAULT_INTERVAL_KEY = longPreferencesKey("default_interval_ms")
private val VIBRATE_ON_CLICK_KEY = booleanPreferencesKey("vibrate_on_click")
private val SHOW_TAP_RIPPLE_KEY = booleanPreferencesKey("show_tap_ripple")
private val KEEP_SCREEN_ON_KEY = booleanPreferencesKey("keep_screen_on")

data class AppSettings(
    val defaultIntervalMs: Long = 100L,
    val vibrateOnClick: Boolean = false,
    val showTapRipple: Boolean = true,
    val keepScreenOn: Boolean = true
)

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val settingsFlow: Flow<AppSettings> = context.autoClickerDataStore.data.map { prefs ->
        AppSettings(
            defaultIntervalMs = (prefs[DEFAULT_INTERVAL_KEY] ?: 100L).coerceAtLeast(MIN_SAFE_INTERVAL_MS),
            vibrateOnClick = prefs[VIBRATE_ON_CLICK_KEY] ?: false,
            showTapRipple = prefs[SHOW_TAP_RIPPLE_KEY] ?: true,
            keepScreenOn = prefs[KEEP_SCREEN_ON_KEY] ?: true
        )
    }

    suspend fun setDefaultInterval(ms: Long) {
        context.autoClickerDataStore.edit { it[DEFAULT_INTERVAL_KEY] = ms.coerceAtLeast(MIN_SAFE_INTERVAL_MS) }
    }

    suspend fun setVibrateOnClick(enabled: Boolean) {
        context.autoClickerDataStore.edit { it[VIBRATE_ON_CLICK_KEY] = enabled }
    }

    suspend fun setShowTapRipple(enabled: Boolean) {
        context.autoClickerDataStore.edit { it[SHOW_TAP_RIPPLE_KEY] = enabled }
    }

    suspend fun setKeepScreenOn(enabled: Boolean) {
        context.autoClickerDataStore.edit { it[KEEP_SCREEN_ON_KEY] = enabled }
    }
}
