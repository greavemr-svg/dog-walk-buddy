package io.github.greavemrsvg.dogwalkbuddy.ui.recording

import android.Manifest
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.greavemrsvg.dogwalkbuddy.DogWalkBuddyApp
import io.github.greavemrsvg.dogwalkbuddy.testutil.FakePermissionRegistry
import io.github.greavemrsvg.dogwalkbuddy.testutil.TestAppContainer
import io.github.greavemrsvg.dogwalkbuddy.testutil.WithPermissionRegistry
import io.github.greavemrsvg.dogwalkbuddy.ui.a11y.WithFontScale
import io.github.greavemrsvg.dogwalkbuddy.ui.a11y.assertAccessibleScreen
import io.github.greavemrsvg.dogwalkbuddy.ui.a11y.assertHeadingsMarked
import io.github.greavemrsvg.dogwalkbuddy.ui.a11y.assertHiddenFromAccessibility
import io.github.greavemrsvg.dogwalkbuddy.ui.a11y.assertIsLargeFullWidthControl
import io.github.greavemrsvg.dogwalkbuddy.ui.a11y.assertNoTextTruncation
import io.github.greavemrsvg.dogwalkbuddy.ui.a11y.assertTouchTargetIsAtLeast
import io.github.greavemrsvg.dogwalkbuddy.ui.common.PERMISSION_ILLUSTRATION_TAG
import io.github.greavemrsvg.dogwalkbuddy.ui.nav.DogWalkBuddyNavHost
import io.github.greavemrsvg.dogwalkbuddy.ui.nav.NAV_TODAY_TAG
import io.github.greavemrsvg.dogwalkbuddy.ui.nav.NAV_WALKS_TAG
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.DogWalkBuddyTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Duration

