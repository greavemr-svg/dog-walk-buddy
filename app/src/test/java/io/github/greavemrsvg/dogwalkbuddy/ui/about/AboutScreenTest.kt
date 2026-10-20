package io.github.greavemrsvg.dogwalkbuddy.ui.about

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.greavemrsvg.dogwalkbuddy.ui.a11y.WithFontScale
import io.github.greavemrsvg.dogwalkbuddy.ui.a11y.assertAccessibleScreen
import io.github.greavemrsvg.dogwalkbuddy.ui.a11y.assertNoTextTruncation
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.DogWalkBuddyTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The About this advice screen on its own (AC26, AC27, AC51, AC52). */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class AboutScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private var backs = 0

    private fun show(fontScale: Float = 1f) {
        composeRule.setContent {
            DogWalkBuddyTheme {
                WithFontScale(fontScale) { AboutScreen(onBack = { backs++ }) }
            }
        }
    }

    @Test
    fun AC52_the_about_screen_has_no_truncated_text_at_the_largest_font_size() {
        show(fontScale = 2.0f)

        composeRule.assertNoTextTruncation()
        composeRule.onNodeWithText("Rating thresholds").assertExists()
    }

    @Test
    fun AC51_the_about_back_control_is_labelled_and_at_least_48dp() {
        show()

        composeRule.assertAccessibleScreen()
    }

    @Test
    fun AC26_about_names_both_guidance_sources_and_AC27_carries_the_attribution() {
        show()

        composeRule.onNodeWithText("Vets Now", substring = true).assertExists()
        composeRule.onNodeWithText("FOUR PAWS UK", substring = true).assertExists()
        composeRule.onNodeWithText("Weather data: MET Norway (CC BY 4.0)").assertExists()
    }

    @Test
    fun AC26_back_returns_from_about() {
        show()

        composeRule.onNodeWithText("Back").assertIsDisplayed().performClick()

        assertEquals(1, backs)
    }
}
