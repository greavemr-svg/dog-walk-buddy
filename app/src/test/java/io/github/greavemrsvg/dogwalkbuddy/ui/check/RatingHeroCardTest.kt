package io.github.greavemrsvg.dogwalkbuddy.ui.check

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasParent
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.greavemrsvg.dogwalkbuddy.domain.check.Factor
import io.github.greavemrsvg.dogwalkbuddy.domain.check.FactorCell
import io.github.greavemrsvg.dogwalkbuddy.domain.check.FactorGrid
import io.github.greavemrsvg.dogwalkbuddy.domain.rating.HeatSensitivity
import io.github.greavemrsvg.dogwalkbuddy.domain.rating.HourlyConditions
import io.github.greavemrsvg.dogwalkbuddy.domain.rating.PavementFactor
import io.github.greavemrsvg.dogwalkbuddy.domain.rating.Rating
import io.github.greavemrsvg.dogwalkbuddy.domain.rating.RatingEngine
import io.github.greavemrsvg.dogwalkbuddy.domain.rating.Reason
import io.github.greavemrsvg.dogwalkbuddy.ui.a11y.assertAnnouncesRatingThenReason
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.DarkRatingPalettes
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.DogWalkBuddyTheme
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.LightRatingPalettes
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.RatingPalettes
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Instant

