package com.autoclicker.pro.presentation.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.autoclicker.pro.data.model.AVAILABLE_SPEEDS_MS
import com.autoclicker.pro.data.model.ClickCondition
import com.autoclicker.pro.data.model.ClickPoint
import com.autoclicker.pro.data.model.ConditionFailureAction
import com.autoclicker.pro.data.model.ExecutionMode
import com.autoclicker.pro.data.model.TextMatchMode
import com.autoclicker.pro.presentation.main.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClickPointEditorScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsState()
    val sequence = state.activeProfile?.sequence

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Click points") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

            ExecutionModeSelector(
                selected = sequence?.mode ?: ExecutionMode.SEQUENCE,
                onSelected = { viewModel.updateExecutionMode(it) }
            )

            SpeedSelector(
                selectedMs = sequence?.intervalMs ?: 100L,
                onSelected = { viewModel.updateInterval(it) }
            )

            RepeatSettingsRow(
                repeatCount = sequence?.repeatCount ?: 1,
                isInfinite = sequence?.isInfinite ?: false,
                startDelayMs = sequence?.startDelayMs ?: 0L,
                onChange = { count, infinite, startDelay -> viewModel.updateRepeatSettings(count, infinite, startDelay) }
            )

            Text("Points", fontWeight = FontWeight.Bold)

            val points = sequence?.points?.sortedBy { it.order } ?: emptyList()
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(points, key = { it.id }) { point ->
                    PointEditorCard(
                        point = point,
                        onUpdate = { viewModel.updatePoint(it) },
                        onDelete = { viewModel.deletePoint(point.id) },
                        onToggleEnabled = { viewModel.togglePointEnabled(point.id) },
                        onMoveUp = { viewModel.movePoint(point.id, up = true) },
                        onMoveDown = { viewModel.movePoint(point.id, up = false) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExecutionModeSelector(selected: ExecutionMode, onSelected: (ExecutionMode) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            readOnly = true,
            value = selected.displayName,
            onValueChange = {},
            label = { Text("Execution mode") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ExecutionMode.values().forEach { mode ->
                DropdownMenuItem(
                    text = { Text(mode.displayName) },
                    onClick = { onSelected(mode); expanded = false }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpeedSelector(selectedMs: Long, onSelected: (Long) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            readOnly = true,
            value = "${selectedMs}ms",
            onValueChange = {},
            label = { Text("Interval between clicks") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            AVAILABLE_SPEEDS_MS.forEach { ms ->
                DropdownMenuItem(
                    text = { Text("${ms}ms") },
                    onClick = { onSelected(ms); expanded = false }
                )
            }
        }
    }
}

@Composable
private fun RepeatSettingsRow(
    repeatCount: Int,
    isInfinite: Boolean,
    startDelayMs: Long,
    onChange: (Int, Boolean, Long) -> Unit
) {
    var repeatText by remember(repeatCount) { mutableStateOf(repeatCount.toString()) }
    var delayText by remember(startDelayMs) { mutableStateOf(startDelayMs.toString()) }
    var infinite by remember(isInfinite) { mutableStateOf(isInfinite) }

    Card {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text("Infinite repeat", modifier = Modifier.weight(1f))
                Switch(
                    checked = infinite,
                    onCheckedChange = {
                        infinite = it
                        onChange(repeatText.toIntOrNull() ?: 1, it, delayText.toLongOrNull() ?: 0L)
                    }
                )
            }
            if (!infinite) {
                OutlinedTextField(
                    value = repeatText,
                    onValueChange = {
                        repeatText = it
                        onChange(it.toIntOrNull() ?: 1, infinite, delayText.toLongOrNull() ?: 0L)
                    },
                    label = { Text("Repeat count") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            OutlinedTextField(
                value = delayText,
                onValueChange = {
                    delayText = it
                    onChange(repeatText.toIntOrNull() ?: 1, infinite, it.toLongOrNull() ?: 0L)
                },
                label = { Text("Start delay (ms)") },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun PointEditorCard(
    point: ClickPoint,
    onUpdate: (ClickPoint) -> Unit,
    onDelete: () -> Unit,
    onToggleEnabled: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    var delayText by remember(point.id) { mutableStateOf(point.delayMs.toString()) }
    var durationText by remember(point.id) { mutableStateOf(point.clickDurationMs.toString()) }
    var repeatText by remember(point.id) { mutableStateOf(point.repeatCount.toString()) }

    Card {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    point.label.ifBlank { "Point ${point.order + 1}" },
                    fontWeight = FontWeight.Bold
                )
                Row {
                    IconButton(onClick = onMoveUp) { Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Move up") }
                    IconButton(onClick = onMoveDown) { Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Move down") }
                    IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = "Delete") }
                }
            }
            Text("X: ${"%.3f".format(point.xPercent)}   Y: ${"%.3f".format(point.yPercent)}")

            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text("Enabled", modifier = Modifier.weight(1f))
                Switch(checked = point.isEnabled, onCheckedChange = { onToggleEnabled() })
            }

            OutlinedTextField(
                value = delayText,
                onValueChange = {
                    delayText = it
                    onUpdate(point.copy(delayMs = it.toLongOrNull() ?: point.delayMs))
                },
                label = { Text("Delay after tap (ms)") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = durationText,
                onValueChange = {
                    durationText = it
                    onUpdate(point.copy(clickDurationMs = it.toLongOrNull() ?: point.clickDurationMs))
                },
                label = { Text("Click duration (ms)") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = repeatText,
                onValueChange = {
                    repeatText = it
                    onUpdate(point.copy(repeatCount = it.toIntOrNull() ?: point.repeatCount))
                },
                label = { Text("Repeat count for this point") },
                modifier = Modifier.fillMaxWidth()
            )

            JitterSection(point = point, onUpdate = onUpdate)
            ConditionSection(point = point, onUpdate = onUpdate)
        }
    }
}

@Composable
private fun JitterSection(point: ClickPoint, onUpdate: (ClickPoint) -> Unit) {
    var delayJitterText by remember(point.id) { mutableStateOf(point.delayJitterMs.toString()) }
    var positionJitterText by remember(point.id) { mutableStateOf(point.positionJitterPx.toString()) }

    Text("Randomization (optional)", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
    OutlinedTextField(
        value = delayJitterText,
        onValueChange = {
            delayJitterText = it
            onUpdate(point.copy(delayJitterMs = it.toLongOrNull() ?: point.delayJitterMs))
        },
        label = { Text("Delay jitter, up to ± (ms)") },
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = positionJitterText,
        onValueChange = {
            positionJitterText = it
            onUpdate(point.copy(positionJitterPx = it.toFloatOrNull() ?: point.positionJitterPx))
        },
        label = { Text("Position jitter, up to ± (px)") },
        modifier = Modifier.fillMaxWidth()
    )
}

/**
 * Optional "wait for text on screen" pre-condition for this point. Entirely
 * generic: the user draws the region and types the text themselves — nothing
 * here assumes a specific target app or UI.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConditionSection(point: ClickPoint, onUpdate: (ClickPoint) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val requestId = remember(point.id) { point.id }
    val existing = point.condition as? ClickCondition.OcrText

    var enabled by remember(point.id) { mutableStateOf(existing != null) }
    var region by remember(point.id) { mutableStateOf(existing?.region) }
    var expectedText by remember(point.id) { mutableStateOf(existing?.expectedText ?: "") }
    var matchMode by remember(point.id) { mutableStateOf(existing?.matchMode ?: TextMatchMode.CONTAINS) }
    var expectPresent by remember(point.id) { mutableStateOf(existing?.expectPresent ?: true) }
    var caseSensitive by remember(point.id) { mutableStateOf(existing?.caseSensitive ?: false) }
    var timeoutText by remember(point.id) { mutableStateOf((existing?.timeoutMs ?: 5000L).toString()) }
    var pollText by remember(point.id) { mutableStateOf((existing?.pollIntervalMs ?: 250L).toString()) }
    var onTimeout by remember(point.id) { mutableStateOf(existing?.onTimeout ?: ConditionFailureAction.SKIP_POINT) }
    var maxRetriesText by remember(point.id) { mutableStateOf((existing?.maxRetries ?: 1).toString()) }

    // A region picked in the overlay arrives here asynchronously.
    val pickedRegion by com.autoclicker.pro.overlay.RegionPickerBus.result.collectAsState()
    LaunchedEffect(pickedRegion) {
        val match = com.autoclicker.pro.overlay.RegionPickerBus.consumeIfMatches(requestId)
        if (match != null) region = match
    }

    fun pushCondition() {
        val r = region
        if (!enabled || r == null || expectedText.isBlank()) {
            onUpdate(point.copy(condition = ClickCondition.None))
            return
        }
        onUpdate(
            point.copy(
                condition = ClickCondition.OcrText(
                    region = r,
                    expectedText = expectedText,
                    matchMode = matchMode,
                    expectPresent = expectPresent,
                    caseSensitive = caseSensitive,
                    timeoutMs = timeoutText.toLongOrNull() ?: 5000L,
                    pollIntervalMs = pollText.toLongOrNull() ?: 250L,
                    onTimeout = onTimeout,
                    maxRetries = maxRetriesText.toIntOrNull() ?: 1
                )
            )
        )
    }

    Text("Wait-for-text condition (optional)", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Text("Only tap once a screen region matches text I define", modifier = Modifier.weight(1f))
        Switch(checked = enabled, onCheckedChange = { enabled = it; pushCondition() })
    }

    if (!enabled) return

    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Text(
            region?.let { "Region set (${"%.2f".format(it.leftPercent)}, ${"%.2f".format(it.topPercent)}) → (${"%.2f".format(it.rightPercent)}, ${"%.2f".format(it.bottomPercent)})" }
                ?: "No region selected yet",
            modifier = Modifier.weight(1f)
        )
        androidx.compose.material3.OutlinedButton(onClick = {
            com.autoclicker.pro.overlay.OverlayService.pickRegion(context, requestId)
        }) { Text("Pick region") }
    }

    OutlinedTextField(
        value = expectedText,
        onValueChange = { expectedText = it; pushCondition() },
        label = { Text("Text to look for (Latin script only — see note below)") },
        modifier = Modifier.fillMaxWidth()
    )
    Text(
        "Note: on-device OCR only reads Latin-script text (English, numbers, etc). " +
            "It cannot read Persian/Arabic script.",
        style = androidx.compose.material3.MaterialTheme.typography.bodySmall
    )

    var matchModeExpanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = matchModeExpanded, onExpandedChange = { matchModeExpanded = it }) {
        OutlinedTextField(
            readOnly = true,
            value = matchMode.displayName,
            onValueChange = {},
            label = { Text("Match mode") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = matchModeExpanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )
        DropdownMenu(expanded = matchModeExpanded, onDismissRequest = { matchModeExpanded = false }) {
            TextMatchMode.values().forEach { mode ->
                DropdownMenuItem(
                    text = { Text(mode.displayName) },
                    onClick = { matchMode = mode; matchModeExpanded = false; pushCondition() }
                )
            }
        }
    }

    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Text("Wait until text appears (off = wait until it disappears)", modifier = Modifier.weight(1f))
        Switch(checked = expectPresent, onCheckedChange = { expectPresent = it; pushCondition() })
    }
    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Text("Case sensitive", modifier = Modifier.weight(1f))
        Switch(checked = caseSensitive, onCheckedChange = { caseSensitive = it; pushCondition() })
    }

    OutlinedTextField(
        value = timeoutText,
        onValueChange = { timeoutText = it; pushCondition() },
        label = { Text("Timeout (ms)") },
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = pollText,
        onValueChange = { pollText = it; pushCondition() },
        label = { Text("Check interval while waiting (ms)") },
        modifier = Modifier.fillMaxWidth()
    )

    var onTimeoutExpanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = onTimeoutExpanded, onExpandedChange = { onTimeoutExpanded = it }) {
        OutlinedTextField(
            readOnly = true,
            value = onTimeout.displayName,
            onValueChange = {},
            label = { Text("If it times out") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = onTimeoutExpanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )
        DropdownMenu(expanded = onTimeoutExpanded, onDismissRequest = { onTimeoutExpanded = false }) {
            ConditionFailureAction.values().forEach { action ->
                DropdownMenuItem(
                    text = { Text(action.displayName) },
                    onClick = { onTimeout = action; onTimeoutExpanded = false; pushCondition() }
                )
            }
        }
    }

    if (onTimeout == ConditionFailureAction.RETRY_THEN_SKIP) {
        OutlinedTextField(
            value = maxRetriesText,
            onValueChange = { maxRetriesText = it; pushCondition() },
            label = { Text("Max retries") },
            modifier = Modifier.fillMaxWidth()
        )
    }
}
