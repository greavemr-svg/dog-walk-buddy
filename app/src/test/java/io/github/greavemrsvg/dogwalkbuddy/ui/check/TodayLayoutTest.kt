package io.github.greavemrsvg.dogwalkbuddy.ui.check

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.greavemrsvg.dogwalkbuddy.domain.check.Factor
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
import io.github.greavemrsvg.dogwalkbuddy.ui.a11y.assertHeadingsMarked
import io.github.greavemrsvg.dogwalkbuddy.ui.a11y.assertIsLargeFullWidthControl
import io.github.greavemrsvg.dogwalkbuddy.ui.a11y.assertNoTextTruncation
import io.github.greavemrsvg.dogwalkbuddy.ui.a11y.assertTouchTargetIsAtLeast
import io.github.greavemrsvg.dogwalkbuddy.ui.nav.FieldKitNavBar
import io.github.greavemrsvg.dogwalkbuddy.ui.nav.NAV_TODAY_TAG
import io.github.greavemrsvg.dogwalkbuddy.ui.nav.Routes
import io.github.greavemrsvg.dogwalkbuddy.ui.recording.START_WALK_TAG
import io.github.greavemrsvg.dogwalkbuddy.ui.recording.StartWalkContent
import io.github.greavemrsvg.dogwalkbuddy.ui.recording.StartWalkUiState
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.DarkFieldKitColors
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.DogWalkBuddyTheme
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.FieldKitColors
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.LightFieldKitColors
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Instant
import java.time.ZoneOffset