/** The hero card: factor grid values and highlight, rating colours and icon, and the announcement (AC17-AC19). */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class RatingHeroCardTest {
    @get:Rule
    val composeRule = createComposeRule()

    private var rating by mutableStateOf(Rating.GO)
    private var reason by mutableStateOf(Reason.GOOD)
    private var factors by mutableStateOf(emptyList<FactorCell>())

    private fun show() {
        composeRule.setContent { DogWalkBuddyTheme { RatingHeroCard(rating = rating, reason = reason, factors = factors) } }
    }

    private fun showConditions(
        conditions: HourlyConditions,
        isDaylight: Boolean,
    ) {
        val rated = RatingEngine.rate(conditions, HeatSensitivity.NORMAL, isDaylight)
        rating = rated.rating
        reason = rated.reason
        factors = FactorGrid.cells(conditions, RatingEngine.pavementFactor(conditions, HeatSensitivity.NORMAL, isDaylight), rated.reason)
    }

    private fun assertCell(
        factor: Factor,
        caption: String,
        value: String,
        highlighted: Boolean,
    ) {
        val tag = factor.testTag()
        composeRule.onNode(hasText(value) and hasParent(hasTestTag(tag)), useUnmergedTree = true).assertExists()
        composeRule.onNode(hasText(caption) and hasParent(hasTestTag(tag)), useUnmergedTree = true).assertExists()
        composeRule
            .onNodeWithTag(tag, useUnmergedTree = true)
            .assert(SemanticsMatcher.expectValue(FactorHighlighted, highlighted))
    }

    @Test
    fun UX_AC17_the_daylight_example_shows_whole_numbers_and_warm_pavement_highlighted() {
        showConditions(HourlyConditions(HOUR, 21.0, 1.0, 26.0, 20.0), isDaylight = true)
        show()

        assertCell(Factor.AIR, "Air", "21°C", highlighted = false)
        assertCell(Factor.RAIN, "Rain", "1 mm/h", highlighted = false)
        assertCell(Factor.WIND, "Wind", "26 mph", highlighted = false)
        assertCell(Factor.PAVEMENT, "Pavement", "Warm", highlighted = true)
    }

    @Test
    fun UX_AC17_the_night_example_rounds_negative_halves_away_from_zero_and_highlights_air() {
        showConditions(HourlyConditions(HOUR, -2.5, 0.0, 5.0, 20.0), isDaylight = false)
        show()

        assertCell(Factor.AIR, "Air", "-3°C", highlighted = true)
        assertCell(Factor.RAIN, "Rain", "0 mm/h", highlighted = false)
        assertCell(Factor.WIND, "Wind", "5 mph", highlighted = false)
        assertCell(Factor.PAVEMENT, "Pavement", "Not rated", highlighted = false)

        showConditions(HourlyConditions(HOUR, -0.4, 0.0, 5.0, 20.0), isDaylight = false)
        assertCell(Factor.AIR, "Air", "0°C", highlighted = true)
    }

    @Test
    fun UX_AC17_pavement_reads_cool_or_hot_and_a_missing_value_reads_no_data_with_no_highlight() {
        showConditions(HourlyConditions(HOUR, 14.5, 0.4, 24.6, 20.0), isDaylight = true)
        show()
        assertCell(Factor.AIR, "Air", "15°C", highlighted = false)
        assertCell(Factor.RAIN, "Rain", "0 mm/h", highlighted = false)
        assertCell(Factor.WIND, "Wind", "25 mph", highlighted = false)
        assertCell(Factor.PAVEMENT, "Pavement", "Cool", highlighted = false)

        showConditions(HourlyConditions(HOUR, 30.0, 0.0, 5.0, 20.0), isDaylight = true)
        assertCell(Factor.PAVEMENT, "Pavement", "Hot", highlighted = true)

        showConditions(HourlyConditions(HOUR, null, 0.0, 5.0, 20.0), isDaylight = true)
        assertEquals(Reason.DATA_MISSING, reason)
        assertCell(Factor.AIR, "Air", "No data", highlighted = false)
        assertCell(Factor.PAVEMENT, "Pavement", "No data", highlighted = false)
        Factor.entries.forEach {
            composeRule.onNodeWithTag(it.testTag(), useUnmergedTree = true).assert(SemanticsMatcher.expectValue(FactorHighlighted, false))
        }
    }

    @Test
    fun UX_AC19_the_card_is_one_polite_node_spoken_as_rating_reason_then_the_four_cells() {
        rating = Rating.CAUTION
        reason = Reason.WINDY
        factors = FactorGrid.cells(HourlyConditions(HOUR, 15.0, 0.0, 26.3, null), PavementFactor.NotRated, Reason.WINDY)
        show()

        val card = composeRule.onNodeWithTag(RATING_BLOCK_TAG)
        card.assertAnnouncesRatingThenReason("Caution", "Windy", "Air 15°C, Rain 0 mm/h, Wind 26 mph, Pavement Not rated")
        card.assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
        // The merged tree has exactly the one card node: the cells add nothing of their own.
        assertEquals(
            0,
            composeRule
                .onNodeWithTag(RATING_BLOCK_TAG)
                .fetchSemanticsNode()
                .children.size,
        )
    }

    @Test
    fun UX_AC18_the_card_uses_the_rating_container_colour_and_icon_in_light() {
        assertContainerColours(LightRatingPalettes)
    }

    @Test
    fun UX_AC18_the_card_uses_the_rating_container_colour_and_icon_in_dark() {
        RuntimeEnvironment.setQualifiers("+night")
        assertContainerColours(DarkRatingPalettes)
    }

    private fun assertContainerColours(palettes: RatingPalettes) {
        factors = FactorGrid.cells(HourlyConditions(HOUR, 15.0, 0.0, 5.0, 100.0), PavementFactor.NotRated, Reason.GOOD)
        show()
        Rating.entries.forEach { current ->
            rating = current
            composeRule.waitForIdle()
            val image = composeRule.onNodeWithTag(RATING_BLOCK_TAG).captureToImage().toPixelMap()
            assertEquals(current.name, palettes.of(current).container, image[image.width / 2, 2])
            composeRule.onNodeWithTag(RATING_ICON_TAG_PREFIX + current.iconRes(), useUnmergedTree = true).assertExists()
        }
    }

    private companion object {
        val HOUR: Instant = Instant.parse("2026-10-19T12:00:00Z")
    }
}
