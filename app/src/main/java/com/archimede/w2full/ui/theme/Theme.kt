package com.archimede.w2full.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PetrolNightColors = darkColorScheme(
    background = Color(0xFF101418),
    surface = Color(0xFF182028),
    primary = Color(0xFF33C3A5),
    secondary = Color(0xFF7FD1FF),
    tertiary = Color(0xFFFFB84D),
    error = Color(0xFFFF6B6B),
    onBackground = Color(0xFFF5F7FA),
    onSurface = Color(0xFFF5F7FA),
    onSurfaceVariant = Color(0xFFA9B4C2),
)

@Composable
fun W2FullTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PetrolNightColors,
        content = content,
    )
}
