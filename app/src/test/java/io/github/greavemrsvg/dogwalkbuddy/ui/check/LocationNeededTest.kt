package io.github.greavemrsvg.dogwalkbuddy.ui.check

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.greavemrsvg.dogwalkbuddy.testutil.TestAppContainer
import io.github.greavemrsvg.dogwalkbuddy.ui.a11y.assertAccessibleScreen
import io.github.greavemrsvg.dogwalkbuddy.ui.a11y.assertHeadingsMarked
import io.github.greavemrsvg.dogwalkbuddy.ui.a11y.assertHiddenFromAccessibility
import io.github.greavemrsvg.dogwalkbuddy.ui.a11y.assertTouchTargetIsAtLeast
import io.github.greavemrsvg.dogwalkbuddy.ui.common.PERMISSION_ILLUSTRATION_TAG
import io.github.greavemrsvg.dogwalkbuddy.ui.nav.DogWalkBuddyNavHost
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.DogWalkBuddyTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Today without approximate location (AC30, AC31, AC58; MVP AC25, AC27, AC46). */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class LocationNeededTest {
    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val host = TodayStateHost(composeRule)

    private fun top(text: String) = composeRule.onNodeWithText(text, useUnmergedTree = true).getBoundsInRoot().top

    private fun assertExplanationInOrder(button: String) {
        composeRule.assertHiddenFromAccessibility(PERMISSION_ILLUSTRATION_TAG)
        val order =
            listOf(
                composeRule.onNodeWithTag(PERMISSION_ILLUSTRATION_TAG, useUnmergedTree = true).getBoundsInRoot().top,
                top(HEADING),
                top(BODY),
                top(BULLETS[0]),
                top(BULLETS[1]),
                top(BULLETS[2]),
                top(button),
                top(LINK),
            )
        assertEquals("Illustration, heading, body, bullets, button, link", order.sortedBy { it.value }, order)
        composeRule.onNodeWithText(HEADING).assertIsDisplayed()
        composeRule.assertHeadingsMarked(HEADING)
        BULLETS.forEach { composeRule.onNodeWithText(it).assertIsDisplayed() }
    }

    @Test
    fun UX_AC30_the_explanation_shows_the_illustration_heading_body_bullets_button_and_link() {
        host.show(CheckUiState.NeedsLocationPermission)

        assertExplanationInOrder("Allow approximate location")
        val allow = composeRule.onNodeWithText("Allow approximate location").assertIsDisplayed()
        allow.assertTouchTargetIsAtLeast(composeRule, 48.dp, 48.dp)
        assertTrue(allow.getBoundsInRoot().height >= 60.dp)
        allow.performClick()
        assertEquals(1, host.grants)
        composeRule
            .onNodeWithText(LINK)
            .performScrollTo()
            .assertTouchTargetIsAtLeast(composeRule, 48.dp, 48.dp)
            .performClick()
        assertEquals(1, host.privacyOpens)
        composeRule.onNodeWithText("Weather data: MET Norway (CC BY 4.0)").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag(RATING_BLOCK_TAG, useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun UX_AC31_after_a_permanent_refusal_the_same_explanation_offers_open_settings() {
        host.show(CheckUiState.LocationPermissionBlocked)

        assertExplanationInOrder("Open settings")
        composeRule.onNodeWithText("Allow approximate location").assertDoesNotExist()
        val settings = composeRule.onNodeWithText("Open settings").assertIsDisplayed()
        settings.assertTouchTargetIsAtLeast(composeRule, 48.dp, 48.dp)
        settings.performClick()
        assertEquals(1, host.settingsOpens)
        assertEquals(0, host.grants)
    }

    @Test
    fun UX_AC30_in_the_app_the_link_opens_privacy_and_no_weather_request_is_made() {
        val test = TestAppContainer(temporaryFolder)
        try {
            test.permissionGranted = false
            composeRule.setContent { DogWalkBuddyTheme { DogWalkBuddyNavHost(container = test.container) } }
            awaitText(HEADING)

            composeRule.onNodeWithText("Allow approximate location").assertIsDisplayed()
            composeRule.onNodeWithText(LINK).performScrollTo().performClick()

            awaitText("Privacy")
            assertEquals(0, test.stub.server.requestCount)
        } finally {
            test.close()
        }
    }

    @Test
    fun UX_AC58_the_location_explanation_is_accessible_in_light() = assertAccessible(night = false)

    @Test
    fun UX_AC58_the_location_explanation_is_accessible_in_dark() = assertAccessible(night = true)

    private fun assertAccessible(night: Boolean) {
        host.show(CheckUiState.NeedsLocationPermission, night = night)

        composeRule.assertAccessibleScreen()
        composeRule.assertHeadingsMarked("Good time to walk?", HEADING)
        composeRule.assertHiddenFromAccessibility(PERMISSION_ILLUSTRATION_TAG)
    }

    private fun awaitText(text: String) {
        composeRule.waitUntil(WAIT_MS) {
            composeRule.onAllNodes(hasText(text), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private companion object {
        const val WAIT_MS = 10_000L
        const val HEADING = "The check needs your approximate location"
        const val BODY = "So we can get the weather where you are. Your approximate location is enough for this."
        const val LINK = "How your data is used"
        val BULLETS =
            listOf(
                "Rounded to about 1 km before it leaves your phone",
                "Only sent when you open the check or tap refresh",
                "Sent only to the weather service, MET Norway",
            )
    }
}
