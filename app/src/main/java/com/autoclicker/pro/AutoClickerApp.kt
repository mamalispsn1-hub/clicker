package com.autoclicker.pro

import android.app.Application
import com.autoclicker.pro.profiles.ForegroundAppSwitcher
import com.autoclicker.pro.service.NotificationHelper
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class AutoClickerApp : Application() {

    @Inject lateinit var foregroundAppSwitcher: ForegroundAppSwitcher

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.ensureChannels(this)
        // Cheap to leave running: it no-ops unless the user both enables
        // "auto-switch profile per app" in Settings AND the accessibility
        // service is connected (foregroundPackage stays null otherwise).
        foregroundAppSwitcher.start()
    }
}
