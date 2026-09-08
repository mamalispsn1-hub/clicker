package com.autoclicker.pro.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import android.content.Context

/** Single DataStore instance for the whole app (profiles + settings live in
 * separate keys within the same preferences file — cheap and avoids needing Room
 * for what is fundamentally a small, infrequently-written config blob). */
val Context.autoClickerDataStore: DataStore<Preferences> by preferencesDataStore(name = "auto_clicker_prefs")
