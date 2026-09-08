package com.autoclicker.pro.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autoclicker.pro.automation.AutoClickEngine
import com.autoclicker.pro.data.model.AppBinding
import com.autoclicker.pro.data.model.AppBindingCollection
import com.autoclicker.pro.data.model.ClickPoint
import com.autoclicker.pro.data.model.ClickSequence
import com.autoclicker.pro.data.model.ExecutionMode
import com.autoclicker.pro.data.model.ExecutionStatus
import com.autoclicker.pro.data.model.Profile
import com.autoclicker.pro.data.model.ProfileCollection
import com.autoclicker.pro.data.repository.AppBindingRepository
import com.autoclicker.pro.data.repository.ProfileRepository
import com.autoclicker.pro.service.ClickForegroundService
import com.autoclicker.pro.settings.AppSettings
import com.autoclicker.pro.settings.SettingsRepository
import com.autoclicker.pro.utils.CoordinateManager
import com.autoclicker.pro.utils.InstalledAppInfo
import com.autoclicker.pro.utils.InstalledAppLister
import com.autoclicker.pro.utils.PermissionChecker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.content.Context
import javax.inject.Inject

data class MainUiState(
    val profileCollection: ProfileCollection = ProfileCollection(),
    val executionStatus: ExecutionStatus = ExecutionStatus(),
    val settings: AppSettings = AppSettings(),
    val appBindings: AppBindingCollection = AppBindingCollection(),
    val overlayPermissionGranted: Boolean = false,
    val accessibilityServiceReady: Boolean = false
) {
    val activeProfile: Profile?
        get() = profileCollection.profiles.firstOrNull { it.id == profileCollection.activeProfileId }
            ?: profileCollection.profiles.firstOrNull()
}

