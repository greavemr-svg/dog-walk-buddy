package io.github.greavemrsvg.dogwalkbuddy.ui.check

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.greavemrsvg.dogwalkbuddy.R
import io.github.greavemrsvg.dogwalkbuddy.domain.check.FactorGrid
import io.github.greavemrsvg.dogwalkbuddy.domain.rating.HeatSensitivity
import io.github.greavemrsvg.dogwalkbuddy.domain.rating.HourRating
import io.github.greavemrsvg.dogwalkbuddy.domain.rating.HourlyConditions
import io.github.greavemrsvg.dogwalkbuddy.domain.rating.PavementFactor
import io.github.greavemrsvg.dogwalkbuddy.domain.rating.Rating
import io.github.greavemrsvg.dogwalkbuddy.domain.rating.Reason
import io.github.greavemrsvg.dogwalkbuddy.domain.window.BestWindow
import io.github.greavemrsvg.dogwalkbuddy.ui.a11y.WithFontScale
import io.github.greavemrsvg.dogwalkbuddy.ui.a11y.assertAccessibleScreen
import io.github.greavemrsvg.dogwalkbuddy.ui.a11y.assertAnnouncesRatingThenReason
import io.github.greavemrsvg.dogwalkbuddy.ui.a11y.assertNoTextTruncation
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.DogWalkBuddyTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Instant
import java.time.ZoneOffset

