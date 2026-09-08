package com.autoclicker.pro.presentation.profiles

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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.autoclicker.pro.presentation.main.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfilesScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsState()
    var newProfileName by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profiles") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newProfileName,
                    onValueChange = { newProfileName = it },
                    label = { Text("New profile name") },
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = {
                        if (newProfileName.isNotBlank()) {
                            viewModel.createProfile(newProfileName)
                            newProfileName = ""
                        }
                    },
                    modifier = Modifier.padding(start = 8.dp)
                ) { Text("Add") }
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.profileCollection.profiles, key = { it.id }) { profile ->
                    Card {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = profile.id == state.profileCollection.activeProfileId,
                                onClick = { viewModel.switchProfile(profile.id) }
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(profile.name)
                                Text("${profile.sequence.points.size} points", style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
                            }
                            IconButton(
                                onClick = { viewModel.deleteProfile(profile.id) },
                                enabled = state.profileCollection.profiles.size > 1
                            ) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete profile")
                            }
                        }
                    }
                }
            }
        }
    }
}
