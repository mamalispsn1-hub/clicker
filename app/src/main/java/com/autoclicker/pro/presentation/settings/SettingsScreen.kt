package com.autoclicker.pro.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.autoclicker.pro.data.model.AVAILABLE_SPEEDS_MS
import com.autoclicker.pro.presentation.main.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: MainViewModel, onBack: () -> Unit, onManageAppBindings: () -> Unit) {
    val state by viewModel.uiState.collectAsState()
    val settings = state.settings

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {

            Text("Default click interval")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AVAILABLE_SPEEDS_MS.forEach { ms ->
                    androidx.compose.material3.FilterChip(
                        selected = settings.defaultIntervalMs == ms,
                        onClick = { viewModel.setDefaultInterval(ms) },
                        label = { Text("${ms}ms") }
                    )
                }
            }

            SettingsSwitchRow(
                label = "Vibrate on click",
                checked = settings.vibrateOnClick,
                onCheckedChange = { viewModel.setVibrateOnClick(it) }
            )
            SettingsSwitchRow(
                label = "Show tap ripple while placing points",
                checked = settings.showTapRipple,
                onCheckedChange = { viewModel.setShowTapRipple(it) }
            )
            SettingsSwitchRow(
                label = "Keep screen on while running",
                checked = settings.keepScreenOn,
                onCheckedChange = { viewModel.setKeepScreenOn(it) }
            )

            Text("Per-app profiles", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            SettingsSwitchRow(
                label = "Auto-switch profile when a bound app opens",
                checked = state.appBindings.autoSwitchEnabled,
                onCheckedChange = { viewModel.setAutoSwitchEnabled(it) }
            )
            androidx.compose.material3.OutlinedButton(
                onClick = onManageAppBindings,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Manage app ↔ profile bindings (${state.appBindings.bindings.size})")
            }
        }
    }
}

@Composable
private fun SettingsSwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.padding(end = 12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
