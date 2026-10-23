package io.github.greavemrsvg.dogwalkbuddy.ui.history

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.greavemrsvg.dogwalkbuddy.data.db.WalkEntity
import io.github.greavemrsvg.dogwalkbuddy.data.db.WalkStatus
import io.github.greavemrsvg.dogwalkbuddy.testutil.TestAppContainer
import io.github.greavemrsvg.dogwalkbuddy.testutil.Wcag
import io.github.greavemrsvg.dogwalkbuddy.testutil.awaitCondition
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.DarkFieldKitColors
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.DogWalkBuddyTheme
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.FieldKitColors
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.LightFieldKitColors
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Tester-owned render checks of the this-week card (AC40, AC41) in light and dark. The pixels come from the
 * rendered Walks tab, so they prove the card really uses the fill its contrast is measured on, and that each day dot
 * is drawn in the design's colour for its state (design, Walks, dot table). Scenario as in AC41: walks on Monday and
 * Wednesday, today Thursday 22 Oct 2026.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h1500dp-xxhdpi")
class ThisWeekCardRenderTest {
    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var test: TestAppContainer

    @Before
    fun setUp() {
        test = TestAppContainer(temporaryFolder)
        runBlocking {
            listOf("2026-10-19T08:00:00Z", "2026-10-21T08:00:00Z").forEach { start ->
                val startMs = Instant.parse(start).toEpochMilli()
                test.database.walkDao().insertWalk(
                    WalkEntity(
                        startUtcMs = startMs,
                        endUtcMs = startMs + 1_800_000,
                        status = WalkStatus.COMPLETE,
                        distanceM = 1609.344,
                    ),
                )
            }
        }
        test.clock.advance(Duration.ofDays(3))
    }

    @After
    fun tearDown() {
        test.close()
    }

    /** The design's dot colours, written out from the design table, not read from the code. */
    private data class DotSpec(
        val walked: Color,
        val pastNoWalk: Color,
        val futureRing: Color,
        val todayRing: Color,
    )

    private val lightDots = DotSpec(Color(0xFF8CC2FF), Color(0xFF2E5C94), LightFieldKitColors.summaryCaption, LightFieldKitColors.onPrimary)
    private val darkDots = DotSpec(Color(0xFF0B2747), Color(0xFFD0E4FF), Color(0xFF1E3A5F), Color(0xFF0B2747))

    @Test
    fun UX_AC40_the_card_is_drawn_on_the_primary_fill_its_text_contrast_is_measured_on_in_light() {
        assertCardFill(LightFieldKitColors)
    }

    @Test
    @Config(qualifiers = "+night")
    fun UX_AC40_the_card_is_drawn_on_the_primary_fill_its_text_contrast_is_measured_on_in_dark() {
        assertCardFill(DarkFieldKitColors)
    }

    @Test
    fun UX_AC41_each_dot_is_drawn_in_the_design_colour_for_its_state_in_light() {
        assertDots(LightFieldKitColors, lightDots)
    }

    @Test
    @Config(qualifiers = "+night")
    fun UX_AC41_each_dot_is_drawn_in_the_design_colour_for_its_state_in_dark() {
        assertDots(DarkFieldKitColors, darkDots)
    }

    private fun show(): PixelMap {
        val viewModel = HistoryViewModel(test.container.walkRepository, test.clock, ZoneOffset.UTC)
        composeRule.setContent {
            DogWalkBuddyTheme {
                HistoryRoute(viewModel = viewModel, zoneId = ZoneOffset.UTC, is24Hour = true, onOpenWalk = {})
            }
        }
        awaitCondition("the walks are listed") {
            composeRule.waitForIdle()
            composeRule.onAllNodes(hasText("Wed 21 Oct") and hasText("at 08:00")).fetchSemanticsNodes().isNotEmpty()
        }
        return composeRule.onRoot().captureToImage().toPixelMap()
    }

    private fun bounds(tag: String): Rect = composeRule.onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot

    private fun assertClose(
        what: String,
        expected: Color,
        actual: Color,
    ) {
        val close =
            abs(expected.red - actual.red) <= TOLERANCE &&
                abs(expected.green - actual.green) <= TOLERANCE &&
                abs(expected.blue - actual.blue) <= TOLERANCE &&
                actual.alpha >= 1f - TOLERANCE
        assertTrue("$what: expected $expected, was $actual", close)
    }

    private fun assertCardFill(colors: FieldKitColors) {
        val pixels = show()
        val card = bounds(THIS_WEEK_TAG)
        // The middle of the card's top padding holds no content, so it shows the card fill.
        val fill = pixels[card.center.x.roundToInt(), (card.top + 4 * composeRule.density.density).roundToInt()]

        assertClose("card fill", colors.primary, fill)
        val captionRatio = Wcag.contrastRatio(colors.summaryCaption, fill)
        assertTrue("captions and week range on the drawn fill are $captionRatio:1, need 4.5:1", captionRatio >= 4.5)
        val statRatio = Wcag.contrastRatio(colors.onPrimary, fill)
        assertTrue("stat values on the drawn fill are $statRatio:1, need 4.5:1", statRatio >= 4.5)
    }

    private fun assertDots(
        colors: FieldKitColors,
        spec: DotSpec,
    ) {
        val pixels = show()
        val density = composeRule.density.density
        val dots = (0..6).map { bounds(WEEK_DOT_TAG_PREFIX + it) }

        fun centre(index: Int) = pixels[dots[index].center.x.roundToInt(), dots[index].center.y.roundToInt()]

        // Just above the dot, in the middle of where the 2 dp today ring is drawn outside the 26 dp circle.
        fun aboveDot(index: Int) = pixels[dots[index].center.x.roundToInt(), (dots[index].top - 1 * density).roundToInt()]

        // Monday and Wednesday walked, Tuesday and Thursday past with no walk.
        assertClose("Monday walked fill", spec.walked, centre(0))
        assertClose("Tuesday past-no-walk fill", spec.pastNoWalk, centre(1))
        assertClose("Wednesday walked fill", spec.walked, centre(2))
        assertClose("Thursday past-no-walk fill", spec.pastNoWalk, centre(3))

        // Friday to Sunday are future: an unfilled dashed ring, so the centre shows the card fill and some ring pixel
        // in the dot's box is the ring colour.
        (4..6).forEach { day ->
            assertClose("future day $day centre is unfilled", colors.primary, centre(day))
            val box = dots[day]
            val ringDrawn =
                (box.left.roundToInt() until box.right.roundToInt()).any { x ->
                    (box.top.roundToInt() until box.bottom.roundToInt()).any { y ->
                        val p = pixels[x, y]
                        abs(p.red - spec.futureRing.red) <= TOLERANCE &&
                            abs(p.green - spec.futureRing.green) <= TOLERANCE &&
                            abs(p.blue - spec.futureRing.blue) <= TOLERANCE
                    }
                }
            assertTrue("future day $day has a ring in ${spec.futureRing}", ringDrawn)
        }

        // Only Thursday has the today ring.
        assertClose("Thursday today ring", spec.todayRing, aboveDot(3))
        listOf(0, 1, 2, 4, 5, 6).forEach { day ->
            assertClose("day $day has no today ring", colors.primary, aboveDot(day))
        }
        assertFalse("the today ring differs from the card fill", spec.todayRing == colors.primary)
    }

    private companion object {
        // About 3 of 255 per channel, for colour-space rounding in the native renderer.
        const val TOLERANCE = 0.013f
    }
}
