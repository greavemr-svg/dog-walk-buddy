package io.github.greavemrsvg.dogwalkbuddy.ui.check

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.greavemrsvg.dogwalkbuddy.testutil.MetBodies
import io.github.greavemrsvg.dogwalkbuddy.testutil.TestAppContainer
import io.github.greavemrsvg.dogwalkbuddy.ui.a11y.WithFontScale
import io.github.greavemrsvg.dogwalkbuddy.ui.a11y.assertAccessibleScreen
import io.github.greavemrsvg.dogwalkbuddy.ui.a11y.assertNoTextTruncation
import io.github.greavemrsvg.dogwalkbuddy.ui.nav.DogWalkBuddyNavHost
import io.github.greavemrsvg.dogwalkbuddy.ui.nav.NAV_DOG_TAG
import io.github.greavemrsvg.dogwalkbuddy.ui.nav.NAV_TODAY_TAG
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.DogWalkBuddyTheme
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Duration
import java.time.Instant

/**
 * The Home screen's check card through the real navigation, ViewModel, repository and an HTTPS stub of MET Norway
 * (design, Test strategy: integration tests).
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class CheckScreenIntegrationTest {
    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var test: TestAppContainer

    @Before
    fun setUp() {
        test = TestAppContainer(temporaryFolder)
    }

    @After
    fun tearDown() {
        test.close()
    }

    private fun showApp(fontScale: Float = 1f) {
        composeRule.setContent {
            DogWalkBuddyTheme {
                WithFontScale(fontScale) { DogWalkBuddyNavHost(container = test.container) }
            }
        }
    }

    private fun awaitText(text: String) {
        composeRule.waitUntil(WAIT_MS) {
            composeRule.onAllNodes(hasText(text), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun checkOnce() = runBlocking { test.container.checkRepository.check(test.location) }

    private fun enqueueTemperature(temperatureC: Double) =
        test.stub.enqueueBody(MetBodies.hourly(Instant.parse("2026-10-19T14:00:00Z"), temperatureC))

    @Test
    fun AC20_a_successful_check_shows_the_rating_reason_best_window_and_the_pollen_notice() {
        test.stub.enqueueFixture()

        showApp()
        awaitText("Good conditions for a walk")

        composeRule.onNodeWithText("Go", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText("Now until", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Pollen isn't included in this check").assertIsDisplayed()
        assertEquals(1, test.stub.server.requestCount)
    }

    @Test
    fun AC26_the_disclaimer_is_on_the_first_screen_without_scrolling_and_about_lists_the_sources() {
        test.stub.enqueueFixture()
        showApp()
        awaitText("Good conditions for a walk")

        composeRule
            .onNodeWithText("Guidance only, not veterinary advice. If you're worried about your dog, contact a vet.")
            .assertIsDisplayed()
        composeRule.onAllNodesWithAboutLink()[0].performClick()

        composeRule.onNodeWithText("Vets Now", substring = true).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("FOUR PAWS UK", substring = true).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Heat (air temperature)", substring = true).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Wind (mean speed)", substring = true).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun AC26_about_lists_every_threshold_from_the_rating_rules() {
        test.stub.enqueueFixture()
        showApp()
        awaitText("Good conditions for a walk")
        composeRule.onAllNodesWithAboutLink()[0].performClick()

        listOf(
            "Normal: Go below 20 °C, Caution from 20 °C to below 25 °C, Avoid from 25 °C.",
            "Sensitive: Go below 16 °C, Caution from 16 °C to below 21 °C, Avoid from 21 °C.",
            "cloud cover is below 50 percent",
            "Go below 0.5 mm/h, Caution from 0.5 mm/h to 4 mm/h, Avoid above 4 mm/h.",
            "Go below 25 mph, Caution from 25 mph to below 39 mph, Avoid from 39 mph.",
            "Caution at 0 °C or below. Cold never gives Avoid.",
        ).forEach { composeRule.onNodeWithText(it, substring = true).assertExists() }
    }

    @Test
    fun AC27_the_attribution_is_on_the_check_screen_and_on_the_about_screen() {
        test.stub.enqueueFixture()
        showApp()
        awaitText("Good conditions for a walk")
        composeRule.onNodeWithText("Weather data: MET Norway (CC BY 4.0)").assertIsDisplayed()

        composeRule.onAllNodesWithAboutLink()[0].performClick()

        composeRule.onNodeWithText("Weather data: MET Norway (CC BY 4.0)").assertExists()
    }

    @Test
    fun AC26_the_about_screen_has_a_back_control_that_returns_to_the_check() {
        test.stub.enqueueFixture()
        showApp()
        awaitText("Good conditions for a walk")
        composeRule.onAllNodesWithAboutLink()[0].performClick()
        composeRule.onNodeWithText("Rating thresholds").assertIsDisplayed()
        composeRule.assertAccessibleScreen()

        composeRule.onNodeWithContentDescription("Back").performClick()

        composeRule.onNodeWithText("Good time to walk?").assertIsDisplayed()
        // Returning shows the result already in memory. It does not make another weather request.
        assertEquals(1, test.stub.server.requestCount)
    }

    @Test
    fun AC25_without_permission_the_explanation_is_shown_and_no_weather_request_is_made() {
        test.permissionGranted = false

        showApp()
        // The state leaves Loading on the ViewModel's own dispatch, so wait for the explanation instead of reading at once.
        awaitText("The check needs your approximate location.")

        composeRule.onNodeWithText("The check needs your approximate location.").assertIsDisplayed()
        composeRule.onNodeWithText("Allow approximate location").assertIsDisplayed()
        assertEquals(0, test.stub.server.requestCount)
    }

    @Test
    fun AC21_offline_after_2h59m_the_previous_result_is_shown_with_its_age() {
        test.stub.enqueueFixture()
        checkOnce()
        test.clock.advance(Duration.ofMinutes(179))
        test.stub.enqueueStatus(SERVER_ERROR)

        showApp()
        awaitText("Checked 2 h 59 min ago")

        composeRule.onNodeWithText("Go", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText("Good conditions for a walk", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText("Best walking window").assertIsDisplayed()
    }

    @Test
    fun AC22_at_3h_with_a_failing_request_cant_check_is_shown_with_retry_and_no_rating() {
        test.stub.enqueueFixture()
        checkOnce()
        test.clock.advance(Duration.ofHours(3))
        test.stub.enqueueStatus(SERVER_ERROR)

        showApp()
        awaitText("Can't check right now")

        composeRule.onNodeWithText("Retry").assertIsDisplayed()
        composeRule.onNodeWithText("Good conditions for a walk", useUnmergedTree = true).assertDoesNotExist()
        composeRule.onNodeWithText("Go", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun AC15_retry_after_cant_check_runs_a_new_check_and_shows_the_result() {
        test.stub.enqueueStatus(SERVER_ERROR).enqueueFixture()
        showApp()
        awaitText("Can't check right now")

        composeRule.onNodeWithText("Retry").performClick()
        awaitText("Good conditions for a walk")

        assertEquals(2, test.stub.server.requestCount)
    }

    @Test
    fun AC15_a_response_with_no_temperature_rain_or_wind_for_the_current_hour_is_cant_check() {
        test.stub.enqueueBody(
            MetBodies.hourly(
                Instant.parse("2026-10-19T14:00:00Z"),
                temperatureC = null,
                windMetresPerSecond = null,
                precipitationMmPerHour = null,
            ),
        )

        showApp()
        awaitText("Can't check right now")

        composeRule.onNodeWithText("Go", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun AC45_with_no_profile_the_note_is_shown_and_opens_the_dog_profile() {
        test.stub.enqueueFixture()
        showApp()
        awaitText("Good conditions for a walk")

        composeRule
            .onNodeWithText("Using normal heat sensitivity. Set up your dog's profile to change this.")
            .assertIsDisplayed()
            .performClick()

        composeRule.onNodeWithText("Save profile").assertIsDisplayed()
    }

    @Test
    fun AC44_changing_the_dog_to_sensitive_re_rates_go_to_caution_with_no_new_request() {
        enqueueTemperature(17.0)
        showApp()
        awaitText("Good conditions for a walk")
        composeRule.onNodeWithTag(NAV_DOG_TAG).performClick()
        composeRule.onAllNodes(hasSetTextAction())[0].performTextInput("Biscuit")
        composeRule.onNodeWithText("Sensitive").performClick()
        composeRule.onNodeWithText("Save profile").performClick()
        awaitText("Profile saved")

        composeRule.onNodeWithTag(NAV_TODAY_TAG).performClick()

        awaitText("Warm: keep it short and bring water")
        composeRule.onNodeWithText("Caution", useUnmergedTree = true).assertIsDisplayed()
        assertEquals(1, test.stub.server.requestCount)
        // C1: with a profile the title names the dog.
        composeRule.onNodeWithText("Good time to walk Biscuit?").assertIsDisplayed()
        composeRule.onNodeWithText("Heat-sensitive thresholds (Biscuit)").assertIsDisplayed()
        // With a profile saved, the "no profile" note is gone.
        composeRule
            .onNodeWithText("Using normal heat sensitivity. Set up your dog's profile to change this.")
            .assertDoesNotExist()
    }

    @Test
    fun AC24_opening_the_home_screen_makes_exactly_one_weather_request() {
        test.stub.enqueueFixture()

        showApp()
        awaitText("Good conditions for a walk")

        assertEquals(1, test.stub.server.requestCount)
        val request = test.stub.server.takeRequest()
        assertEquals("51.51", request.url.queryParameter("lat"))
        assertEquals("-0.13", request.url.queryParameter("lon"))
    }

    @Test
    fun AC51_the_home_screen_controls_are_labelled_and_at_least_48dp() {
        test.stub.enqueueFixture()
        showApp()
        awaitText("Good conditions for a walk")

        composeRule.assertAccessibleScreen()
    }

    @Test
    fun AC52_the_home_screen_at_the_largest_font_size_is_not_truncated() {
        test.stub.enqueueFixture()
        showApp(fontScale = 2.0f)
        awaitText("Good conditions for a walk")

        composeRule.assertNoTextTruncation()
    }

    private fun ComposeContentTestRule.onAllNodesWithAboutLink() = onAllNodes(hasText("About this advice") and hasClickAction())

    private companion object {
        const val WAIT_MS = 10_000L
        const val SERVER_ERROR = 500
    }
}
