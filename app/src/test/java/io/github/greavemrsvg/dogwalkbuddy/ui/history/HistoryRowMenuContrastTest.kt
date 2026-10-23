package io.github.greavemrsvg.dogwalkbuddy.ui.history

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Instant
import java.time.ZoneOffset

/**
 * The open row "More" menu must stand out from the row cards behind it in both themes (AC44, AC58,
 * owner_answers.t6_dark_menu): its edge meets 3:1 against the card (WCAG 1.4.11) and its item text meets 4.5:1 on
 * the menu surface. The pixels are read from the rendered popup.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h1500dp-xxhdpi")
class HistoryRowMenuContrastTest {
    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var test: TestAppContainer

    @Before
    fun setUp() {
        test = TestAppContainer(temporaryFolder)
        val start = Instant.parse("2026-10-19T09:15:00Z")
        runBlocking {
            test.database.walkDao().insertWalk(
                WalkEntity(
                    startUtcMs = start.toEpochMilli(),
                    endUtcMs = start.plusSeconds(4500).toEpochMilli(),
                    status = WalkStatus.COMPLETE,
                    distanceM = 3218.688,
                ),
            )
        }
    }

    @After
    fun tearDown() {
        test.close()
    }

    @Test
    fun AC58_the_open_row_menu_edge_and_text_stand_out_from_the_row_card_in_light_theme() {
        assertMenuStandsOut(LightFieldKitColors)
    }

    @Test
    @Config(qualifiers = "+night")
    fun AC58_the_open_row_menu_edge_and_text_stand_out_from_the_row_card_in_dark_theme() {
        assertMenuStandsOut(DarkFieldKitColors)
    }

    private fun assertMenuStandsOut(colors: FieldKitColors) {
        openMenu()
        val pixels = composeRule.onNode(isPopup()).captureToImage().toPixelMap()
        val density = composeRule.density.density
        val midY = pixels.height / 2
        // The outermost column of the menu, one physical pixel in from anti-aliasing, is its visible edge.
        val edge = pixels[1, midY]
        // The menu's own vertical padding area holds no content, so it shows the menu surface.
        val container = pixels[pixels.width / 2, (4 * density).toInt()]
        val card = colors.surface

        val edgeRatio = Wcag.contrastRatio(opaque(edge), card)
        assertTrue("menu edge $edge vs row card $card is $edgeRatio:1, needs 3:1", edgeRatio >= 3.0)
        val textRatio = Wcag.contrastRatio(colors.onSurface, opaque(container))
        assertTrue("item text on menu surface $container is $textRatio:1, needs 4.5:1", textRatio >= 4.5)
    }

    private fun opaque(color: Color): Color = color.copy(alpha = 1f)

    private fun openMenu() {
        val viewModel = HistoryViewModel(test.container.walkRepository, test.clock, ZoneOffset.UTC)
        composeRule.setContent {
            DogWalkBuddyTheme {
                HistoryRoute(
                    viewModel = viewModel,
                    zoneId = ZoneOffset.UTC,
                    is24Hour = true,
                    onOpenWalk = {},
                )
            }
        }
        awaitCondition("the walk is listed") {
            composeRule.waitForIdle()
            composeRule.onAllNodes(hasText("Mon 19 Oct") and hasText("at 09:15")).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithContentDescription("More").performClick()
        composeRule.waitForIdle()
    }
}
