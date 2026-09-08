package com.autoclicker.pro.presentation.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.autoclicker.pro.data.model.ExecutionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onEdit: () -> Unit,
    onProfiles: () -> Unit,
    onSettings: () -> Unit,
    onDebug: () -> Unit,
    onRequestOverlayPermission: () -> Unit,
    onRequestAccessibilityPermission: () -> Unit,
    onShowOverlay: () -> Unit,
    onHideOverlay: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Auto Clicker Pro") },
                actions = {
                    IconButton(onClick = onDebug) {
                        Icon(Icons.Filled.BugReport, contentDescription = "Debug")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PermissionCard(
                title = "Overlay permission",
                granted = state.overlayPermissionGranted,
                actionLabel = "Grant overlay",
                onAction = onRequestOverlayPermission
            )
            PermissionCard(
                title = "Accessibility service",
                granted = state.accessibilityServiceReady,
                actionLabel = "Enable service",
                onAction = onRequestAccessibilityPermission
            )

            StatusCard(state = state)

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = {
                        if (state.executionStatus.state == ExecutionState.RUNNING) viewModel.pauseSequence()
                        else if (state.executionStatus.state == ExecutionState.PAUSED) viewModel.resumeSequence()
                        else viewModel.startSequence()
                    },
                    enabled = state.overlayPermissionGranted && state.accessibilityServiceReady,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        when (state.executionStatus.state) {
                            ExecutionState.RUNNING -> "PAUSE"
                            ExecutionState.PAUSED -> "RESUME"
                            else -> "START"
                        }
                    )
                }
                OutlinedButton(onClick = { viewModel.emergencyStop() }, modifier = Modifier.weight(1f)) {
                    Text("STOP")
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(onClick = onEdit, modifier = Modifier.weight(1f)) { Text("EDIT") }
                OutlinedButton(onClick = onProfiles, modifier = Modifier.weight(1f)) { Text("PROFILES") }
                OutlinedButton(onClick = onSettings, modifier = Modifier.weight(1f)) { Text("SETTINGS") }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onShowOverlay,
                    enabled = state.overlayPermissionGranted,
                    modifier = Modifier.weight(1f)
                ) { Text("Show floating panel") }
                OutlinedButton(onClick = onHideOverlay, modifier = Modifier.weight(1f)) {
                    Text("Hide floating panel")
                }
            }

            Text("Click points", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
            val points = state.activeProfile?.sequence?.points?.sortedBy { it.order } ?: emptyList()
            if (points.isEmpty()) {
                Text("No click points yet — use \"Show floating panel\" then the + button, or open Edit.")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(points, key = { it.id }) { point ->
                        Card {
                            Column(Modifier.padding(12.dp)) {
                                Text(
                                    point.label.ifBlank { "Point ${point.order + 1}" },
                                    fontWeight = FontWeight.Bold
                                )
                                Text("x=${"%.2f".format(point.xPercent)}  y=${"%.2f".format(point.yPercent)}  delay=${point.delayMs}ms")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionCard(title: String, granted: Boolean, actionLabel: String, onAction: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(title, fontWeight = FontWeight.Bold)
                Text(if (granted) "Granted" else "Not granted")
            }
            if (!granted) {
                Button(onClick = onAction) { Text(actionLabel) }
            }
        }
    }
}

@Composable
private fun StatusCard(state: MainUiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Service status: ${if (state.accessibilityServiceReady) "Connected" else "Disconnected"}")
            Text("Overlay status: ${if (state.overlayPermissionGranted) "Permitted" else "Not permitted"}")
            Text("Current profile: ${state.activeProfile?.name ?: "-"}")
            Text("Execution status: ${state.executionStatus.state}")
            Text("Click count: ${state.executionStatus.clickCount}")
            Text("Last action: ${state.executionStatus.lastAction}")
        }
    }
}
