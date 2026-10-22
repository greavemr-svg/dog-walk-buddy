package io.github.greavemrsvg.dogwalkbuddy.ui.nav

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsConfiguration
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.greavemrsvg.dogwalkbuddy.data.repo.PhotoChange
import io.github.greavemrsvg.dogwalkbuddy.domain.profile.ProfileInput
import io.github.greavemrsvg.dogwalkbuddy.domain.rating.HeatSensitivity
import io.github.greavemrsvg.dogwalkbuddy.testutil.TestAppContainer
import io.github.greavemrsvg.dogwalkbuddy.ui.a11y.WithFontScale
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
 * Tester-added edge cases for the bottom navigation (task 2): system Back after several tab switches (AC11), the
 * tiles' place below Save (AC53), and the spoken dog label at both font scales (AC60 b, c).
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class BottomNavEdgeCasesTest {
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

    private fun tapNav(tag: String) = composeRule.onNodeWithTag(tag).performClick()

    private fun systemBack() = composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }

    private fun assertOnlySelected(tag: String) {
        listOf(NAV_TODAY_TAG, NAV_WALKS_TAG, NAV_DOG_TAG).forEach { item ->
            if (item == tag) composeRule.onNodeWithTag(item).assertIsSelected() else composeRule.onNodeWithTag(item).assertIsNotSelected()
        }
    }

    private fun awaitDogLabel(name: String) {
        composeRule.waitUntil(WAIT_MS) {
            composeRule
                .onAllNodes(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab))
                .fetchSemanticsNodes()
                .any { node -> textOf(node.config) == name }
        }
    }

    /** What TalkBack reads for the merged dog item: its content description if it has one, otherwise its text. */
    private fun spokenDogLabel(): String {
        val config = composeRule.onNodeWithTag(NAV_DOG_TAG).fetchSemanticsNode().config
        val description = config.getOrNull(SemanticsProperties.ContentDescription).orEmpty()
        val text = config.getOrNull(SemanticsProperties.Text).orEmpty().map { it.text }
        return (description.ifEmpty { text }).joinToString(" ")
    }

    @Test
    fun UX_AC11_after_many_tab_switches_one_system_back_shows_today_and_a_second_leaves_the_app() {
        showApp()
        tapNav(NAV_WALKS_TAG)
        awaitText("This week")
        tapNav(NAV_DOG_TAG)
        awaitText("Save profile")
        tapNav(NAV_WALKS_TAG)
        awaitText("This week")
        tapNav(NAV_WALKS_TAG)

        systemBack()

        awaitText("Good conditions for a walk")
        assertOnlySelected(NAV_TODAY_TAG)
        assertFalse(composeRule.activity.isFinishing)

        systemBack()

        assertTrue(composeRule.activity.isFinishing)
    }

    @Test
    fun UX_AC11_after_returning_to_today_by_its_nav_item_system_back_leaves_the_app() {
        showApp()
        tapNav(NAV_DOG_TAG)
        awaitText("Save profile")
        tapNav(NAV_TODAY_TAG)
        awaitText("Good conditions for a walk")
        assertOnlySelected(NAV_TODAY_TAG)

        systemBack()

        assertTrue(composeRule.activity.isFinishing)
    }

    @Test
    fun UX_AC11_system_back_on_the_dog_tab_after_privacy_shows_today() {
        showApp()
        tapNav(NAV_DOG_TAG)
        composeRule.onNodeWithText("Privacy & data").performScrollTo().performClick()
        awaitText("There are no accounts, analytics or tracking.")
        composeRule.onNodeWithContentDescription("Back").performClick()
        awaitText("Save profile")
        assertOnlySelected(NAV_DOG_TAG)

        systemBack()

        awaitText("Good conditions for a walk")
        assertOnlySelected(NAV_TODAY_TAG)
        assertFalse(composeRule.activity.isFinishing)
    }

    @Test
    fun UX_AC53_the_privacy_and_about_tiles_sit_below_save() {
        showApp()
        tapNav(NAV_DOG_TAG)
        awaitText("Save profile")

        composeRule.onNodeWithText("About the advice").performScrollTo()
        val save = composeRule.onNodeWithText("Save profile").fetchSemanticsNode().boundsInRoot
        val privacy = composeRule.onNodeWithText("Privacy & data").fetchSemanticsNode().boundsInRoot
        val about = composeRule.onNodeWithText("About the advice").fetchSemanticsNode().boundsInRoot

        assertTrue("the Privacy tile ($privacy) is below Save ($save)", privacy.top >= save.bottom)
        assertTrue("the About tile ($about) is below Save ($save)", about.top >= save.bottom)
    }

    @Test
    fun UX_AC60_a_30_character_name_is_spoken_in_full_at_font_scale_1() {
        assertLongNameSpokenInFull(fontScale = 1f)
    }

    @Test
    fun UX_AC60_a_30_character_name_is_spoken_in_full_at_font_scale_2() {
        assertLongNameSpokenInFull(fontScale = 2f)
    }

    @Test
    fun UX_AC60_a_name_that_fits_is_spoken_in_full_at_font_scale_1() {
        runBlocking { test.container.profileRepository.save(ProfileInput("Biscuit", null), HeatSensitivity.NORMAL, PhotoChange.Keep) }
        showApp()
        awaitDogLabel("Biscuit")

        assertEquals("Biscuit", spokenDogLabel())
        val dogItem = composeRule.onNodeWithTag(NAV_DOG_TAG).fetchSemanticsNode()
        assertTrue(SemanticsProperties.Selected in dogItem.config)
    }

    private fun textOf(config: SemanticsConfiguration): String? {
        val texts = config.getOrNull(SemanticsProperties.Text) ?: return null
        return texts.singleOrNull()?.text
    }

    private fun assertLongNameSpokenInFull(fontScale: Float) {
        runBlocking { test.container.profileRepository.save(ProfileInput(LONG_NAME, null), HeatSensitivity.NORMAL, PhotoChange.Keep) }
        showApp(fontScale)
        awaitDogLabel(LONG_NAME)

        assertTrue("spoken label \"${spokenDogLabel()}\" contains the full name", spokenDogLabel().contains(LONG_NAME))
        assertFalse("the spoken label is not shortened", spokenDogLabel().contains("…"))
    }

    private companion object {
        const val WAIT_MS = 10_000L

        /** 30 characters, the longest name the profile allows. */
        const val LONG_NAME = "Sir Barkington of Puddle Lanes"
    }
}
