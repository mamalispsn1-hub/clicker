package com.autoclicker.pro.utils

import android.content.Context
import android.provider.Settings
import android.text.TextUtils
import com.autoclicker.pro.accessibility.AutoClickAccessibilityService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PermissionChecker @Inject constructor(@ApplicationContext private val context: Context) {

    fun hasOverlayPermission(): Boolean =
        android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M ||
            Settings.canDrawOverlays(context)

    /**
     * Settings.Secure only tells us the service is *enabled by the user*; it
     * doesn't guarantee the service is currently bound/connected (that can
     * lag briefly after enabling, or the system can unbind a misbehaving
     * service). We treat "enabled AND connected" as the real ready state,
     * exposed by [AutoClickAccessibilityService.isServiceEnabled].
     */
    fun isAccessibilityServiceEnabledInSettings(): Boolean {
        val expectedComponent = "${context.packageName}/${AutoClickAccessibilityService::class.java.name}"
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val splitter = TextUtils.SimpleStringSplitter(':')
        splitter.setString(enabledServices)
        while (splitter.hasNext()) {
            if (splitter.next().equals(expectedComponent, ignoreCase = true)) return true
        }
        return false
    }

    fun isAccessibilityServiceReady(): Boolean =
        isAccessibilityServiceEnabledInSettings() && AutoClickAccessibilityService.isServiceEnabled()
}
