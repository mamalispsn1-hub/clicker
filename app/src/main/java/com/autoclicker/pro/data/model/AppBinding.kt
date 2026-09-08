package com.autoclicker.pro.data.model

import kotlinx.serialization.Serializable

/** Maps a package name to the profile that should become active whenever
 * that app is in the foreground. Purely a generic "context switch" building
 * block (the same idea as per-app volume/DND on stock Android, or Tasker's
 * "Application" context) — the user picks the package from their own
 * installed-apps list; nothing here is tied to a specific target app. */
@Serializable
data class AppBinding(
    val packageName: String,
    val appLabel: String,
    val profileId: String
)

@Serializable
data class AppBindingCollection(
    val bindings: List<AppBinding> = emptyList(),
    val autoSwitchEnabled: Boolean = false
)