/**
 * Start walk on Home and the recording screen through the real navigation and ViewModels. Permission dialogs are
 * answered by a fake registry, the service by a fake launcher (the service has its own tests).
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class WalkRecordingScreensTest {
    val composeRule = createComposeRule()

    val temporaryFolder = TemporaryFolder()

    /**
     * Test harness: the database closes only after the compose rule has torn down the screen and destroyed its
     * activity (which clears the Recording ViewModel). Closing it in an @After (inside the compose rule) let the
     * ViewModel's live `observePoints` query run on a closed connection pool under full-suite load.
     */
    @get:Rule
    val rules: RuleChain =
        RuleChain
            .outerRule(temporaryFolder)
            .around(
                object : ExternalResource() {
                    override fun after() {
                        test.close()
                    }
                },
            ).around(composeRule)

    private lateinit var test: TestAppContainer
    private lateinit var registry: FakePermissionRegistry

    @Before
    fun setUp() {
        test = TestAppContainer(temporaryFolder)
        test.stub.enqueueFixture()
        // By default every permission request is granted, and the notification permission is already granted.
        grantNotifications()
        registry = grantingRegistry()
    }

    private fun grantNotifications() =
        shadowOf(ApplicationProvider.getApplicationContext<DogWalkBuddyApp>()).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)

    private fun grantingRegistry() =
        FakePermissionRegistry { asked ->
            if (Manifest.permission.ACCESS_FINE_LOCATION in asked) test.fineGranted = true
            asked.toSet()
        }

    private fun showApp(fontScale: Float = 1f) {
        composeRule.setContent {
            DogWalkBuddyTheme {
                WithPermissionRegistry(registry) {
                    WithFontScale(fontScale) { DogWalkBuddyNavHost(container = test.container) }
                }
            }
        }
        awaitText("Good conditions for a walk")
    }

    private fun awaitText(text: String) {
        composeRule.waitUntil(WAIT_MS) {
            composeRule.onAllNodes(hasText(text), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun tapStart() = composeRule.onNodeWithTag(START_WALK_TAG).assertIsDisplayed().performClick()

    private fun recordingWalks() = runBlocking { test.container.walkRepository.recordingWalks() }

    @Test
    fun AC28_home_shows_a_full_width_start_walk_control_below_the_check_card_on_the_first_screen() {
        showApp()

        val start = composeRule.onNodeWithTag(START_WALK_TAG)
        start.assertIsDisplayed()
        start.assertTextEquals("Start walk")
        val attribution = composeRule.onNodeWithText("Weather data: MET Norway (CC BY 4.0)").fetchSemanticsNode()
        assertTrue(start.fetchSemanticsNode().boundsInRoot.top >= attribution.boundsInRoot.bottom)
    }

    @Test
    fun AC51_start_walk_is_at_least_72dp_high_and_full_width() {
        showApp()

        composeRule.onNodeWithTag(START_WALK_TAG).assertIsLargeFullWidthControl(composeRule)
    }

    @Test
    fun AC51_the_home_screen_with_start_walk_is_labelled_and_every_control_is_at_least_48dp() {
        showApp()

        composeRule.assertAccessibleScreen()
    }

    @Test
    fun AC52_the_home_screen_with_start_walk_at_the_largest_font_size_is_not_truncated() {
        showApp(fontScale = 2.0f)

        composeRule.assertNoTextTruncation()
        composeRule.onNodeWithTag(START_WALK_TAG).assertIsLargeFullWidthControl(composeRule)
    }

    @Test
    fun AC28_one_tap_starts_a_walk_and_shows_the_recording_screen_with_elapsed_time_and_distance() {
        showApp()

        tapStart()
        awaitText("Recording walk")

        assertEquals(1, recordingWalks().size)
        assertEquals(recordingWalks().single().id, test.launcher.started.single())
        composeRule.onNodeWithTag(ELAPSED_TAG).assertTextContains("0:00:00")
        composeRule.onNodeWithTag(DISTANCE_TAG).assertTextContains("0.0 mi")
        // Precise location was already granted, so there was no location request. Notifications were granted too.
        assertTrue(registry.requests.isEmpty())
    }

    @Test
    fun AC28_the_elapsed_time_and_distance_update_while_the_screen_is_shown() {
        showApp()
        tapStart()
        awaitText("Recording walk")

        test.clock.advance(Duration.ofMinutes(12))
        composeRule.mainClock.advanceTimeBy(2_000)
        test.container.recordingStateHolder.updateDistance(1609.344)

        composeRule.waitUntil(WAIT_MS) {
            composeRule.onAllNodes(hasText("0:12:00", substring = true)).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(DISTANCE_TAG).assertTextContains("1.0 mi")
    }

    @Test
    fun AC29_the_elapsed_time_includes_ten_minutes_with_the_screen_away() {
        showApp()
        tapStart()
        awaitText("Recording walk")

        test.clock.advance(Duration.ofMinutes(10))
        composeRule.mainClock.advanceTimeBy(2_000)

        composeRule.waitUntil(WAIT_MS) {
            composeRule.onAllNodes(hasText("0:10:00", substring = true)).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun AC51_stop_is_at_least_72dp_high_and_full_width_and_the_recording_screen_is_accessible() {
        showApp()
        tapStart()
        awaitText("Recording walk")

        composeRule.onNodeWithTag(STOP_WALK_TAG).assertIsLargeFullWidthControl(composeRule)
        composeRule.assertAccessibleScreen()
    }

    @Test
    fun AC52_the_recording_screen_at_the_largest_font_size_is_not_truncated_and_stop_is_readable() {
        showApp(fontScale = 2.0f)
        tapStart()
        awaitText("Recording walk")

        composeRule.assertNoTextTruncation()
        composeRule.onNodeWithTag(STOP_WALK_TAG).assertTextEquals("Stop and save walk")
        composeRule.onNodeWithTag(STOP_WALK_TAG).assertIsLargeFullWidthControl(composeRule)
    }

    @Test
    fun AC30_stop_asks_the_service_to_save_and_the_screen_closes_when_the_walk_has_ended() {
        showApp()
        tapStart()
        awaitText("Recording walk")

        composeRule.onNodeWithTag(STOP_WALK_TAG).performScrollTo().performClick()

        assertEquals(1, test.launcher.stopCount)
        composeRule.onNodeWithTag(STOP_WALK_TAG).assertTextEquals("Saving your walk")
        composeRule.onNodeWithTag(STOP_WALK_TAG).assertIsNotEnabled()
        // A second tap while saving does nothing.
        composeRule.onNodeWithTag(STOP_WALK_TAG).performClick()
        assertEquals(1, test.launcher.stopCount)
        // The service ends the walk and clears the session. The screen then returns to Home.
        test.container.recordingStateHolder.clear()
        awaitText("Good time to walk?")
        composeRule.onNodeWithTag(NAV_TODAY_TAG).assertIsSelected()
        composeRule.onNodeWithText("Good time to walk?").assertIsDisplayed()
        composeRule.onNodeWithTag(START_WALK_TAG).assertTextEquals("Start walk")
    }

    @Test
    fun a_walk_in_progress_is_shown_on_home_and_the_control_returns_to_the_recording_screen() {
        showApp()
        tapStart()
        awaitText("Recording walk")

        composeRule.onNodeWithContentDescription("Back").performClick()
        awaitText("Walk in progress")
        composeRule.onNodeWithTag(START_WALK_TAG).assertTextEquals("Walk in progress")
        composeRule.onNodeWithTag(START_WALK_TAG).performClick()

        awaitText("Recording walk")
        assertEquals(1, recordingWalks().size)
        assertEquals(1, test.launcher.started.size)
    }

    @Test
    fun UX_AC16_after_leaving_recording_through_another_tab_today_shows_walk_in_progress_which_opens_recording() {
        showApp()
        tapStart()
        awaitText("Elapsed time")
        composeRule.onNodeWithContentDescription("Back").performClick()
        composeRule.onNodeWithTag(NAV_WALKS_TAG).performClick()
        awaitText("This week")

        composeRule.onNodeWithTag(NAV_TODAY_TAG).performClick()

        awaitText("Walk in progress")
        composeRule.onNodeWithTag(NAV_TODAY_TAG).assertIsSelected()
        composeRule.onNodeWithTag(START_WALK_TAG).assertTextEquals("Walk in progress")
        composeRule.onNodeWithTag(START_WALK_TAG).assertIsDisplayed().performClick()
        awaitText("Elapsed time")
        assertEquals(1, recordingWalks().size)
        assertEquals(1, test.launcher.started.size)
    }

    @Test
    fun AC33_without_precise_location_no_walk_starts_and_the_explanation_and_grant_control_show() {
        test.fineGranted = false
        showApp()

        tapStart()

        awaitText("Recording a walk needs precise location.")
        composeRule.onNodeWithText("Allow precise location").assertIsDisplayed()
        assertTrue(recordingWalks().isEmpty())
        assertTrue(test.launcher.started.isEmpty())
        assertTrue(registry.requests.isEmpty())
    }

    @Test
    fun AC33_granting_precise_location_from_the_explanation_starts_the_walk() {
        test.fineGranted = false
        showApp()
        tapStart()
        awaitText("Allow precise location")

        composeRule.onNodeWithText("Allow precise location").performClick()

        awaitText("Recording walk")
        // Precise is requested together with approximate, as Android 12 and later require.
        assertEquals(
            listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
            registry.requests.single(),
        )
        assertEquals(1, recordingWalks().size)
    }

    @Test
    fun AC33_a_refusal_keeps_the_explanation_and_starts_nothing() {
        test.fineGranted = false
        registry = FakePermissionRegistry { emptySet() }
        showApp()
        tapStart()
        awaitText("Allow precise location")

        composeRule.onNodeWithText("Allow precise location").performClick()

        composeRule.waitForIdle()
        composeRule.onNodeWithText("Recording a walk needs precise location.").assertIsDisplayed()
        assertTrue(recordingWalks().isEmpty())
        // The system will not ask again here, so the control now opens Settings.
        composeRule.onNodeWithText("Open settings").assertIsDisplayed()
        composeRule.onNodeWithText("Open settings").performClick()
    }

    @Test
    fun UX_AC32_the_precise_location_sheet_explains_with_a_hidden_illustration_and_controls_of_at_least_48dp() {
        test.fineGranted = false
        showApp()

        tapStart()

        awaitText("Recording a walk needs precise location.")
        composeRule.assertHeadingsMarked("Recording a walk needs precise location.")
        composeRule.assertHiddenFromAccessibility(PERMISSION_ILLUSTRATION_TAG)
        val allow = composeRule.onNodeWithText("Allow precise location").assertIsDisplayed()
        allow.assertTouchTargetIsAtLeast(composeRule, 48.dp, 48.dp)
        assertTrue(allow.getBoundsInRoot().height >= 60.dp)
        composeRule.assertAccessibleScreen()
        assertTrue(recordingWalks().isEmpty())
        assertTrue(test.launcher.started.isEmpty())
    }

    @Test
    fun UX_AC32_after_a_permanent_refusal_the_sheet_offers_open_settings_of_at_least_48dp() {
        test.fineGranted = false
        registry = FakePermissionRegistry { emptySet() }
        showApp()
        tapStart()
        awaitText("Allow precise location")

        composeRule.onNodeWithText("Allow precise location").performClick()

        awaitText("Open settings")
        composeRule.onNodeWithText("Recording a walk needs precise location.").assertIsDisplayed()
        composeRule.onNodeWithText("Open settings").assertTouchTargetIsAtLeast(composeRule, 48.dp, 48.dp)
        composeRule.assertHiddenFromAccessibility(PERMISSION_ILLUSTRATION_TAG)
        assertTrue(recordingWalks().isEmpty())
    }

    @Test
    fun UX_AC32_closing_the_sheet_from_the_scrim_starts_no_walk_and_leaves_today_as_it_was() {
        test.fineGranted = false
        showApp()
        tapStart()
        awaitText("Allow precise location")

        composeRule.onNodeWithContentDescription("Close sheet").performSemanticsAction(SemanticsActions.OnClick)

        composeRule.waitUntil(WAIT_MS) {
            composeRule.onAllNodes(hasText("Allow precise location"), useUnmergedTree = true).fetchSemanticsNodes().isEmpty()
        }
        composeRule.onNodeWithTag(START_WALK_TAG).assertTextEquals("Start walk")
        composeRule.onNodeWithText("Good conditions for a walk", useUnmergedTree = true).assertIsDisplayed()
        assertTrue(recordingWalks().isEmpty())
        assertTrue(test.launcher.started.isEmpty())
        assertTrue(registry.requests.isEmpty())
    }

    @Test
    fun AC28_without_notification_permission_the_walk_still_starts_and_the_screen_shows_a_hint() {
        shadowOf(ApplicationProvider.getApplicationContext<DogWalkBuddyApp>())
            .denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
        registry = FakePermissionRegistry { emptySet() }
        showApp()

        tapStart()
        awaitText("Recording walk")

        assertEquals(listOf(listOf(Manifest.permission.POST_NOTIFICATIONS)), registry.requests)
        assertEquals(1, recordingWalks().size)
        composeRule.onNodeWithText("Notifications are off. Your walk is still being recorded.").assertIsDisplayed()
    }

    @Test
    fun with_notification_permission_the_recording_screen_shows_no_hint() {
        showApp()

        tapStart()
        awaitText("Recording walk")

        composeRule.onNodeWithText("Notifications are off. Your walk is still being recorded.").assertDoesNotExist()
    }

    private companion object {
        const val WAIT_MS = 10_000L
    }
}
