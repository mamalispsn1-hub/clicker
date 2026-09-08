package com.autoclicker.pro.presentation.appbindings

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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.autoclicker.pro.data.model.AppBinding
import com.autoclicker.pro.presentation.main.MainViewModel
import com.autoclicker.pro.utils.InstalledAppInfo

/**
 * Lets the user bind installed apps to profiles (generic "when app X opens,
 * switch to profile Y" — same idea as per-app volume/DND). Purely a picker
 * over the user's own installed-app list; nothing here targets a specific app.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBindingsScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsState()
    var installedApps by remember { mutableStateOf<List<InstalledAppInfo>>(emptyList()) }

    LaunchedEffect(Unit) {
        installedApps = viewModel.listInstalledApps()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("App ↔ profile bindings") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

            Text("Existing bindings", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
            if (state.appBindings.bindings.isEmpty()) {
                Text("None yet — add one below.")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.appBindings.bindings, key = { it.packageName }) { binding ->
                        Card {
                            Row(
                                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(binding.appLabel)
                                    val profileName = state.profileCollection.profiles
                                        .firstOrNull { it.id == binding.profileId }?.name ?: "Unknown profile"
                                    Text("→ $profileName", style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
                                }
                                IconButton(onClick = { viewModel.removeAppBinding(binding.packageName) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Remove binding")
                                }
                            }
                        }
                    }
                }
            }

            Text("Add a binding", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
            AddBindingForm(
                installedApps = installedApps,
                profiles = state.profileCollection.profiles,
                onAdd = { app, profileId -> viewModel.upsertAppBinding(AppBinding(app.packageName, app.label, profileId)) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddBindingForm(
    installedApps: List<InstalledAppInfo>,
    profiles: List<com.autoclicker.pro.data.model.Profile>,
    onAdd: (InstalledAppInfo, String) -> Unit
) {
    var selectedApp by remember { mutableStateOf<InstalledAppInfo?>(null) }
    var selectedProfileId by remember { mutableStateOf(profiles.firstOrNull()?.id) }
    var appExpanded by remember { mutableStateOf(false) }
    var profileExpanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(expanded = appExpanded, onExpandedChange = { appExpanded = it }) {
        OutlinedTextField(
            readOnly = true,
            value = selectedApp?.label ?: "Choose an app",
            onValueChange = {},
            label = { Text("App") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = appExpanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )
        DropdownMenu(expanded = appExpanded, onDismissRequest = { appExpanded = false }) {
            installedApps.forEach { app ->
                DropdownMenuItem(
                    text = { Text(app.label) },
                    onClick = { selectedApp = app; appExpanded = false }
                )
            }
        }
    }

    ExposedDropdownMenuBox(expanded = profileExpanded, onExpandedChange = { profileExpanded = it }) {
        OutlinedTextField(
            readOnly = true,
            value = profiles.firstOrNull { it.id == selectedProfileId }?.name ?: "Choose a profile",
            onValueChange = {},
            label = { Text("Profile") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = profileExpanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )
        DropdownMenu(expanded = profileExpanded, onDismissRequest = { profileExpanded = false }) {
            profiles.forEach { profile ->
                DropdownMenuItem(
                    text = { Text(profile.name) },
                    onClick = { selectedProfileId = profile.id; profileExpanded = false }
                )
            }
        }
    }

    androidx.compose.material3.Button(
        onClick = {
            val app = selectedApp
            val profileId = selectedProfileId
            if (app != null && profileId != null) onAdd(app, profileId)
        },
        enabled = selectedApp != null && selectedProfileId != null,
        modifier = Modifier.fillMaxWidth()
    ) { Text("Add binding") }
}
