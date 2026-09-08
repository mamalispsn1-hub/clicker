package com.autoclicker.pro.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.autoclicker.pro.data.model.Profile
import com.autoclicker.pro.data.model.ProfileCollection
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val PROFILES_KEY = stringPreferencesKey("profiles_json")

@Singleton
class ProfileRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    val profileCollectionFlow: Flow<ProfileCollection> = context.autoClickerDataStore.data.map { prefs ->
        val raw = prefs[PROFILES_KEY]
        if (raw.isNullOrBlank()) {
            ProfileCollection()
        } else {
            runCatching { json.decodeFromString<ProfileCollection>(raw) }
                .getOrDefault(ProfileCollection())
        }
    }

    suspend fun save(collection: ProfileCollection) {
        context.autoClickerDataStore.edit { prefs ->
            prefs[PROFILES_KEY] = json.encodeToString(collection)
        }
    }

    suspend fun upsertProfile(profile: Profile, makeActive: Boolean = false) {
        val current = readOnce()
        val existingIndex = current.profiles.indexOfFirst { it.id == profile.id }
        val newProfiles = if (existingIndex >= 0) {
            current.profiles.toMutableList().apply { set(existingIndex, profile) }
        } else {
            current.profiles + profile
        }
        save(
            current.copy(
                profiles = newProfiles,
                activeProfileId = if (makeActive) profile.id else current.activeProfileId
            )
        )
    }

    suspend fun deleteProfile(profileId: String) {
        val current = readOnce()
        if (current.profiles.size <= 1) return // always keep at least one profile
        val remaining = current.profiles.filterNot { it.id == profileId }
        val newActive = if (current.activeProfileId == profileId) remaining.first().id else current.activeProfileId
        save(current.copy(profiles = remaining, activeProfileId = newActive))
    }

    suspend fun setActiveProfile(profileId: String) {
        val current = readOnce()
        save(current.copy(activeProfileId = profileId))
    }

    private suspend fun readOnce(): ProfileCollection = profileCollectionFlow.first()
}
