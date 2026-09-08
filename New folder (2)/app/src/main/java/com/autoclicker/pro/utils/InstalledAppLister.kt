package com.autoclicker.pro.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class InstalledAppInfo(val packageName: String, val label: String)

/** Lists the user's own launchable apps so they can pick one from a normal
 * list UI (like any app-picker) instead of typing a package name. Read-only;
 * this never inspects an app's UI or accessibility content — only the
 * standard "is this app in the launcher" package metadata. */
@Singleton
class InstalledAppLister @Inject constructor(@ApplicationContext private val context: Context) {

    fun listLaunchableApps(): List<InstalledAppInfo> {
        val packageManager = context.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)

        val resolveInfos = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(
                launcherIntent,
                PackageManager.ResolveInfoFlags.of(0)
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(launcherIntent, 0)
        }

        return resolveInfos
            .mapNotNull { resolveInfo ->
                val pkg = resolveInfo.activityInfo?.packageName ?: return@mapNotNull null
                if (pkg == context.packageName) return@mapNotNull null // skip ourselves
                val label = runCatching { resolveInfo.loadLabel(packageManager).toString() }.getOrDefault(pkg)
                InstalledAppInfo(packageName = pkg, label = label)
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }
}
