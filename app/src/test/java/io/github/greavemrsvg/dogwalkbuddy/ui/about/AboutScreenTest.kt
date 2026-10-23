package io.github.greavemrsvg.dogwalkbuddy.ui.about

import android.content.Context
import androidx.annotation.RawRes
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.greavemrsvg.dogwalkbuddy.R
import io.github.greavemrsvg.dogwalkbuddy.domain.rating.Rating
import io.github.greavemrsvg.dogwalkbuddy.ui.a11y.WithFontScale
import io.github.greavemrsvg.dogwalkbuddy.ui.a11y.assertAccessibleScreen
import io.github.greavemrsvg.dogwalkbuddy.ui.a11y.assertHeadingsMarked
import io.github.greavemrsvg.dogwalkbuddy.ui.a11y.assertHiddenFromAccessibility
import io.github.greavemrsvg.dogwalkbuddy.ui.a11y.assertNoTextTruncation
import io.github.greavemrsvg.dogwalkbuddy.ui.a11y.assertTouchTargetIsAtLeast
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.DogWalkBuddyTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The About this advice screen on its own (AC26, AC27, AC51, AC52). */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class AboutScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private var backs = 0

    private fun show(fontScale: Float = 1f) {
        composeRule.setContent {
            DogWalkBuddyTheme {
                WithFontScale(fontScale) { AboutScreen(onBack = { backs++ }) }
            }
        }
    }

    @Test
    fun AC52_the_about_screen_has_no_truncated_text_at_the_largest_font_size() {
        show(fontScale = 2.0f)

        composeRule.assertNoTextTruncation()
        composeRule.onNodeWithText("Rating thresholds").assertExists()
    }

    @Test
    fun AC51_the_about_back_control_is_labelled_and_at_least_48dp() {
        show()

        composeRule.assertAccessibleScreen()
    }

    @Test
    fun AC26_about_names_both_guidance_sources_and_AC27_carries_the_attribution() {
        show()

        composeRule.onNodeWithText("Vets Now", substring = true).assertExists()
        composeRule.onNodeWithText("FOUR PAWS UK", substring = true).assertExists()
        composeRule.onNodeWithText("Weather data: MET Norway (CC BY 4.0)").assertExists()
    }

    private fun assertTextPresent(fragment: String) {
        val matches = composeRule.onAllNodesWithText(fragment, substring = true).fetchSemanticsNodes()
        assertTrue("Missing from About: $fragment", matches.isNotEmpty())
    }

    @Test
    fun AC26_about_shows_the_maplibre_native_notice_in_full() {
        show()

        listOf(
            "BSD 2-Clause License",
            "Copyright (c) 2021 MapLibre contributors",
            "Copyright (c) 2018-2021 MapTiler.com",
            "Copyright (c) 2014-2020 Mapbox",
            "Redistributions of source code must retain the above copyright",
            "Redistributions in binary form must reproduce the above copyright",
            "THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS \"AS",
            "SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.",
        ).forEach(::assertTextPresent)
        composeRule.onNodeWithText("MapLibre Native Android 13.6.1 (BSD 2-Clause License)").assertExists()
    }

    @Test
    fun AC26_about_shows_the_maplibre_gestures_notice_in_full() {
        show()

        listOf(
            "Copyright (c) 2018, Mapbox",
            "Copyright (c) 2024, MapLibre contributors",
            "Copyright (c) 2012, Almer Thie",
            "Copyright (c) 2005-2013, The Android Open Source Project",
        ).forEach(::assertTextPresent)
        composeRule.onNodeWithText("MapLibre Android Gestures 0.0.4 (BSD 2-Clause License)").assertExists()
    }

    @Test
    fun AC26_about_lists_the_apache_libraries_and_carries_the_full_apache_text() {
        show()

        listOf(
            "Activity 1.11.0, Core 1.17.0, Lifecycle 2.9.4, Navigation 2.9.7, Room 2.8.4",
            "Jetpack Compose (Compose BOM 2026.08.00)",
            "kotlinx.coroutines 1.10.2 and kotlinx.serialization 1.10.0",
            "OkHttp 5.1.0 and Okio 3.15.0",
            "MapLibre Native GeoJSON 6.0.1 and Turf 6.0.1",
            "Apache License",
            "Version 2.0, January 2004",
            "TERMS AND CONDITIONS FOR USE, REPRODUCTION, AND DISTRIBUTION",
            "Unless required by applicable law or agreed to in writing, software",
            "END OF TERMS AND CONDITIONS",
            "APPENDIX: How to apply the Apache License to your work.",
        ).forEach(::assertTextPresent)
        composeRule.onNodeWithText("Libraries under the Apache License 2.0").assertExists()
    }

    @Test
    fun AC52_the_licence_notices_are_readable_at_the_largest_font_size() {
        show(fontScale = 2.0f)

        composeRule.assertNoTextTruncation()
        assertTextPresent("Copyright (c) 2021 MapLibre contributors")
        assertTextPresent("Navigation 2.9.7")
        assertTextPresent("END OF TERMS AND CONDITIONS")
    }

    @Test
    fun AC26_about_shows_the_third_party_notices_of_the_maplibre_native_library() {
        show()

        listOf(
            "supercluster.hpp",
            "The FreeType Project LICENSE",
        ).forEach(::assertTextPresent)
        composeRule.onNodeWithText("Third-party code in the MapLibre Native library").assertExists()
    }

    @Test
    fun AC26_about_shows_the_public_suffix_list_heading_and_the_mpl_text() {
        show()

        listOf(
            "Public Suffix List",
            "Mozilla Public License Version 2.0",
            "Note that public_suffix_list.dat is compiled from The Public Suffix List:",
            "https://publicsuffix.org",
        ).forEach(::assertTextPresent)
        composeRule.onNodeWithText("Public Suffix List").assertExists()
    }

    @Test
    fun AC52_the_third_party_and_mpl_notices_are_readable_at_the_largest_font_size() {
        show(fontScale = 2.0f)

        composeRule.assertNoTextTruncation()
        assertTextPresent("supercluster.hpp")
        assertTextPresent("The FreeType Project LICENSE")
        assertTextPresent("Mozilla Public License Version 2.0")
    }

    /** The licence file's paragraphs, read from the bundled resource. */
    private fun paragraphsOf(
        @RawRes resId: Int,
    ): List<String> =
        ApplicationProvider
            .getApplicationContext<Context>()
            .resources
            .openRawResource(resId)
            .bufferedReader(Charsets.UTF_8)
            .use { it.readText() }
            .replace("\r\n", "\n")
            .split("\n\n")
            .filter { it.isNotBlank() }

    private fun assertFontSection(
        name: String,
        @RawRes licence: Int,
        copyright: String,
    ) {
        composeRule
            .onNode(hasText(name) and SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
            .assertExists()
        val paragraphs = paragraphsOf(licence)
        assertTrue("$name licence starts with its copyright line", paragraphs.first().startsWith(copyright))
        assertTrue(paragraphs.any { it.contains("SIL OPEN FONT LICENSE Version 1.1 - 26 February 2007") })
        paragraphs.forEach { paragraph ->
            assertTrue(
                "$name licence paragraph missing: ${paragraph.take(PREVIEW_LENGTH)}",
                composeRule.onAllNodesWithText(paragraph).fetchSemanticsNodes().isNotEmpty(),
            )
        }
    }

    @Test
    fun UX_AC8_about_shows_bricolage_grotesque_and_its_full_licence() {
        show()

        assertFontSection(
            name = "Bricolage Grotesque",
            licence = R.raw.licence_ofl_bricolage_grotesque,
            copyright = "Copyright 2022 The Bricolage Grotesque Project Authors (https://github.com/ateliertriay/bricolage)",
        )
    }

    @Test
    fun UX_AC8_about_shows_figtree_and_its_full_licence() {
        show()

        assertFontSection(
            name = "Figtree",
            licence = R.raw.licence_ofl_figtree,
            copyright = "Copyright 2022 The Figtree Project Authors (https://github.com/erikdkennedy/figtree)",
        )
    }

    @Test
    fun UX_AC8_the_font_sections_follow_the_existing_licence_sections() {
        show()

        fun headingTop(name: String) =
            composeRule
                .onNode(hasText(name) and SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
                .fetchSemanticsNode()
                .positionInRoot.y
        // The last paragraph of the Apache text, the last of the existing licence sections.
        val apacheEnd =
            composeRule
                .onAllNodesWithText("APPENDIX: How to apply the Apache License to your work.", substring = true)
                .onLast()
                .fetchSemanticsNode()
                .positionInRoot.y

        val tops =
            listOf(headingTop("Apache License, Version 2.0"), apacheEnd, headingTop("Bricolage Grotesque"), headingTop("Figtree"))

        assertEquals(tops.sorted(), tops)
        assertEquals(tops.size, tops.toSet().size)
    }

    @Test
    fun UX_AC8_the_font_licences_are_readable_at_the_largest_font_size() {
        show(fontScale = 2.0f)

        composeRule.assertNoTextTruncation()
        assertTextPresent("Copyright 2022 The Bricolage Grotesque Project Authors")
        assertTextPresent("Copyright 2022 The Figtree Project Authors")
    }

    @Test
    fun AC26_back_returns_from_about() {
        show()

        composeRule.onNodeWithContentDescription("Back").assertIsDisplayed().performClick()

        assertEquals(1, backs)
    }

    /** The node is the table cell at [row] and [column], spanning one row and one column. */
    private fun cellAt(
        row: Int,
        column: Int,
    ) = SemanticsMatcher("cell at row $row, column $column") { node ->
        node.config.getOrNull(SemanticsProperties.CollectionItemInfo)?.let {
            it.rowIndex == row && it.columnIndex == column && it.rowSpan == 1 && it.columnSpan == 1
        } == true
    }

    private fun heading(text: String) = composeRule.onNode(hasText(text) and SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))

    private fun topOf(text: String) =
        composeRule
            .onNodeWithText(text)
            .fetchSemanticsNode()
            .positionInRoot.y

    @Test
    fun UX_AC56_about_has_a_back_control_the_title_headline_and_intro() {
        show()

        composeRule.onNodeWithContentDescription("Back").assertTouchTargetIsAtLeast(composeRule, 48.dp, 48.dp)
        composeRule.assertHeadingsMarked("About this advice", "How we rate the weather", "Rating thresholds")
        composeRule
            .onNodeWithText("The overall rating is the worst of these. Heat-sensitive dogs use the values in brackets.")
            .assertIsDisplayed()
        composeRule.onNodeWithText("Rating thresholds").assertIsDisplayed()
    }

    @Test
    fun UX_AC56_the_table_has_the_column_headers_with_rating_icons_and_exactly_the_D11_cells_in_order() {
        show()

        listOf("Factor", "Go", "Caution", "Avoid").forEachIndexed { column, header ->
            heading(header)
                .assert(cellAt(0, column))
        }
        val expected =
            listOf(
                "Heat" to
                    listOf(
                        "below 20\u00A0°C (16\u00A0°C)",
                        "20 to below 25\u00A0°C (16 to below 21\u00A0°C)",
                        "25\u00A0°C or above (21\u00A0°C or above)",
                    ),
                "Pavement" to
                    listOf(
                        "below 20\u00A0°C (16\u00A0°C)",
                        "20 to below 25\u00A0°C (16 to below 21\u00A0°C)",
                        "25\u00A0°C or above (21\u00A0°C or above)",
                    ),
                "Rain" to listOf("below 0.5", "0.5 to 4", "above 4"),
                "Wind" to listOf("below 25", "25 to below 39", "39 or above"),
                "Cold" to listOf("above 0\u00A0°C", "0\u00A0°C or below", "never"),
            )
        val columns = listOf("Go", "Caution", "Avoid")
        expected.forEachIndexed { index, (row, cells) ->
            heading(row)
                .assert(cellAt(index + 1, 0))
            cells.forEachIndexed { column, cell ->
                composeRule
                    .onNodeWithContentDescription("$row, ${columns[column]}, $cell")
                    .assert(hasText(cell))
                    .assert(
                        cellAt(index + 1, column + 1),
                    )
            }
        }
        val rowTops = expected.map { (row, _) -> topOf(row) }
        assertEquals(rowTops.sorted(), rowTops)
        Rating.entries.forEach { rating ->
            composeRule.assertHiddenFromAccessibility(ratingHeaderIconTag(rating))
            val icon = composeRule.onNodeWithTag(ratingHeaderIconTag(rating), useUnmergedTree = true).fetchSemanticsNode()
            val word = heading(ratingWord(rating)).fetchSemanticsNode()
            assertTrue("$rating icon sits in its header", word.boundsInRoot.contains(icon.boundsInRoot.center))
        }
    }

    private fun ratingWord(rating: Rating) =
        when (rating) {
            Rating.GO -> "Go"
            Rating.CAUTION -> "Caution"
            Rating.AVOID -> "Avoid"
        }

    @Test
    fun UX_AC56_the_table_is_a_collection_of_6_rows_and_4_columns() {
        show()

        composeRule
            .onNodeWithTag(THRESHOLDS_TABLE_TAG)
            .assert(
                SemanticsMatcher("a 6 x 4 table") { node ->
                    node.config.getOrNull(SemanticsProperties.CollectionInfo)?.let { it.rowCount == 6 && it.columnCount == 4 } == true
                },
            )
    }

    @Test
    fun UX_AC56_every_existing_threshold_sentence_stays_word_for_word_below_the_table() {
        show()

        val tableBottom =
            composeRule
                .onNodeWithTag(THRESHOLDS_TABLE_TAG)
                .fetchSemanticsNode()
                .boundsInRoot.bottom
        listOf(
            R.string.about_overall,
            R.string.about_heat,
            R.string.about_pavement,
            R.string.about_rain,
            R.string.about_wind,
            R.string.about_cold,
        ).forEach { resId ->
            val sentence = ApplicationProvider.getApplicationContext<Context>().getString(resId)
            assertTrue(sentence, topOf(sentence) > tableBottom)
        }
        composeRule
            .onNodeWithText(
                "Heat (air temperature). Normal: Go below 20 °C, Caution from 20 °C to below 25 °C, Avoid from 25 °C. " +
                    "Sensitive: Go below 16 °C, Caution from 16 °C to below 21 °C, Avoid from 21 °C.",
            ).assertExists()
    }

    @Test
    fun UX_AC57_the_sources_heading_reads_where_this_comes_from_and_the_existing_texts_stay() {
        show()

        composeRule.assertHeadingsMarked("Where this comes from", "Data", "Open-source licences")
        composeRule.onNodeWithText("Guidance sources").assertDoesNotExist()
        listOf(
            "The thresholds are based on this guidance. It is general guidance, and forecasts can be wrong.",
            "Vets Now: heat-related illness can start above about 20 °C.",
            "FOUR PAWS UK: asphalt reaches about 52 °C when the air is 25 °C.",
            "The wind thresholds follow the Beaufort scale's mean wind speeds, as published by the Royal Meteorological Society.",
            "Guidance only, not veterinary advice. If you're worried about your dog, contact a vet.",
            "Weather data: MET Norway (CC BY 4.0)",
            "© OpenStreetMap contributors · © OpenMapTiles",
            "Pollen isn't included in this check",
        ).forEach { composeRule.onNodeWithText(it).assertExists() }
        assertTrue(topOf("Where this comes from") < topOf("Weather data: MET Norway (CC BY 4.0)"))
    }

    @Test
    fun UX_AC60_about_has_no_truncated_text_at_200_percent_in_the_dark_theme() {
        RuntimeEnvironment.setQualifiers("+night")
        show(fontScale = 2.0f)

        composeRule.assertNoTextTruncation()
        composeRule.onNodeWithContentDescription("Heat, Caution, 20 to below 25\u00A0°C (16 to below 21\u00A0°C)").assertExists()
    }

    @Test
    fun UX_AC58_about_is_accessible_in_the_dark_theme() {
        RuntimeEnvironment.setQualifiers("+night")
        show()

        composeRule.assertAccessibleScreen()
        listOf("Factor", "Go", "Caution", "Avoid", "Heat", "Pavement", "Rain", "Wind", "Cold").forEach { heading(it).assertExists() }
    }

    private companion object {
        const val PREVIEW_LENGTH = 60
    }

    @Test
    fun UX_AC56_no_table_cell_line_starts_with_a_degree_sign_at_the_default_font_size() {
        show()

        assertEveryNumberKeepsItsDegreeSign()
    }

    @Test
    fun UX_AC56_no_table_cell_line_starts_with_a_degree_sign_at_200_percent() {
        show(fontScale = 2.0f)

        assertEveryNumberKeepsItsDegreeSign()
    }

    /**
     * Owner decision t8_degree_wrap: each number and its "°C" stay on one line. Every table cell string joins them with
     * a non-breaking space, and no laid-out line of any table cell starts with "°C".
     */
    private fun assertEveryNumberKeepsItsDegreeSign() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val degreeCells =
            listOf(
                R.string.about_table_temp_go,
                R.string.about_table_temp_go_sensitive,
                R.string.about_table_temp_caution,
                R.string.about_table_temp_caution_sensitive,
                R.string.about_table_temp_avoid,
                R.string.about_table_temp_avoid_sensitive,
                R.string.about_table_cold_go,
                R.string.about_table_cold_caution,
            ).map { context.getString(it) }
        degreeCells.forEach { cell ->
            assertTrue(cell, cell.contains("°C"))
            assertFalse("\"$cell\" has a breakable space before °C", Regex("[0-9][ \t\n]°C").containsMatchIn(cell))
            assertTrue("\"$cell\" joins the number and °C", Regex("[0-9]\u00A0°C").containsMatchIn(cell))
        }

        val layouts = mutableListOf<TextLayoutResult>()
        composeRule
            .onAllNodes(hasAnyAncestor(hasTestTag(THRESHOLDS_TABLE_TAG)) and hasText("°C", substring = true), useUnmergedTree = true)
            .fetchSemanticsNodes()
            .forEach { node ->
                node.config
                    .getOrNull(SemanticsActions.GetTextLayoutResult)
                    ?.action
                    ?.invoke(layouts)
            }
        assertEquals("one layout per heat, pavement and cold degree cell", 8, layouts.size)
        layouts.forEach { layout ->
            val text = layout.layoutInput.text.text
            (0 until layout.lineCount).forEach { line ->
                val shown = text.substring(layout.getLineStart(line), layout.getLineEnd(line)).trimStart()
                assertFalse("line $line of \"$text\" starts with °C", shown.startsWith("°C"))
            }
        }
    }
}