/** The check card on its own, driven by fixed states (design, Test strategy: Compose screen tests). */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
// A typical phone window at default size, so "without scrolling" means what a user sees.
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class CheckCardTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val formatter = BestWindowFormatter(context.resources, ZoneOffset.UTC, { true })
    private var state by mutableStateOf<CheckUiState>(CheckUiState.Loading)
    private var refreshes = 0
    private var grants = 0
    private var settingsOpens = 0
    private var aboutOpens = 0
    private var profileOpens = 0

    private fun show(
        initial: CheckUiState,
        fontScale: Float = 1f,
    ) {
        state = initial
        composeRule.setContent {
            DogWalkBuddyTheme {
                WithFontScale(fontScale) {
                    // Scrollable, as on the Home screen, so tall content at a large font is not squeezed by the window.
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        CheckCard(
                            state = state,
                            windowFormatter = formatter,
                            onRefresh = { refreshes++ },
                            onGrantPermission = { grants++ },
                            onOpenAbout = { aboutOpens++ },
                            onOpenProfile = { profileOpens++ },
                            onOpenSettings = { settingsOpens++ },
                        )
                    }
                }
            }
        }
    }

    private fun ratedState(
        rating: Rating = Rating.GO,
        reason: Reason = Reason.GOOD,
        window: BestWindow = nowUntilNoon(rating),
        ageMinutes: Long? = null,
        usingDefaultSensitivity: Boolean = false,
    ) = CheckUiState.Rated(
        current = HourRating(HOUR, rating, reason),
        window = window,
        ageMinutes = ageMinutes,
        usingDefaultSensitivity = usingDefaultSensitivity,
        hours = listOf(HourRating(HOUR, rating, reason)),
        factors = FactorGrid.cells(CONDITIONS, PavementFactor.NotRated, reason),
        isCached = false,
        isOffline = false,
        dogName = null,
        sensitivity = HeatSensitivity.NORMAL,
    )

    private fun nowUntilNoon(rating: Rating) = BestWindow.Window(HOUR, NOON, rating, startsNow = true)

    @Test
    fun AC20_a_successful_check_says_pollen_is_not_included() {
        show(ratedState())

        composeRule.onNodeWithText("Pollen isn't included in this check").assertIsDisplayed()
    }

    @Test
    fun AC26_the_disclaimer_and_the_about_link_are_visible_without_scrolling_at_default_font_size() {
        show(ratedState())

        composeRule
            .onNodeWithText("Guidance only, not veterinary advice. If you're worried about your dog, contact a vet.")
            .assertIsDisplayed()
        composeRule.onNodeWithText("About this advice").assertIsDisplayed().performClick()
        assertEquals(1, aboutOpens)
    }

    @Test
    fun AC26_the_disclaimer_is_also_shown_with_a_cached_result_and_with_no_result() {
        val disclaimer = "Guidance only, not veterinary advice. If you're worried about your dog, contact a vet."

        show(ratedState(ageMinutes = 179))
        composeRule.onNodeWithText(disclaimer).assertIsDisplayed()

        state = CheckUiState.CantCheck
        composeRule.onNodeWithText(disclaimer).assertIsDisplayed()
    }

    @Test
    fun AC27_the_attribution_is_shown_for_every_state() {
        val attribution = "Weather data: MET Norway (CC BY 4.0)"
        show(ratedState())
        composeRule.onNodeWithText(attribution).assertIsDisplayed()

        listOf(CheckUiState.Loading, CheckUiState.NeedsLocationPermission, CheckUiState.CantCheck).forEach {
            state = it
            composeRule.onNodeWithText(attribution).assertIsDisplayed()
        }
    }

    @Test
    fun AC21_a_stored_result_is_labelled_with_its_age_in_hours_and_minutes() {
        show(ratedState(Rating.CAUTION, Reason.WINDY, ageMinutes = 179))

        composeRule.onNodeWithText("Checked 2 h 59 min ago").assertIsDisplayed()
        composeRule.onNodeWithText("Windy", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun AC21_an_age_under_an_hour_is_shown_in_minutes_and_a_fresh_result_has_no_label() {
        show(ratedState(ageMinutes = 12))
        composeRule.onNodeWithText("Checked 12 min ago").assertIsDisplayed()

        state = ratedState(ageMinutes = null)
        composeRule.onNodeWithText("Checked 12 min ago").assertDoesNotExist()
    }

    @Test
    fun AC15_cant_check_shows_a_retry_and_no_rating() {
        show(CheckUiState.CantCheck)

        composeRule.onNodeWithText("Can't check right now").assertIsDisplayed()
        composeRule.onNodeWithText("Try again").assertIsDisplayed().performClick()
        assertEquals(1, refreshes)
        Rating.entries.forEach { composeRule.onNodeWithText(context.getString(it.labelRes()), useUnmergedTree = true).assertDoesNotExist() }
        composeRule.onNodeWithTag(RATING_BLOCK_TAG, useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun AC22_cant_check_has_no_best_window_and_no_pollen_note() {
        show(CheckUiState.CantCheck)

        composeRule.onNodeWithText("Best window, next 12 hours").assertDoesNotExist()
        composeRule.onNodeWithText("Pollen isn't included in this check").assertDoesNotExist()
    }

    @Test
    fun AC25_without_permission_the_card_explains_and_offers_to_grant_it() {
        show(CheckUiState.NeedsLocationPermission)

        composeRule.onNodeWithText("The check needs your approximate location").assertIsDisplayed()
        composeRule.onNodeWithText("Allow approximate location").assertIsDisplayed().performClick()
        assertEquals(1, grants)
        composeRule.onNodeWithTag(RATING_BLOCK_TAG, useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun AC25_after_a_permanent_refusal_the_card_offers_open_settings_instead_of_the_dialog() {
        show(CheckUiState.LocationPermissionBlocked)

        composeRule.onNodeWithText("The check needs your approximate location").assertIsDisplayed()
        composeRule.onNodeWithText("Allow approximate location").assertDoesNotExist()
        composeRule.onNodeWithText("Open settings").assertIsDisplayed().performClick()
        assertEquals(1, settingsOpens)
        assertEquals(0, grants)
        composeRule.onNodeWithTag(RATING_BLOCK_TAG, useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun AC21_a_stored_result_under_a_minute_old_reads_0_min_ago() {
        show(ratedState(ageMinutes = 0))

        composeRule.onNodeWithText("Checked 0 min ago").assertIsDisplayed()
    }

    @Test
    fun AC45_with_no_profile_the_card_says_normal_sensitivity_is_used_and_links_to_the_profile() {
        show(ratedState(usingDefaultSensitivity = true))

        composeRule
            .onNodeWithText("Using normal heat sensitivity. Set up your dog's profile to change this.")
            .assertIsDisplayed()
            .performClick()
        assertEquals(1, profileOpens)
    }

    @Test
    fun AC45_with_a_profile_the_note_is_not_shown() {
        show(ratedState(usingDefaultSensitivity = false))

        composeRule
            .onNodeWithText("Using normal heat sensitivity. Set up your dog's profile to change this.")
            .assertDoesNotExist()
    }

    @Test
    fun AC53_each_rating_differs_by_label_and_by_icon_as_well_as_colour() {
        show(ratedState(Rating.GO, Reason.GOOD))
        val icons = mutableSetOf<Int>()
        val labels = mutableSetOf<String>()

        Rating.entries.forEach { rating ->
            state = ratedState(rating, Reason.GOOD)
            val label = context.getString(rating.labelRes())
            composeRule.onNodeWithText(label, useUnmergedTree = true).assertIsDisplayed()
            composeRule.onNodeWithTag(RATING_ICON_TAG_PREFIX + rating.iconRes(), useUnmergedTree = true).assertIsDisplayed()
            icons += rating.iconRes()
            labels += label
        }

        assertEquals(setOf(R.drawable.ic_rating_go, R.drawable.ic_rating_caution, R.drawable.ic_rating_avoid), icons)
        assertEquals(setOf("Go", "Caution", "Avoid"), labels)
    }

    @Test
    fun AC50_the_result_is_announced_as_rating_then_reason_in_a_polite_live_region() {
        show(ratedState(Rating.CAUTION, Reason.WINDY))

        val block = composeRule.onNodeWithTag(RATING_BLOCK_TAG, useUnmergedTree = true)
        block.assertAnnouncesRatingThenReason("Caution", "Windy", FACTORS_SPOKEN)
        block.assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
    }

    @Test
    fun AC50_every_reason_is_announced_with_its_exact_text() {
        show(ratedState())

        Reason.entries.forEach { reason ->
            state = ratedState(Rating.AVOID, reason)
            composeRule
                .onNodeWithTag(RATING_BLOCK_TAG, useUnmergedTree = true)
                .assertAnnouncesRatingThenReason("Avoid", context.getString(reason.textRes()), FACTORS_SPOKEN)
        }
    }

    @Test
    fun AC50_the_best_window_is_text_under_a_heading() {
        show(ratedState(window = BestWindow.Window(HOUR.plusSeconds(HOUR_SECONDS), NOON, Rating.CAUTION, startsNow = false)))

        composeRule.onNodeWithText("Best window, next 12 hours").assertIsDisplayed()
        composeRule.onNodeWithText("Best available: Caution").assertIsDisplayed()
        composeRule.onNodeWithText("11:00-12:00").assertIsDisplayed()
    }

    @Test
    fun AC17_a_window_starting_now_reads_now_until_the_end() {
        show(ratedState(window = BestWindow.Window(HOUR, NOON, Rating.GO, startsNow = true)))

        composeRule.onNodeWithText("Now until 12:00").assertIsDisplayed()
    }

    @Test
    fun AC19_no_good_time_is_shown_as_text() {
        show(ratedState(window = BestWindow.NoGoodTime))

        composeRule.onNodeWithText("No good time in the next 12 hours").assertIsDisplayed()
    }

    @Test
    fun AC50_loading_and_permission_messages_are_polite_live_regions() {
        show(CheckUiState.Loading)
        composeRule
            .onNodeWithText("Checking the weather")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))

        state = CheckUiState.CantCheck
        composeRule
            .onNodeWithText("Can't check right now")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
    }

    @Test
    fun AC51_every_control_in_every_state_is_labelled_and_at_least_48dp() {
        show(ratedState(usingDefaultSensitivity = true))
        composeRule.assertAccessibleScreen()

        listOf(CheckUiState.NeedsLocationPermission, CheckUiState.CantCheck).forEach {
            state = it
            composeRule.assertAccessibleScreen()
        }
    }

    @Test
    fun AC52_at_the_largest_font_size_no_text_is_truncated() {
        show(
            ratedState(Rating.AVOID, Reason.PAVEMENT_WARM, ageMinutes = 179, usingDefaultSensitivity = true),
            fontScale = 2.0f,
        )

        composeRule.assertNoTextTruncation()
        composeRule.onNodeWithText("Avoid", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithText(context.getString(Reason.PAVEMENT_WARM.textRes()), useUnmergedTree = true).assertExists()
        composeRule.onNodeWithText("Now until 12:00").assertExists()
    }

    @Test
    fun AC52_the_other_states_are_not_truncated_at_the_largest_font_size() {
        show(CheckUiState.CantCheck, fontScale = 2.0f)
        composeRule.assertNoTextTruncation()

        state = CheckUiState.NeedsLocationPermission
        composeRule.assertNoTextTruncation()

        state = CheckUiState.LocationPermissionBlocked
        composeRule.assertNoTextTruncation()
    }

    @Test
    fun AC53_the_rating_labels_for_go_caution_and_avoid_are_all_different() {
        assertNotEquals(Rating.GO.labelRes(), Rating.CAUTION.labelRes())
        assertNotEquals(Rating.CAUTION.labelRes(), Rating.AVOID.labelRes())
        assertNotEquals(Rating.GO.iconRes(), Rating.AVOID.iconRes())
    }

    private companion object {
        val HOUR: Instant = Instant.parse("2026-10-19T10:00:00Z")
        val NOON: Instant = Instant.parse("2026-10-19T12:00:00Z")
        const val HOUR_SECONDS = 3600L

        // The current hour's values (AC19 example): T = 15.0 °C, P = 0.0 mm/h, M = 26.3 mph, no cloud data.
        val CONDITIONS = HourlyConditions(HOUR, 15.0, 0.0, 26.3, null)
        const val FACTORS_SPOKEN = "Air 15°C, Rain 0 mm/h, Wind 26 mph, Pavement Not rated"
    }
}
