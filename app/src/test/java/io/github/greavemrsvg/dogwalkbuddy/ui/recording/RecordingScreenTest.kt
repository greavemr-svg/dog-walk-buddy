package io.github.greavemrsvg.dogwalkbuddy.ui.recording

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.width
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.greavemrsvg.dogwalkbuddy.domain.location.LatLon
import io.github.greavemrsvg.dogwalkbuddy.domain.location.LocationFix
import io.github.greavemrsvg.dogwalkbuddy.domain.walk.RouteProjection
import io.github.greavemrsvg.dogwalkbuddy.service.RecordingSession
import io.github.greavemrsvg.dogwalkbuddy.testutil.TestAppContainer
import io.github.greavemrsvg.dogwalkbuddy.ui.a11y.WithFontScale
import io.github.greavemrsvg.dogwalkbuddy.ui.a11y.assertAccessibleScreen
import io.github.greavemrsvg.dogwalkbuddy.ui.a11y.assertHeadingsMarked
import io.github.greavemrsvg.dogwalkbuddy.ui.a11y.assertHiddenFromAccessibility
import io.github.greavemrsvg.dogwalkbuddy.ui.a11y.assertIsLargeFullWidthControl
import io.github.greavemrsvg.dogwalkbuddy.ui.a11y.assertNoTextTruncation
import io.github.greavemrsvg.dogwalkbuddy.ui.a11y.assertTouchTargetIsAtLeast
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.DogWalkBuddyTheme
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.RecordingPalette
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset

