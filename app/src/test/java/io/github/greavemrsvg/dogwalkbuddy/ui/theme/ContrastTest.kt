package io.github.greavemrsvg.dogwalkbuddy.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import io.github.greavemrsvg.dogwalkbuddy.testutil.Wcag
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * AC51: the shared theme tokens meet WCAG 2.1 contrast in both themes. Text is at least 4.5:1. The rating, the
 * reason and Start/Stop are at least 7:1, so they read in bright sunlight. Borders are at least 3:1.
 */
class ContrastTest {
    private fun assertRatio(
        what: String,
        foreground: Color,
        background: Color,
        minimum: Double,
    ) {
        val ratio = Wcag.contrastRatio(foreground, background)
        assertTrue("$what has contrast $ratio, below $minimum", ratio >= minimum)
    }

    private fun assertTextContrast(
        scheme: ColorScheme,
        name: String,
    ) {
        assertRatio("$name body text", scheme.onSurface, scheme.surface, AA_TEXT)
        assertRatio("$name text on background", scheme.onBackground, scheme.background, AA_TEXT)
        assertRatio("$name secondary text", scheme.onSurfaceVariant, scheme.surface, AA_TEXT)
        assertRatio("$name error text", scheme.error, scheme.surface, AA_TEXT)
        assertRatio("$name link and outlined button text", scheme.primary, scheme.surface, AA_TEXT)
        assertRatio("$name filled button text", scheme.onPrimary, scheme.primary, AA_TEXT)
        assertRatio("$name error button text", scheme.onError, scheme.error, AA_TEXT)
        assertRatio("$name border", scheme.outline, scheme.surface, AA_NON_TEXT)
    }

    private fun assertKeyInformationContrast(
        scheme: ColorScheme,
        ratingColors: RatingColors,
        name: String,
    ) {
        assertRatio("$name reason text", scheme.onSurface, scheme.surface, KEY_TEXT)
        assertRatio("$name Go rating", ratingColors.go, scheme.surface, KEY_TEXT)
        assertRatio("$name Caution rating", ratingColors.caution, scheme.surface, KEY_TEXT)
        assertRatio("$name Avoid rating", ratingColors.avoid, scheme.surface, KEY_TEXT)
        assertRatio("$name Start button", scheme.onPrimary, scheme.primary, KEY_TEXT)
        assertRatio("$name Stop button", scheme.onError, scheme.error, KEY_TEXT)
    }

    @Test
    fun AC51_text_has_at_least_4_5_to_1_contrast_in_the_light_theme() = assertTextContrast(LightColors, "light")

    @Test
    fun AC51_text_has_at_least_4_5_to_1_contrast_in_the_dark_theme() = assertTextContrast(DarkColors, "dark")

    @Test
    fun AC51_rating_reason_and_start_stop_have_at_least_7_to_1_contrast_in_the_light_theme() =
        assertKeyInformationContrast(LightColors, LightRatingColors, "light")

    @Test
    fun AC51_rating_reason_and_start_stop_have_at_least_7_to_1_contrast_in_the_dark_theme() =
        assertKeyInformationContrast(DarkColors, DarkRatingColors, "dark")

    @Test
    fun AC53_the_three_ratings_use_three_different_colours_in_each_theme() {
        listOf(LightRatingColors, DarkRatingColors).forEach {
            assertTrue(setOf(it.go, it.caution, it.avoid).size == 3)
        }
    }

    private companion object {
        const val AA_TEXT = 4.5
        const val AA_NON_TEXT = 3.0
        const val KEY_TEXT = 7.0
    }
}
