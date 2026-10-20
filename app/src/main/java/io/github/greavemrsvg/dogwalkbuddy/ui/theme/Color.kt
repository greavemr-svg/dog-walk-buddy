package io.github.greavemrsvg.dogwalkbuddy.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * High-contrast tokens shared by every screen (AC51). Text is at least 4.5:1 on the surface. The rating, the
 * reason and Start/Stop are at least 7:1, so they read in bright sunlight. `ContrastTest` checks every pair.
 */
val LightColors: ColorScheme =
    lightColorScheme(
        primary = Color(0xFF1B5E20),
        onPrimary = Color(0xFFFFFFFF),
        background = Color(0xFFFFFFFF),
        onBackground = Color(0xFF1A1C1E),
        surface = Color(0xFFFFFFFF),
        onSurface = Color(0xFF1A1C1E),
        onSurfaceVariant = Color(0xFF3E4348),
        outline = Color(0xFF5F6368),
        error = Color(0xFF8C1D18),
        onError = Color(0xFFFFFFFF),
    )

val DarkColors: ColorScheme =
    darkColorScheme(
        primary = Color(0xFFA5D6A7),
        onPrimary = Color(0xFF00390F),
        background = Color(0xFF121212),
        onBackground = Color(0xFFF1F1F1),
        surface = Color(0xFF121212),
        onSurface = Color(0xFFF1F1F1),
        onSurfaceVariant = Color(0xFFCCCCCC),
        outline = Color(0xFF9AA0A6),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
    )

/** The colour of the rating label and icon. Colour is never the only difference (AC53): text and icon differ too. */
data class RatingColors(
    val go: Color,
    val caution: Color,
    val avoid: Color,
)

val LightRatingColors = RatingColors(go = Color(0xFF1B5E20), caution = Color(0xFF7A4400), avoid = Color(0xFF9B1B1B))

val DarkRatingColors = RatingColors(go = Color(0xFFA5D6A7), caution = Color(0xFFFFCC80), avoid = Color(0xFFFF9A9A))

val LocalRatingColors = staticCompositionLocalOf { LightRatingColors }