/** The restyled Recording screen (AC4, AC33-AC38, AC58), hosted alone and, for the route line, with Room. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class RecordingScreenTest {
    val composeRule = createComposeRule()

    val temporaryFolder = TemporaryFolder()

    /** Owns the route's ViewModel, so its Room collector can be stopped before the database closes. */
    private val viewModelStore = ViewModelStore()

    /**
     * Test harness: the screen and its ViewModel are torn down before the database closes. Closing the database in
     * an @After (inside the compose rule) let the ViewModel's live `observePoints` query run on a closed connection
     * pool under full-suite load ("connection pool has been closed").
     */
    @get:Rule
    val rules: RuleChain =
        RuleChain
            .outerRule(temporaryFolder)
            .around(
                object : ExternalResource() {
                    override fun after() {
                        viewModelStore.clear()
                        test?.close()
                    }
                },
            ).around(composeRule)

    private var test: TestAppContainer? = null

    private var stops = 0

    private val start = Instant.parse("2026-10-19T19:05:00Z")

    private fun showContent(
        elapsed: Duration = Duration.ofMinutes(23).plusSeconds(41),
        distanceM: Double = 2000.0,
        is24Hour: Boolean = true,
        stopping: Boolean = false,
        notificationsOff: Boolean = false,
        fontScale: Float = 1f,
        route: List<LatLon> = emptyList(),
    ) {
        composeRule.setContent {
            DogWalkBuddyTheme {
                WithFontScale(fontScale) {
                    RecordingContent(
                        session = RecordingSession(walkId = 1, startUtc = start, distanceM = distanceM),
                        now = start.plus(elapsed),
                        route = route,
                        startedFormat = StartedTimeFormat(ZoneOffset.UTC, is24Hour),
                        stopping = stopping,
                        showNotificationsHint = notificationsOff,
                        onStop = { stops++ },
                        onBack = {},
                    )
                }
            }
        }
    }

    private fun pixel(
        node: SemanticsNodeInteraction,
        x: Int,
        y: Int,
    ): Color = node.captureToImage().toPixelMap()[x, y]

    private fun assertRecordingPaletteIsDrawn() {
        val root = composeRule.onRoot()
        val rootSize = root.fetchSemanticsNode().size
        assertEquals("background", RecordingPalette.background, pixel(root, rootSize.width - 2, rootSize.height - 2))
        val distance = composeRule.onNodeWithTag(DISTANCE_TAG)
        assertEquals("stat card", RecordingPalette.card, pixel(distance, 4, distance.fetchSemanticsNode().size.height / 2))
        val dot = composeRule.onNodeWithTag(LIVE_DOT_TAG, useUnmergedTree = true)
        val dotSize = dot.fetchSemanticsNode().size
        assertEquals("live dot", RecordingPalette.liveDot, pixel(dot, dotSize.width / 2, dotSize.height / 2))
        val stop = composeRule.onNodeWithTag(STOP_WALK_TAG).performScrollTo()
        val stopHeight = stop.fetchSemanticsNode().size.height
        val inset = with(composeRule.density) { 40.dp.roundToPx() }
        assertEquals("Stop", RecordingPalette.stopBackground, pixel(stop, inset, stopHeight / 2))
    }

    @Test
    fun UX_AC4_recording_draws_the_recording_palette_when_the_system_is_light() {
        showContent()

        assertRecordingPaletteIsDrawn()
    }

    @Test
    @Config(qualifiers = "+night")
    fun UX_AC4_recording_draws_the_same_recording_palette_when_the_system_is_dark() {
        showContent()

        assertRecordingPaletteIsDrawn()
    }

    @Test
    fun UX_AC33_recording_has_a_back_control_and_a_status_pill_with_a_live_dot_and_recording_walk() {
        showContent()

        composeRule
            .onNodeWithContentDescription("Back")
            .assertIsDisplayed()
            .assertTouchTargetIsAtLeast(composeRule, 48.dp, 48.dp)
        composeRule.onNodeWithText("Recording walk").assertIsDisplayed()
        composeRule.onNodeWithTag(LIVE_DOT_TAG, useUnmergedTree = true).assertIsDisplayed()
        val dot = composeRule.onNodeWithTag(LIVE_DOT_TAG, useUnmergedTree = true).getBoundsInRoot()
        val title = composeRule.onNodeWithText("Recording walk").getBoundsInRoot()
        assertTrue("the live dot is before the title", dot.right <= title.left)
        assertEquals(8.dp, dot.width)
    }

    @Test
    fun UX_AC34_the_timer_reads_h_mm_ss_under_elapsed_time_and_is_spoken_in_words() {
        showContent(elapsed = Duration.ofMinutes(23).plusSeconds(41))

        composeRule.onNodeWithTag(ELAPSED_TAG).assertTextEquals("Elapsed time", "0:23:41")
        composeRule
            .onNodeWithTag(TIMER_TAG, useUnmergedTree = true)
            .assertTextEquals("0:23:41")
            .assertContentDescriptionEquals("23 minutes 41 seconds")
    }

    @Test
    fun UX_AC34_an_hour_timer_is_spoken_without_zero_units() {
        showContent(elapsed = Duration.ofHours(1).plusSeconds(5))

        composeRule
            .onNodeWithTag(TIMER_TAG, useUnmergedTree = true)
            .assertTextEquals("1:00:05")
            .assertContentDescriptionEquals("1 hour 5 seconds")
    }

    @Test
    fun UX_AC34_the_distance_card_shows_miles_and_the_started_card_shows_the_start_time_in_24_hour_format() {
        showContent(distanceM = 2000.0, is24Hour = true)

        composeRule.onNodeWithTag(DISTANCE_TAG).assertTextEquals("Distance", "1.2 mi")
        composeRule.onNodeWithTag(STARTED_TAG).assertTextEquals("Started", "19:05")
    }

    @Test
    fun UX_AC34_the_started_card_follows_the_12_hour_device_format() {
        showContent(is24Hour = false)

        composeRule.onNodeWithTag(STARTED_TAG).assertTextEquals("Started", "7:05 pm")
    }

    @Test
    fun UX_AC36_with_notifications_on_the_tip_shows() {
        showContent(notificationsOff = false)

        composeRule
            .onNodeWithText(
                "Pop your phone in your pocket. Recording carries on with the screen off, and a notification shows while it's on.",
            ).assertIsDisplayed()
        composeRule.onNodeWithText("Notifications are off. Your walk is still being recorded.").assertDoesNotExist()
    }

    @Test
    fun UX_AC36_with_notifications_off_the_exact_notice_replaces_the_tip() {
        showContent(notificationsOff = true)

        composeRule.onNodeWithText("Notifications are off. Your walk is still being recorded.").performScrollTo().assertIsDisplayed()
        composeRule
            .onNodeWithText(
                "Pop your phone in your pocket. Recording carries on with the screen off, and a notification shows while it's on.",
            ).assertDoesNotExist()
    }

    @Test
    fun UX_AC37_stop_is_full_width_76dp_high_with_a_stop_icon_and_the_approved_label() {
        showContent()

        val stop = composeRule.onNodeWithTag(STOP_WALK_TAG).performScrollTo()
        stop.assertTextEquals("Stop and save walk").assertIsEnabled().assertHeightIsAtLeast(76.dp)
        stop.assertIsLargeFullWidthControl(composeRule)
        composeRule.onNodeWithTag(STOP_ICON_TAG, useUnmergedTree = true).assertIsDisplayed()
        composeRule.assertHiddenFromAccessibility(STOP_ICON_TAG)

        stop.performClick()

        assertEquals(1, stops)
    }

    @Test
    fun UX_AC37_while_saving_stop_is_disabled_and_reads_saving_your_walk() {
        showContent(stopping = true)

        composeRule
            .onNodeWithTag(STOP_WALK_TAG)
            .performScrollTo()
            .assertTextEquals("Saving your walk")
            .assertIsNotEnabled()
    }

    @Test
    fun UX_AC38_at_200_percent_the_timer_distance_started_time_and_stop_are_readable_without_truncation_or_overlap() {
        showContent(fontScale = 2.0f, is24Hour = false)

        composeRule.assertNoTextTruncation()
        composeRule.onNodeWithTag(TIMER_TAG, useUnmergedTree = true).assertTextEquals("0:23:41")
        composeRule.onNodeWithTag(DISTANCE_TAG).assertTextEquals("Distance", "1.2 mi")
        composeRule.onNodeWithTag(STARTED_TAG).assertTextEquals("Started", "7:05 pm")
        val timer = composeRule.onNodeWithTag(TIMER_TAG, useUnmergedTree = true).getBoundsInRoot()
        val distance = composeRule.onNodeWithTag(DISTANCE_TAG).getBoundsInRoot()
        val started = composeRule.onNodeWithTag(STARTED_TAG).getBoundsInRoot()
        assertTrue("timer above the cards", timer.bottom <= distance.top && timer.bottom <= started.top)
        assertTrue("the cards side by side", distance.right <= started.left)
        val stop = composeRule.onNodeWithTag(STOP_WALK_TAG).performScrollTo()
        stop.assertTextEquals("Stop and save walk").assertIsDisplayed()
        stop.assertIsLargeFullWidthControl(composeRule)
    }

    private fun assertAccessibleRecording() {
        composeRule.assertAccessibleScreen()
        composeRule.assertHeadingsMarked("Recording walk")
        composeRule.assertHiddenFromAccessibility(LIVE_DOT_TAG)
        composeRule.assertHiddenFromAccessibility(STOP_ICON_TAG)
        composeRule.onNodeWithTag(ROUTE_SKETCH_TAG).assertContentDescriptionEquals("Route so far")
        composeRule.onNodeWithTag(STOP_WALK_TAG).assertIsLargeFullWidthControl(composeRule)
    }

    @Test
    fun UX_AC58_recording_is_accessible_in_light() {
        showContent(route = listOf(LatLon(51.5, -0.13), LatLon(51.501, -0.129)))

        assertAccessibleRecording()
    }

    @Test
    @Config(qualifiers = "+night")
    fun UX_AC58_recording_is_accessible_in_dark() {
        showContent(route = listOf(LatLon(51.5, -0.13), LatLon(51.501, -0.129)))

        assertAccessibleRecording()
    }

    @Test
    fun UX_AC35_the_route_caption_and_spoken_label_show_and_fewer_than_two_fixes_draw_an_empty_panel() {
        showContent(route = listOf(LatLon(51.5, -0.13)))

        composeRule.onNodeWithText("Route so far · saved on this phone").assertIsDisplayed()
        val sketch = composeRule.onNodeWithTag(ROUTE_SKETCH_TAG)
        sketch.assertContentDescriptionEquals("Route so far")
        assertEquals(emptyList<Any>(), sketch.fetchSemanticsNode().config[RouteSketchPoints])
        val size = sketch.fetchSemanticsNode().size
        assertEquals("empty panel", RecordingPalette.mapPanel, pixel(sketch, size.width / 2, size.height / 2))
    }

    /** Hosts the real route with the real ViewModel, Room and clock, for a walk that has just started. */
    private fun showRecordingRoute(): Pair<TestAppContainer, Long> {
        val app = TestAppContainer(temporaryFolder).also { test = it }
        val walk = runBlocking { app.container.walkRepository.startWalk() }
        app.container.recordingStateHolder.begin(walk.id, walk.startUtc)
        val viewModel =
            ViewModelProvider.create(
                viewModelStore,
                viewModelFactory {
                    initializer {
                        RecordingViewModel(
                            app.container.walkRecordingController,
                            app.container.recordingStateHolder,
                            app.container.walkRepository,
                        )
                    }
                },
            )[RecordingViewModel::class]
        composeRule.setContent {
            DogWalkBuddyTheme {
                RecordingRoute(
                    viewModel = viewModel,
                    clock = app.clock,
                    startedFormat = StartedTimeFormat(ZoneOffset.UTC, true),
                    onClose = {},
                )
            }
        }
        composeRule.waitForIdle()
        return app to walk.id
    }

    private fun storeFix(
        app: TestAppContainer,
        walkId: Long,
        seq: Int,
        position: LatLon,
    ) = runBlocking {
        val fix = LocationFix(position.latitude, position.longitude, 5.0, app.clock.instant())
        app.container.walkRepository.recordFix(walkId, seq, fix, 0.0)
    }

    private fun sketchPoints() = composeRule.onNodeWithTag(ROUTE_SKETCH_TAG).fetchSemanticsNode().config[RouteSketchPoints]

    private fun awaitSketchPoints(count: Int) {
        composeRule.waitUntil(WAIT_MS) { sketchPoints().size == count }
    }

    @Test
    fun UX_AC35_fixes_a_b_and_c_are_drawn_as_one_fitted_line_that_redraws_with_the_current_marker_at_c_and_no_network() {
        val (app, walkId) = showRecordingRoute()
        val requestsBefore = app.stub.server.requestCount
        val a = LatLon(51.5000, -0.1300)
        val b = LatLon(51.5010, -0.1290)
        val c = LatLon(51.5020, -0.1270)

        storeFix(app, walkId, 0, a)
        composeRule.waitForIdle()
        assertTrue("one fix draws an empty panel", sketchPoints().isEmpty())

        storeFix(app, walkId, 1, b)
        awaitSketchPoints(2)
        storeFix(app, walkId, 2, c)
        awaitSketchPoints(3)

        val node = composeRule.onNodeWithTag(ROUTE_SKETCH_TAG).fetchSemanticsNode()
        val padding = with(composeRule.density) { 24.dp.toPx() }
        val expected = RouteProjection.fit(listOf(a, b, c), node.size.width.toFloat(), node.size.height.toFloat(), padding)
        assertEquals(expected, sketchPoints())
        val panel = Rect(0f, 0f, node.size.width.toFloat(), node.size.height.toFloat())
        sketchPoints().forEach { assertTrue("$it is inside the panel", panel.contains(Offset(it.x, it.y))) }
        assertEquals("no network request while Recording is shown", requestsBefore, app.stub.server.requestCount)
    }

    @Test
    fun UX_AC34_the_timer_updates_every_second() {
        val (app, _) = showRecordingRoute()
        composeRule.onNodeWithTag(TIMER_TAG, useUnmergedTree = true).assertTextEquals("0:00:00")

        app.clock.advance(Duration.ofSeconds(1))
        composeRule.mainClock.advanceTimeBy(1_000)

        composeRule.waitUntil(WAIT_MS) {
            composeRule
                .onNodeWithTag(TIMER_TAG, useUnmergedTree = true)
                .fetchSemanticsNode()
                .config[SemanticsProperties.Text]
                .single()
                .text == "0:00:01"
        }
        composeRule.onNodeWithTag(TIMER_TAG, useUnmergedTree = true).assertContentDescriptionEquals("1 second")
    }

    private companion object {
        const val WAIT_MS = 5_000L
    }
}
