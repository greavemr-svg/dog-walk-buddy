package io.github.greavemrsvg.dogwalkbuddy.ui.profile

import androidx.compose.material3.ColorScheme
import io.github.greavemrsvg.dogwalkbuddy.testutil.Wcag
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.DarkColors
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.LightColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * AC51 for the profile screen: text contrast is at least 4.5:1 in light and dark themes. The profile screen uses
 * the shared theme tokens, which `ContrastTest` also checks (including the 7:1 rule for the rating and Start/Stop).
 */
class ProfileContrastTest {
    private fun assertTextPairsMeetAa(
        scheme: ColorScheme,
        themeName: String,
    ) {
        val pairs =
            mapOf(
                "body text" to (scheme.onSurface to scheme.surface),
                "field label and hint" to (scheme.onSurfaceVariant to scheme.surface),
                "error text" to (scheme.error to scheme.surface),
                "link and outlined button text" to (scheme.primary to scheme.surface),
                "Save button text" to (scheme.onPrimary to scheme.primary),
            )
        pairs.forEach { (what, colours) ->
            val ratio = Wcag.contrastRatio(colours.first, colours.second)
            assertTrue("$themeName $what has contrast $ratio", ratio >= MIN_TEXT_CONTRAST)
        }
    }

    @Test
    fun AC51_profile_text_has_at_least_4_5_to_1_contrast_in_the_light_theme() = assertTextPairsMeetAa(LightColors, "light")

    @Test
    fun AC51_profile_text_has_at_least_4_5_to_1_contrast_in_the_dark_theme() = assertTextPairsMeetAa(DarkColors, "dark")

    @Test
    fun AC51_the_contrast_helper_matches_the_wcag_reference_values() {
        assertEquals(21.0, Wcag.contrastRatio(androidx.compose.ui.graphics.Color.Black, androidx.compose.ui.graphics.Color.White), 0.001)
        assertEquals(1.0, Wcag.contrastRatio(androidx.compose.ui.graphics.Color.White, androidx.compose.ui.graphics.Color.White), 0.001)
    }

    private companion object {
        const val MIN_TEXT_CONTRAST = 4.5
    }
}