@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val profileRepository: ProfileRepository,
    private val settingsRepository: SettingsRepository,
    private val appBindingRepository: AppBindingRepository,
    private val installedAppLister: InstalledAppLister,
    private val autoClickEngine: AutoClickEngine,
    private val permissionChecker: PermissionChecker,
    val coordinateManager: CoordinateManager
) : ViewModel() {

    private val _permissionsRefreshTrigger = MutableStateFlow(0)

    val uiState: StateFlow<MainUiState> = combine(
        profileRepository.profileCollectionFlow,
        autoClickEngine.status,
        settingsRepository.settingsFlow,
        appBindingRepository.bindingsFlow,
        _permissionsRefreshTrigger
    ) { profiles, status, settings, appBindings, _ ->
        MainUiState(
            profileCollection = profiles,
            executionStatus = status,
            settings = settings,
            appBindings = appBindings,
            overlayPermissionGranted = permissionChecker.hasOverlayPermission(),
            accessibilityServiceReady = permissionChecker.isAccessibilityServiceReady()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MainUiState())

    /** Call from MainActivity's onResume — permission grants happen in system
     * Settings screens, outside our process's normal recomposition triggers. */
    fun refreshPermissions() {
        _permissionsRefreshTrigger.value += 1
    }

    fun startSequence() {
        val profile = uiState.value.activeProfile ?: return
        ClickForegroundService.start(context)
        autoClickEngine.start(profile.sequence)
    }

    fun pauseSequence() = autoClickEngine.pause()
    fun resumeSequence() = autoClickEngine.resume()
    fun emergencyStop() = autoClickEngine.stop()

    fun addPoint(xPercent: Float, yPercent: Float) = viewModelScope.launch {
        val profile = uiState.value.activeProfile ?: return@launch
        val point = ClickPoint(xPercent = xPercent, yPercent = yPercent, order = profile.sequence.points.size)
        saveSequence(profile, profile.sequence.copy(points = profile.sequence.points + point))
    }

    fun updatePoint(point: ClickPoint) = viewModelScope.launch {
        val profile = uiState.value.activeProfile ?: return@launch
        val updatedPoints = profile.sequence.points.map { if (it.id == point.id) point else it }
        saveSequence(profile, profile.sequence.copy(points = updatedPoints))
    }

    fun deletePoint(pointId: String) = viewModelScope.launch {
        val profile = uiState.value.activeProfile ?: return@launch
        val updatedPoints = profile.sequence.points.filterNot { it.id == pointId }
            .mapIndexed { index, p -> p.copy(order = index) }
        saveSequence(profile, profile.sequence.copy(points = updatedPoints))
    }

    fun togglePointEnabled(pointId: String) = viewModelScope.launch {
        val profile = uiState.value.activeProfile ?: return@launch
        val updatedPoints = profile.sequence.points.map {
            if (it.id == pointId) it.copy(isEnabled = !it.isEnabled) else it
        }
        saveSequence(profile, profile.sequence.copy(points = updatedPoints))
    }

    fun movePoint(pointId: String, up: Boolean) = viewModelScope.launch {
        val profile = uiState.value.activeProfile ?: return@launch
        val sorted = profile.sequence.points.sortedBy { it.order }.toMutableList()
        val index = sorted.indexOfFirst { it.id == pointId }
        if (index < 0) return@launch
        val swapWith = if (up) index - 1 else index + 1
        if (swapWith !in sorted.indices) return@launch
        val a = sorted[index]
        val b = sorted[swapWith]
        sorted[index] = b.copy(order = a.order)
        sorted[swapWith] = a.copy(order = b.order)
        saveSequence(profile, profile.sequence.copy(points = sorted))
    }

    fun updateExecutionMode(mode: ExecutionMode) = viewModelScope.launch {
        val profile = uiState.value.activeProfile ?: return@launch
        saveSequence(profile, profile.sequence.copy(mode = mode))
    }

    fun updateInterval(intervalMs: Long) = viewModelScope.launch {
        val profile = uiState.value.activeProfile ?: return@launch
        saveSequence(profile, profile.sequence.copy(intervalMs = intervalMs))
    }

    fun updateRepeatSettings(repeatCount: Int, isInfinite: Boolean, startDelayMs: Long) = viewModelScope.launch {
        val profile = uiState.value.activeProfile ?: return@launch
        saveSequence(
            profile,
            profile.sequence.copy(repeatCount = repeatCount, isInfinite = isInfinite, startDelayMs = startDelayMs)
        )
    }

    fun createProfile(name: String) = viewModelScope.launch {
        profileRepository.upsertProfile(Profile(name = name, sequence = ClickSequence()), makeActive = true)
    }

    fun switchProfile(profileId: String) = viewModelScope.launch {
        profileRepository.setActiveProfile(profileId)
    }

    fun deleteProfile(profileId: String) = viewModelScope.launch {
        profileRepository.deleteProfile(profileId)
    }

    fun setDefaultInterval(ms: Long) = viewModelScope.launch { settingsRepository.setDefaultInterval(ms) }
    fun setVibrateOnClick(enabled: Boolean) = viewModelScope.launch { settingsRepository.setVibrateOnClick(enabled) }
    fun setShowTapRipple(enabled: Boolean) = viewModelScope.launch { settingsRepository.setShowTapRipple(enabled) }
    fun setKeepScreenOn(enabled: Boolean) = viewModelScope.launch { settingsRepository.setKeepScreenOn(enabled) }

    // ---------------------------------------------------------------------
    // Per-app profile auto-switch (generic — see ForegroundAppSwitcher)
    // ---------------------------------------------------------------------

    fun setAutoSwitchEnabled(enabled: Boolean) = viewModelScope.launch {
        appBindingRepository.setAutoSwitchEnabled(enabled)
    }

    fun upsertAppBinding(binding: AppBinding) = viewModelScope.launch {
        appBindingRepository.upsertBinding(binding)
    }

    fun removeAppBinding(packageName: String) = viewModelScope.launch {
        appBindingRepository.removeBinding(packageName)
    }

    suspend fun listInstalledApps(): List<InstalledAppInfo> = withContext(Dispatchers.IO) {
        installedAppLister.listLaunchableApps()
    }

    private suspend fun saveSequence(profile: Profile, sequence: ClickSequence) {
        profileRepository.upsertProfile(profile.copy(sequence = sequence))
    }
}
