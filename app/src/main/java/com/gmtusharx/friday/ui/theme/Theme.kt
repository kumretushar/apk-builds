package com.gmtusharx.friday.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Midnight = Color(0xFF040612)
val Navy = Color(0xFF0A1030)
val Panel = Color(0xFF101A3E)
val NeonCyan = Color(0xFF00E5FF)
val NeonPurple = Color(0xFF9D4DFF)
val NeonMagenta = Color(0xFFFF4DDE)
val TextPrimary = Color(0xFFE8F1FF)
val TextMuted = Color(0xFF8FA3C8)

private val FridayColors = darkColorScheme(
    primary = NeonCyan,
    onPrimary = Midnight,
    secondary = NeonPurple,
    tertiary = NeonMagenta,
    background = Midnight,
    onBackground = TextPrimary,
    surface = Panel,
    onSurface = TextPrimary,
    surfaceVariant = Navy,
    onSurfaceVariant = TextMuted,
    outline = NeonCyan.copy(alpha = 0.4f)
)

@Composable
fun FridayTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = FridayColors, content = content)
}