/**
 * Today as the shell shows it: the scrolling check content, the pinned Start walk and the bottom navigation bar, at
 * a typical phone size (AC20, AC21, AC24-AC28, AC58).
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class TodayLayoutTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val formatter = BestWindowFormatter(context.resources, ZoneOffset.UTC, { true })
    private var state by mutableStateOf<CheckUiState>(CheckUiState.Loading)
    private var refreshes = 0
    private var profileOpens = 0
    private var aboutOpens = 0
    private var starts = 0

    private fun show(
        initial: CheckUiState,
        fontScale: Float = 1f,
        night: Boolean = false,
    ) {
        if (night) RuntimeEnvironment.setQualifiers("+night")
        state = initial
        composeRule.setContent {
            DogWalkBuddyTheme {
                WithFontScale(fontScale) {
                    Column(Modifier.fillMaxSize()) {
                        TodayScreen(
                            checkContent = {
                                CheckCard(
                                    state = state,
                                    windowFormatter = formatter,
                                    onRefresh = { refreshes++ },
                                    onGrantPermission = {},
                                    onOpenAbout = { aboutOpens++ },
                                    onOpenProfile = { profileOpens++ },
                                )
                            },
                            startWalkContent = { StartWalkContent(StartWalkUiState.Idle, { starts++ }, {}, {}) },
                            modifier = Modifier.weight(1f),
                        )
                        FieldKitNavBar(selectedRoute = Routes.HOME, dogName = (state as? CheckUiState.Rated)?.dogName, onSelect = {})
                    }
                }
            }
        }
    }

    private fun rated(
        rating: Rating = Rating.GO,
        reason: Reason = Reason.GOOD,
        conditions: HourlyConditions = HourlyConditions(HOUR, 15.0, 0.0, 9.0, 100.0),
        pavement: PavementFactor = PavementFactor.NotRated,
        ageMinutes: Long? = null,
        isCached: Boolean = false,
        isOffline: Boolean = false,
        dogName: String? = "Biscuit",
        sensitivity: HeatSensitivity = HeatSensitivity.NORMAL,
    ) = CheckUiState.Rated(
        current = HourRating(HOUR, rating, reason),
        window = BestWindow.Window(HOUR, HOUR.plusSeconds(3 * 3600), rating, startsNow = true),
        ageMinutes = ageMinutes,
        usingDefaultSensitivity = dogName == null,
        hours = listOf(HourRating(HOUR, rating, reason)),
        factors = FactorGrid.cells(conditions, pavement, reason),
        isCached = isCached,
        isOffline = isOffline,
        dogName = dogName,
        sensitivity = sensitivity,
    )

    /** The worst case of AC25: Caution, offline banner, age label, Sensitive line, long pavement reason. */
    private fun worstCase() =
        rated(
            rating = Rating.CAUTION,
            reason = Reason.PAVEMENT_WARM,
            conditions = HourlyConditions(HOUR, 17.0, 0.0, 9.0, 20.0),
            pavement = PavementFactor.Rated(Rating.CAUTION),
            ageMinutes = 179,
            isCached = true,
            isOffline = true,
            dogName = "Biscuit",
            sensitivity = HeatSensitivity.SENSITIVE,
        )

    private fun SemanticsNodeInteraction.bottom(): Float = fetchSemanticsNode().let { it.positionInRoot.y + it.size.height }

    private fun SemanticsNodeInteraction.top(): Float = fetchSemanticsNode().positionInRoot.y

    /** The node lies wholly inside the scroll viewport, so it is fully on screen without scrolling. */
    private fun assertFullyInViewport(node: SemanticsNodeInteraction) {
        val viewport = composeRule.onNodeWithTag(TODAY_SCROLL_TAG).fetchSemanticsNode()
        node.assertIsDisplayed()
        val viewportBottom = viewport.positionInRoot.y + viewport.size.height
        check(node.top() >= viewport.positionInRoot.y && node.bottom() <= viewportBottom) {
            "Node ends at ${node.bottom()}, below the visible content, which ends at $viewportBottom"
        }
    }

    private fun aboutLink() = composeRule.onNode(hasText("About this advice") and hasClickAction())

    @Test
    fun UX_AC20_the_header_shows_the_named_title_just_now_meta_and_a_refresh_button() {
        show(rated())

        composeRule.onNodeWithText("Good time to walk Biscuit?").assertIsDisplayed()
        composeRule.onNodeWithText("Your area · checked just now").assertIsDisplayed()
        composeRule.assertHeadingsMarked("Good time to walk Biscuit?")
        val refresh = composeRule.onNodeWithContentDescription("Refresh")
        refresh.assertIsDisplayed().assertTouchTargetIsAtLeast(composeRule, 48.dp, 48.dp)
        refresh.performClick()
        assertEquals(1, refreshes)
    }

    @Test
    fun UX_AC20_an_older_result_shows_its_exact_age_and_no_profile_shows_the_plain_title() {
        show(rated(ageMinutes = 12, dogName = null))

        composeRule.onNodeWithText("Checked 12 min ago").assertIsDisplayed()
        composeRule.onNodeWithText("Your area · checked just now").assertDoesNotExist()
        composeRule.onNodeWithText("Good time to walk?").assertIsDisplayed()

        state = rated(ageMinutes = 179, dogName = null)
        composeRule.onNodeWithText("Checked 2 h 59 min ago").assertIsDisplayed()
    }

    @Test
    fun UX_AC20_states_without_a_result_show_the_title_only() {
        show(CheckUiState.Loading)
        listOf(CheckUiState.Loading, CheckUiState.CantCheck, CheckUiState.NeedsLocationPermission).forEach {
            state = it
            composeRule.onNodeWithText("Good time to walk?").assertIsDisplayed()
            composeRule.onNodeWithContentDescription("Refresh").assertDoesNotExist()
            composeRule.onNodeWithText("Your area · checked just now").assertDoesNotExist()
        }
    }

    @Test
    fun UX_AC21_a_cached_result_offline_shows_the_bold_offline_lead_then_the_cached_line() {
        show(rated(ageMinutes = 30, isCached = true, isOffline = true))

        composeRule
            .onNodeWithText("You're offline. Showing your last check. It's kept for up to 3 hours.")
            .assertIsDisplayed()
        val banner = composeRule.onNodeWithText("You're offline. Showing your last check. It's kept for up to 3 hours.")
        val text = banner.fetchSemanticsNode().config[SemanticsProperties.Text].first()
        val bold = text.spanStyles.single()
        assertEquals("You're offline.", text.text.substring(bold.start, bold.end))
        assertEquals(androidx.compose.ui.text.font.FontWeight.Bold, bold.item.fontWeight)
        // The banner sits above the hero card.
        check(banner.bottom() <= composeRule.onNodeWithTag(RATING_BLOCK_TAG).top())
    }

    @Test
    fun UX_AC21_a_cached_result_online_shows_only_the_cached_line_and_a_fresh_result_no_banner() {
        show(rated(ageMinutes = 30, isCached = true, isOffline = false))
        composeRule.onNodeWithText("Showing your last check. It's kept for up to 3 hours.").assertIsDisplayed()
        composeRule.onNodeWithText("You're offline.", substring = true).assertDoesNotExist()

        // A fresh in-session result, even one old enough to show its age, has no banner.
        state = rated(ageMinutes = 30, isCached = false)
        composeRule.onNodeWithText("Showing your last check.", substring = true).assertDoesNotExist()
    }

    @Test
    fun UX_AC24_the_footnote_has_the_disclaimer_about_link_pollen_and_attribution_as_separate_texts() {
        show(rated())

        composeRule
            .onNodeWithText("Guidance only, not veterinary advice. If you're worried about your dog, contact a vet.")
            .assertIsDisplayed()
        aboutLink().assertIsDisplayed().assertTouchTargetIsAtLeast(composeRule, 48.dp, 48.dp).performClick()
        assertEquals(1, aboutOpens)
        composeRule.onNodeWithText("Pollen isn't included in this check").assertIsDisplayed()
        composeRule.onNodeWithText("Weather data: MET Norway (CC BY 4.0)").assertIsDisplayed()
        composeRule.onNodeWithText("Heat-sensitive thresholds", substring = true).assertDoesNotExist()
        // The footnote comes before the best-window card (K1).
        check(
            composeRule.onNodeWithText("Weather data: MET Norway (CC BY 4.0)").bottom() <=
                composeRule.onNodeWithText("Now until 13:00").top(),
        )
    }

    @Test
    fun UX_AC24_no_profile_links_to_the_dog_tab_and_a_sensitive_dog_gets_its_own_line() {
        show(rated(dogName = null))
        composeRule
            .onNodeWithText("Using normal heat sensitivity. Set up your dog's profile to change this.")
            .assertTouchTargetIsAtLeast(composeRule, 48.dp, 48.dp)
            .performClick()
        assertEquals(1, profileOpens)

        state = rated(dogName = "Biscuit", sensitivity = HeatSensitivity.SENSITIVE)
        composeRule.onNodeWithText("Heat-sensitive thresholds (Biscuit)").assertIsDisplayed()
        composeRule.onNodeWithText("Pollen isn't included in this check").assertIsDisplayed()
        composeRule.onNodeWithText("Using normal heat sensitivity.", substring = true).assertDoesNotExist()
    }

    @Test
    fun UX_AC25_the_disclaimer_and_about_link_are_fully_visible_without_scrolling_in_the_four_cases() {
        val disclaimer = "Guidance only, not veterinary advice. If you're worried about your dog, contact a vet."
        show(rated())
        listOf(
            rated(),
            worstCase(),
            rated(rating = Rating.AVOID, reason = Reason.DANGEROUS_WINDS, conditions = HourlyConditions(HOUR, 12.0, 0.0, 45.0, 100.0)),
            rated(dogName = null),
        ).forEach {
            state = it
            composeRule.waitForIdle()
            assertFullyInViewport(composeRule.onNodeWithText(disclaimer))
            assertFullyInViewport(aboutLink())
        }
        // The worst case really has every part AC25 names.
        state = worstCase()
        composeRule.onNodeWithText("Checked 2 h 59 min ago").assertExists()
        composeRule.onNodeWithText("Heat-sensitive thresholds (Biscuit)").assertExists()
        composeRule.onNodeWithTag(Factor.PAVEMENT.testTag(), useUnmergedTree = true).assertExists()
    }

    @Test
    fun UX_AC26_start_walk_is_full_width_at_least_72dp_primary_and_directly_above_the_nav() {
        show(rated())

        val start = composeRule.onNodeWithTag(START_WALK_TAG)
        start.assertIsDisplayed().assertIsLargeFullWidthControl(composeRule)
        composeRule.onNodeWithText("Start walk").assertExists()
        val gap = with(composeRule.density) { (composeRule.onNodeWithTag(NAV_TODAY_TAG).top() - start.bottom()).toDp() }
        check(gap >= 0.dp && gap <= 24.dp) { "Start walk is $gap above the navigation bar" }
        val image = start.captureToImage().toPixelMap()
        assertEquals(LightFieldKitColors.primary, image[image.width / 2, 3])

        start.performClick()
        assertEquals(1, starts)
    }

    @Test
    fun UX_AC27_at_200_percent_nothing_is_truncated_and_start_and_the_nav_stay_on_screen() {
        show(rated(), fontScale = 2.0f)
        listOf(rated(), worstCase(), rated(rating = Rating.AVOID, reason = Reason.HEAVY_RAIN)).forEach {
            state = it
            composeRule.assertNoTextTruncation()
            composeRule.onNodeWithText("Now until 13:00").assertExists()
            val rootBottom = composeRule.onRoot().bottom()
            val start = composeRule.onNodeWithTag(START_WALK_TAG)
            start.assertIsDisplayed()
            check(start.bottom() <= rootBottom && start.bottom() <= composeRule.onNodeWithTag(NAV_TODAY_TAG).top())
            composeRule.onNodeWithTag(NAV_TODAY_TAG).assertIsDisplayed()
            check(composeRule.onNodeWithTag(NAV_TODAY_TAG).bottom() <= rootBottom)
        }
    }

    @Test
    fun UX_AC28_loading_shows_a_muted_placeholder_with_checking_the_weather_and_no_result() {
        show(CheckUiState.Loading)

        composeRule
            .onNodeWithText("Checking the weather")
            .assertIsDisplayed()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
        composeRule.onNodeWithTag(RATING_BLOCK_TAG, useUnmergedTree = true).assertDoesNotExist()
        Rating.entries.forEach { composeRule.onNodeWithText(context.getString(it.labelRes()), useUnmergedTree = true).assertDoesNotExist() }
        Reason.entries.forEach { composeRule.onNodeWithText(context.getString(it.textRes()), useUnmergedTree = true).assertDoesNotExist() }
        Factor.entries.forEach { composeRule.onNodeWithTag(it.testTag(), useUnmergedTree = true).assertDoesNotExist() }
        val image = composeRule.onNodeWithTag(LOADING_TAG, useUnmergedTree = true).fetchSemanticsNode().parentImage()
        assertEquals(LightFieldKitColors.surfaceMuted, image)
    }

    private fun SemanticsNode.parentImage() =
        composeRule
            .onRoot()
            .captureToImage()
            .toPixelMap()
            .let { it[(positionInRoot.x - 8).toInt().coerceAtLeast(0), positionInRoot.y.toInt()] }

    @Test
    fun UX_AC58_every_today_state_is_accessible_in_light() {
        assertAccessibleInEveryState(LightFieldKitColors, night = false)
    }

    @Test
    fun UX_AC58_every_today_state_is_accessible_in_dark() {
        assertAccessibleInEveryState(DarkFieldKitColors, night = true)
    }

    private fun assertAccessibleInEveryState(
        colors: FieldKitColors,
        night: Boolean,
    ) {
        show(worstCase(), night = night)
        composeRule.assertAccessibleScreen()
        composeRule.assertHeadingsMarked("Good time to walk Biscuit?")
        composeRule.onNodeWithTag(RATING_BLOCK_TAG).assertContentDescriptionEquals(
            "Caution. Pavement may be hot: test it with the back of your hand. Air 17°C, Rain 0 mm/h, Wind 9 mph, Pavement Warm",
        )
        val start = composeRule.onNodeWithTag(START_WALK_TAG).captureToImage().toPixelMap()
        assertEquals(colors.primary, start[start.width / 2, 3])
        listOf(rated(dogName = null), CheckUiState.Loading, CheckUiState.CantCheck, CheckUiState.NeedsLocationPermission).forEach {
            state = it
            composeRule.assertAccessibleScreen()
            composeRule.assertHeadingsMarked("Good time to walk?")
        }
    }

    private companion object {
        val HOUR: Instant = Instant.parse("2026-10-19T10:00:00Z")
    }
}
