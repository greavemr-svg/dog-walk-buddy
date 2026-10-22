package io.github.greavemrsvg.dogwalkbuddy.ui.about

import android.content.Context
import androidx.annotation.RawRes
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.greavemrsvg.dogwalkbuddy.R
import io.github.greavemrsvg.dogwalkbuddy.ui.a11y.WithFontScale
import io.github.greavemrsvg.dogwalkbuddy.ui.a11y.assertAccessibleScreen
import io.github.greavemrsvg.dogwalkbuddy.ui.a11y.assertNoTextTruncation
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.DogWalkBuddyTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
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

    private companion object {
        const val PREVIEW_LENGTH = 60
    }
}
