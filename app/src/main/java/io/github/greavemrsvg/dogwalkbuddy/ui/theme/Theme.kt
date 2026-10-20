package io.github.greavemrsvg.dogwalkbuddy.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

/** Minimum touch target (AC51). */
val MinTouchTarget = 48.dp

// Placeholder theme. The high-contrast tokens arrive with the check screen (task 5).
val LightColors: ColorScheme = lightColorScheme()
val DarkColors: ColorScheme = darkColorScheme()

@Composable
fun DogWalkBuddyTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, content = content)
}
