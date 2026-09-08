package com.autoclicker.pro.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AccentBlue = Color(0xFF4F8CFF)
private val AccentRed = Color(0xFFFF5252)
private val DarkBackground = Color(0xFF10121C)

private val DarkColors = darkColorScheme(
    primary = AccentBlue,
    secondary = AccentRed,
    background = DarkBackground,
    surface = Color(0xFF1B1F2E)
)

private val LightColors = lightColorScheme(
    primary = AccentBlue,
    secondary = AccentRed
)

@Composable
fun AutoClickerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = MaterialTheme.typography,
        content = content
    )
}
