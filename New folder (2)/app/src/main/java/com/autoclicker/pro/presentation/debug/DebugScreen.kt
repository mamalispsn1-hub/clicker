package com.autoclicker.pro.presentation.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import com.autoclicker.pro.presentation.main.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsState()
    val screenInfo = viewModel.coordinateManager.currentScreenInfo()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Debug") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            DebugRow("Screen size", "${screenInfo.widthPx} x ${screenInfo.heightPx}")
            DebugRow("Density", "${screenInfo.density} (dpi=${screenInfo.densityDpi})")
            DebugRow("Orientation", if (screenInfo.isPortrait) "Portrait" else "Landscape")
            DebugRow("Accessibility status", if (state.accessibilityServiceReady) "Connected" else "Disconnected")
            DebugRow("Overlay status", if (state.overlayPermissionGranted) "Granted" else "Not granted")
            DebugRow("Current profile", state.activeProfile?.name ?: "-")
            DebugRow("Current point", state.executionStatus.currentPointLabel.ifBlank { "-" })
            DebugRow("Current state", state.executionStatus.state.name)
            DebugRow("Click count", state.executionStatus.clickCount.toString())
            DebugRow("Last action", state.executionStatus.lastAction)
            DebugRow("Execution latency", "${state.executionStatus.lastExecutionLatencyMs}ms")
            DebugRow("Condition latency", "${state.executionStatus.lastConditionLatencyMs}ms")
            DebugRow("OCR latency", "${state.executionStatus.lastOcrLatencyMs}ms")
            DebugRow("Condition result", state.executionStatus.lastConditionResult)
            DebugRow("Screenshot capture supported", if (com.autoclicker.pro.accessibility.AutoClickAccessibilityService.isScreenshotCaptureSupported) "Yes (API 30+)" else "No — needs Android 11+")
        }
    }
}

@Composable
private fun DebugRow(label: String, value: String) {
    Text(text = "$label: $value", fontFamily = FontFamily.Monospace)
}
