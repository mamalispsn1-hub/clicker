package com.autoclicker.pro.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.autoclicker.pro.data.model.ExecutionState

/**
 * The always-on-top floating control strip. Deliberately small and thumb-reachable —
 * this sits on top of whatever app the user is automating, so screen real estate
 * and touch-target size both matter more than on the in-app screens.
 */
@Composable
fun FloatingPanel(
    executionState: ExecutionState,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit,
    onAddPoint: () -> Unit,
    onOpenApp: () -> Unit,
    onSettings: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = Color(0xE6141826),
        shadowElevation = 8.dp,
        modifier = Modifier.padding(4.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
        ) {
            PanelIconButton(
                icon = if (executionState == ExecutionState.RUNNING) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                tint = Color(0xFF4CD964),
                contentDescription = if (executionState == ExecutionState.RUNNING) "Pause" else "Start",
                onClick = if (executionState == ExecutionState.RUNNING) onPause else onStart
            )
            PanelIconButton(
                icon = Icons.Filled.Stop,
                tint = Color(0xFFFF5252),
                contentDescription = "Emergency stop",
                onClick = onStop
            )
            PanelIconButton(
                icon = Icons.Filled.Add,
                tint = Color(0xFF4F8CFF),
                contentDescription = "Add click point",
                onClick = onAddPoint
            )
            PanelIconButton(
                icon = Icons.Filled.Menu,
                tint = Color.White,
                contentDescription = "Open app",
                onClick = onOpenApp
            )
            PanelIconButton(
                icon = Icons.Filled.Settings,
                tint = Color.White,
                contentDescription = "Settings",
                onClick = onSettings
            )
        }
    }
}

@Composable
private fun PanelIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    contentDescription: String,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick) {
        Icon(imageVector = icon, contentDescription = contentDescription, tint = tint)
    }
}
