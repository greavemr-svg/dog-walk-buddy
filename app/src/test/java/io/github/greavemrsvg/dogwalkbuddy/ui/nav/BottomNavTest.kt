package io.github.greavemrsvg.dogwalkbuddy.ui.nav

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.greavemrsvg.dogwalkbuddy.data.repo.PhotoChange
import io.github.greavemrsvg.dogwalkbuddy.domain.profile.ProfileInput
import io.github.greavemrsvg.dogwalkbuddy.domain.rating.HeatSensitivity
import io.github.greavemrsvg.dogwalkbuddy.testutil.TestAppContainer
import io.github.greavemrsvg.dogwalkbuddy.ui.a11y.WithFontScale
import io.github.greavemrsvg.dogwalkbuddy.ui.a11y.assertAccessibleScreen
import io.github.greavemrsvg.dogwalkbuddy.ui.a11y.assertHeadingsMarked
import io.github.greavemrsvg.dogwalkbuddy.ui.a11y.assertHiddenFromAccessibility
import io.github.greavemrsvg.dogwalkbuddy.ui.a11y.assertNoTextTruncation
import io.github.greavemrsvg.dogwalkbuddy.ui.a11y.assertTouchTargetIsAtLeast
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.DogWalkBuddyTheme
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The bottom navigation bar and its tabs through the real navigation and ViewModels, with a stub MET server
 * (AC9-AC11, AC13-AC15, AC53 tiles, AC58, AC60 b and c). The first screen is a rated check, so the footnote
 * "About this advice" link is shown.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class BottomNavTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var test: TestAppContainer

    @Before
    fun setUp() {
        test = TestAppContainer(temporaryFolder)
        test.stub.enqueueFixture()
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
        awaitText("Good conditions for a walk")
    }

    private fun awaitText(text: String) {
        composeRule.waitUntil(WAIT_MS) {
            composeRule.onAllNodes(hasText(text), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun saveProfile(name: String) =
        runBlocking { test.container.profileRepository.save(ProfileInput(name, null), HeatSensitivity.NORMAL, PhotoChange.Keep) }

    private fun tapNav(tag: String) = composeRule.onNodeWithTag(tag).performClick()

    private fun systemBack() = composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }

    private fun navItems() =
        composeRule
            .onAllNodes(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab))
            .fetchSemanticsNodes()
            .sortedBy { it.boundsInRoot.left }

    private fun dogLabelLayout(): TextLayoutResult {
        val node = composeRule.onNodeWithTag(NAV_DOG_LABEL_TAG, useUnmergedTree = true).fetchSemanticsNode()
        val layouts = mutableListOf<TextLayoutResult>()
        node.config
            .getOrNull(SemanticsActions.GetTextLayoutResult)
            ?.action
            ?.invoke(layouts)
        return layouts.single()
    }

    private fun assertOnlySelected(tag: String) {
        listOf(NAV_TODAY_TAG, NAV_WALKS_TAG, NAV_DOG_TAG).forEach { item ->
            if (item == tag) {
                composeRule.onNodeWithTag(item).assertIsSelected()
            } else {
                composeRule.onNodeWithTag(item).assertIsNotSelected()
            }
        }
    }

    @Test
    fun UX_AC9_the_app_opens_on_today_with_three_labelled_nav_items_in_order() {
        showApp()

        val items = navItems()
        assertEquals(
            listOf("Today", "Walks", "Dog"),
            items.map { node ->
                node.config
                    .getOrNull(SemanticsProperties.Text)
                    .orEmpty()
                    .joinToString { it.text }
            },
        )
        assertOnlySelected(NAV_TODAY_TAG)
        composeRule.onNodeWithText("Good time to walk?").assertIsDisplayed()
        listOf(NAV_TODAY_TAG, NAV_WALKS_TAG, NAV_DOG_TAG).forEach { tag ->
            composeRule.onNodeWithTag(tag).assertIsDisplayed().assertTouchTargetIsAtLeast(composeRule, 80.dp, 56.dp)
            composeRule.onNodeWithTag(tag + NAV_ICON_TAG_SUFFIX, useUnmergedTree = true).assertIsDisplayed()
        }
    }

    @Test
    fun UX_AC10_tapping_a_nav_item_shows_its_destination_and_selects_it() {
        showApp()

        tapNav(NAV_WALKS_TAG)
        awaitText("This week")
        assertOnlySelected(NAV_WALKS_TAG)

        tapNav(NAV_DOG_TAG)
        awaitText("Save profile")
        assertOnlySelected(NAV_DOG_TAG)

        tapNav(NAV_TODAY_TAG)
        awaitText("Good conditions for a walk")
        assertOnlySelected(NAV_TODAY_TAG)
    }

    @Test
    fun UX_AC10_the_home_link_list_no_longer_exists() {
        showApp()

        listOf("Walk history", "Dog profile", "Privacy").forEach { link ->
            composeRule.onNodeWithText(link).assertDoesNotExist()
        }
        // Only the check's own footnote link is left; the Home "About this advice" link is gone.
        assertEquals(
            1,
            composeRule.onAllNodes(hasText("About this advice") and hasClickAction()).fetchSemanticsNodes().size,
        )
    }

    @Test
    fun UX_AC11_system_back_on_the_walks_tab_shows_today() {
        showApp()
        tapNav(NAV_WALKS_TAG)
        awaitText("This week")

        systemBack()

        awaitText("Good conditions for a walk")
        assertOnlySelected(NAV_TODAY_TAG)
        assertFalse(composeRule.activity.isFinishing)
    }

    @Test
    fun UX_AC11_system_back_on_the_dog_tab_shows_today() {
        showApp()
        tapNav(NAV_WALKS_TAG)
        tapNav(NAV_DOG_TAG)
        awaitText("Save profile")

        systemBack()

        awaitText("Good conditions for a walk")
        assertOnlySelected(NAV_TODAY_TAG)
        assertFalse(composeRule.activity.isFinishing)
    }

    @Test
    fun UX_AC11_system_back_on_today_leaves_the_app() {
        showApp()

        systemBack()

        assertTrue(composeRule.activity.isFinishing)
    }

    @Test
    fun UX_AC13_privacy_is_two_taps_from_the_first_screen_through_the_dog_tab_tile() {
        showApp()

        tapNav(NAV_DOG_TAG)
        composeRule.onNodeWithText("Privacy & data").performScrollTo().performClick()

        composeRule.onNodeWithText("There are no accounts, analytics or tracking.").assertExists()
        composeRule.onNodeWithTag(NAV_DOG_TAG).assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Back").performClick()
        awaitText("Save profile")
        assertOnlySelected(NAV_DOG_TAG)
    }

    @Test
    fun UX_AC14_about_is_one_tap_from_the_check_and_also_reached_from_the_dog_tab_tile() {
        showApp()

        composeRule.onNode(hasText("About this advice") and hasClickAction()).performScrollTo().performClick()
        composeRule.onNodeWithText("Rating thresholds").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Back").performClick()
        awaitText("Good conditions for a walk")
        assertOnlySelected(NAV_TODAY_TAG)

        tapNav(NAV_DOG_TAG)
        composeRule.onNodeWithText("About the advice").performScrollTo().performClick()
        composeRule.onNodeWithText("Rating thresholds").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Back").performClick()
        awaitText("Save profile")
        assertOnlySelected(NAV_DOG_TAG)
    }

    @Test
    fun UX_AC15_the_dog_label_is_the_saved_name_and_returns_to_Dog_after_delete_all_my_data() {
        showApp()
        composeRule.onNodeWithTag(NAV_DOG_TAG).assertTextEquals("Dog")

        tapNav(NAV_DOG_TAG)
        composeRule.waitUntil(WAIT_MS) { composeRule.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().isNotEmpty() }
        composeRule.onAllNodes(hasSetTextAction())[0].performTextInput("Biscuit")
        composeRule.onNodeWithText("Save profile").performScrollTo().performClick()
        awaitText("Profile saved")
        composeRule.waitUntil(WAIT_MS) {
            navItems()
                .last()
                .config[SemanticsProperties.Text]
                .single()
                .text == "Biscuit"
        }
        composeRule.onNodeWithTag(NAV_DOG_TAG).assertTextEquals("Biscuit")

        composeRule.onNodeWithText("Privacy & data").performScrollTo().performClick()
        composeRule.onNodeWithText("Delete all my data").performScrollTo().performClick()
        composeRule.onNodeWithText("Delete everything").performClick()
        awaitText("All your data has been deleted from this phone.")
        composeRule.onNodeWithContentDescription("Back").performClick()

        composeRule.waitUntil(WAIT_MS) {
            navItems()
                .last()
                .config[SemanticsProperties.Text]
                .single()
                .text == "Dog"
        }
        composeRule.onNodeWithTag(NAV_DOG_TAG).assertTextEquals("Dog")
        // The dog tab forgets the deleted profile too.
        composeRule.onNodeWithText("Biscuit").assertDoesNotExist()
    }

    @Test
    fun UX_AC60_a_30_character_name_stays_on_one_line_shortened_with_an_ellipsis_and_is_spoken_in_full() {
        saveProfile(LONG_NAME)
        showApp()
        composeRule.waitUntil(WAIT_MS) {
            navItems()
                .last()
                .config[SemanticsProperties.Text]
                .single()
                .text == LONG_NAME
        }

        val layout = dogLabelLayout()
        assertEquals(1, layout.lineCount)
        assertTrue("the name is shortened with an ellipsis", layout.isLineEllipsized(0))
        composeRule.onNodeWithTag(NAV_DOG_TAG).assertTextEquals(LONG_NAME)
    }

    @Test
    fun UX_AC60_at_200_percent_a_30_character_name_is_the_only_shortened_text_and_is_spoken_in_full() {
        saveProfile(LONG_NAME)
        showApp(fontScale = 2f)
        composeRule.waitUntil(WAIT_MS) {
            navItems()
                .last()
                .config[SemanticsProperties.Text]
                .single()
                .text == LONG_NAME
        }

        val layout = dogLabelLayout()
        assertEquals(1, layout.lineCount)
        assertTrue("the name is shortened with an ellipsis", layout.isLineEllipsized(0))
        composeRule.onNodeWithTag(NAV_DOG_TAG).assertTextEquals(LONG_NAME)
        composeRule.assertNoTextTruncation(allowEllipsisTags = setOf(NAV_DOG_LABEL_TAG))
    }

    @Test
    fun UX_AC60_at_200_percent_with_no_profile_the_nav_labels_are_never_shortened() {
        showApp(fontScale = 2f)

        composeRule.assertNoTextTruncation()
        assertEquals(listOf("Today", "Walks", "Dog"), navItems().map { it.config[SemanticsProperties.Text].single().text })
    }

    @Test
    fun UX_AC60_a_name_that_fits_shows_in_full_with_no_ellipsis() {
        saveProfile("Biscuit")
        showApp()
        composeRule.waitUntil(WAIT_MS) {
            navItems()
                .last()
                .config[SemanticsProperties.Text]
                .single()
                .text == "Biscuit"
        }

        val layout = dogLabelLayout()
        assertEquals(1, layout.lineCount)
        assertFalse("the name is not shortened", layout.isLineEllipsized(0))
        composeRule.onNodeWithTag(NAV_DOG_TAG).assertTextEquals("Biscuit")
    }

    @Test
    fun UX_AC58_the_tabs_are_accessible_with_headings_and_decorative_nav_icons() {
        showApp()
        composeRule.assertAccessibleScreen()
        listOf(NAV_TODAY_TAG, NAV_WALKS_TAG, NAV_DOG_TAG).forEach { tag ->
            composeRule.assertHiddenFromAccessibility(tag + NAV_ICON_TAG_SUFFIX)
        }

        tapNav(NAV_WALKS_TAG)
        awaitText("This week")
        composeRule.assertAccessibleScreen()
        composeRule.assertHeadingsMarked("Walks")

        tapNav(NAV_DOG_TAG)
        awaitText("Save profile")
        composeRule.assertAccessibleScreen()
        composeRule.assertHeadingsMarked("Dog profile")
    }

    @Test
    @Config(qualifiers = "+night")
    fun UX_AC58_in_dark_the_tabs_are_accessible_with_headings_and_decorative_nav_icons() {
        UX_AC58_the_tabs_are_accessible_with_headings_and_decorative_nav_icons()
    }

    private companion object {
        const val WAIT_MS = 10_000L

        /** 30 characters, the longest name the profile allows. */
        const val LONG_NAME = "Sir Barkington of Puddle Lanes"
    }
}
