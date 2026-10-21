package io.github.greavemrsvg.dogwalkbuddy.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.dp

/** Minimum touch target (AC51). */
val MinTouchTarget = 48.dp

/** Minimum height of the full-width Start walk and Stop controls (AC51). */
val StartStopMinHeight = 72.dp

@Composable
fun DogWalkBuddyTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    CompositionLocalProvider(LocalRatingColors provides if (dark) DarkRatingColors else LightRatingColors) {
        MaterialTheme(colorScheme = if (dark) DarkColors else LightColors, content = content)
    }
}
