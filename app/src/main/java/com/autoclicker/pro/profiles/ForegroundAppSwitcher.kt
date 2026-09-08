package com.autoclicker.pro.profiles

import android.util.Log
import com.autoclicker.pro.accessibility.AutoClickAccessibilityService
import com.autoclicker.pro.data.repository.AppBindingRepository
import com.autoclicker.pro.data.repository.ProfileRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Optional, off-by-default helper: when enabled in Settings, watches which
 * app is in the foreground (a generic OS signal, see
 * [AutoClickAccessibilityService.foregroundPackage]) and switches the active
 * profile to whatever the user has bound that package to. Equivalent to
 * "per-app profile" features in general automation tools — this class has no
 * knowledge of what any specific app's UI looks like.
 */
@Singleton
class ForegroundAppSwitcher @Inject constructor(
    private val appBindingRepository: AppBindingRepository,
    private val profileRepository: ProfileRepository
) {
    private var watchJob: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun start() {
        if (watchJob?.isActive == true) return
        watchJob = scope.launch {
            AutoClickAccessibilityService.foregroundPackage
                .filterNotNull()
                .distinctUntilChanged()
                .collectLatest { packageName ->
                    val bindings = appBindingRepository.bindingsFlow.first()
                    if (!bindings.autoSwitchEnabled) return@collectLatest
                    val match = bindings.bindings.firstOrNull { it.packageName == packageName } ?: return@collectLatest
                    Log.i("ForegroundAppSwitcher", "Switching to profile ${match.profileId} for $packageName")
                    profileRepository.setActiveProfile(match.profileId)
                }
        }
    }

    fun stop() {
        watchJob?.cancel()
        watchJob = null
    }
}
