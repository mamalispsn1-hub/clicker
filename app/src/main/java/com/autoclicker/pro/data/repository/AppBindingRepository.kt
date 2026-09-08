package com.autoclicker.pro.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.autoclicker.pro.data.model.AppBinding
import com.autoclicker.pro.data.model.AppBindingCollection
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val APP_BINDINGS_KEY = stringPreferencesKey("app_bindings_json")

@Singleton
class AppBindingRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    val bindingsFlow: Flow<AppBindingCollection> = context.autoClickerDataStore.data.map { prefs ->
        val raw = prefs[APP_BINDINGS_KEY]
        if (raw.isNullOrBlank()) AppBindingCollection()
        else runCatching { json.decodeFromString<AppBindingCollection>(raw) }.getOrDefault(AppBindingCollection())
    }

    suspend fun setAutoSwitchEnabled(enabled: Boolean) {
        val current = bindingsFlow.first()
        save(current.copy(autoSwitchEnabled = enabled))
    }

    suspend fun upsertBinding(binding: AppBinding) {
        val current = bindingsFlow.first()
        val updated = current.bindings.filterNot { it.packageName == binding.packageName } + binding
        save(current.copy(bindings = updated))
    }

    suspend fun removeBinding(packageName: String) {
        val current = bindingsFlow.first()
        save(current.copy(bindings = current.bindings.filterNot { it.packageName == packageName }))
    }

    private suspend fun save(collection: AppBindingCollection) {
        context.autoClickerDataStore.edit { prefs ->
            prefs[APP_BINDINGS_KEY] = json.encodeToString(collection)
        }
    }
}
