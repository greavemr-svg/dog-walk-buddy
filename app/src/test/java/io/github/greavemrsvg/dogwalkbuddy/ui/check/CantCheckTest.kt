package io.github.greavemrsvg.dogwalkbuddy.ui.check

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.greavemrsvg.dogwalkbuddy.ui.a11y.assertAccessibleScreen
import io.github.greavemrsvg.dogwalkbuddy.ui.a11y.assertHeadingsMarked
import io.github.greavemrsvg.dogwalkbuddy.ui.a11y.assertTouchTargetIsAtLeast
import io.github.greavemrsvg.dogwalkbuddy.ui.nav.NAV_TODAY_TAG
import io.github.greavemrsvg.dogwalkbuddy.ui.recording.START_WALK_TAG
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Today when the check cannot produce a result (AC29, AC58; MVP AC15, AC22, AC26, AC27). */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class CantCheckTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val host = TodayStateHost(composeRule)

    @Test
    fun UX_AC29_the_card_shows_the_exact_title_and_explanation_and_try_again_runs_a_check() {
        host.show(CheckUiState.CantCheck)

        composeRule.onNodeWithText("Can't check right now").assertIsDisplayed()
        composeRule
            .onNodeWithText(
                "We couldn't get the weather, and there's no recent check to fall back on. Check your connection and try again.",
            ).assertIsDisplayed()
        val retry = composeRule.onNodeWithText("Try again").assertIsDisplayed()
        retry.assertTouchTargetIsAtLeast(composeRule, 48.dp, 48.dp)
        assertTrue(retry.getBoundsInRoot().height >= 48.dp)
        retry.performClick()
        assertEquals(1, host.refreshes)
    }

    @Test
    fun UX_AC29_a_panel_says_a_walk_can_still_be_recorded_with_the_lead_in_bold() {
        host.show(CheckUiState.CantCheck)

        val lead = "You can still record a walk."
        val panel = composeRule.onNodeWithTag(CAN_STILL_RECORD_TAG, useUnmergedTree = true).assertIsDisplayed()
        val text = panel.fetchSemanticsNode().config[SemanticsProperties.Text].single()
        assertEquals("$lead Recording works without a connection and saves to your phone.", text.text)
        val bold = text.spanStyles.single { it.item.fontWeight == FontWeight.Bold }
        assertEquals(lead, text.text.substring(bold.start, bold.end))
    }

    @Test
    fun UX_AC29_the_disclaimer_attribution_and_about_link_show_and_no_part_of_a_result_does() {
        host.show(CheckUiState.CantCheck)

        composeRule
            .onNodeWithText("Guidance only, not veterinary advice. If you're worried about your dog, contact a vet.")
            .assertIsDisplayed()
        composeRule.onNodeWithText("Weather data: MET Norway (CC BY 4.0)").assertIsDisplayed()
        composeRule.onNodeWithText("About this advice").assertIsDisplayed().performClick()
        assertEquals(1, host.aboutOpens)
        composeRule.onNodeWithContentDescription("Refresh").assertDoesNotExist()
        composeRule.onNodeWithText("Your area · checked just now", useUnmergedTree = true).assertDoesNotExist()
        listOf("Go", "Caution", "Avoid").forEach { composeRule.onNodeWithText(it, useUnmergedTree = true).assertDoesNotExist() }
        composeRule.onNodeWithTag(RATING_BLOCK_TAG, useUnmergedTree = true).assertDoesNotExist()
        composeRule.onNodeWithTag(BEST_WINDOW_CARD_TAG, useUnmergedTree = true).assertDoesNotExist()
        composeRule.onNodeWithTag(HOUR_STRIP_TAG, useUnmergedTree = true).assertDoesNotExist()
        composeRule.onNodeWithTag(START_WALK_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(NAV_TODAY_TAG).assertIsDisplayed()
    }

    @Test
    fun UX_AC58_cant_check_is_accessible_in_light() = assertAccessible(night = false)

    @Test
    fun UX_AC58_cant_check_is_accessible_in_dark() = assertAccessible(night = true)

    private fun assertAccessible(night: Boolean) {
        host.show(CheckUiState.CantCheck, night = night)

        composeRule.assertAccessibleScreen()
        composeRule.assertHeadingsMarked("Good time to walk?", "Can't check right now")
    }
}
