package io.github.greavemrsvg.dogwalkbuddy.ui.profile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.greavemrsvg.dogwalkbuddy.domain.profile.ProfileError
import io.github.greavemrsvg.dogwalkbuddy.domain.rating.HeatSensitivity
import io.github.greavemrsvg.dogwalkbuddy.testutil.Wcag
import io.github.greavemrsvg.dogwalkbuddy.ui.a11y.WithFontScale
import io.github.greavemrsvg.dogwalkbuddy.ui.a11y.assertAccessibleScreen
import io.github.greavemrsvg.dogwalkbuddy.ui.a11y.assertHeadingsMarked
import io.github.greavemrsvg.dogwalkbuddy.ui.a11y.assertHiddenFromAccessibility
import io.github.greavemrsvg.dogwalkbuddy.ui.a11y.assertNoTextTruncation
import io.github.greavemrsvg.dogwalkbuddy.ui.a11y.assertTouchTargetIsAtLeast
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.DarkFieldKitColors
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.DogWalkBuddyTheme
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.FieldKitColors
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.LightFieldKitColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.abs

/** The screen drawn from a fixed [ProfileUiState], for states the real view model only passes through briefly. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h1500dp-xxhdpi")
class ProfileScreenStateTest {
    @get:Rule
    val composeRule = createComposeRule()

    private var saves = 0

    private fun show(
        state: ProfileUiState,
        fontScale: Float = 1f,
    ) {
        composeRule.setContent {
            DogWalkBuddyTheme {
                WithFontScale(fontScale) {
                    ProfileScreen(
                        state = state,
                        onNameChange = {},
                        onBreedChange = {},
                        onSensitivityChange = {},
                        onChoosePhoto = {},
                        onRemovePhoto = {},
                        onSave = { saves++ },
                    )
                }
            }
        }
    }

    @Test
    fun AC50_a_failed_save_is_announced_as_a_polite_live_region() {
        show(ProfileUiState(isLoaded = true, saveFailed = true))

        composeRule
            .onNodeWithText("Couldn't save the profile. Try again.")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
    }

    @Test
    fun AC41_a_failed_load_is_announced_as_a_polite_live_region() {
        show(ProfileUiState(isLoaded = true, loadFailed = true))

        composeRule
            .onNodeWithText("Couldn't load your dog's profile.")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
    }

    @Test
    fun AC41_no_load_message_is_shown_when_the_load_worked() {
        show(ProfileUiState(isLoaded = true))

        composeRule.onNodeWithText("Couldn't load your dog's profile.").assertDoesNotExist()
    }

    @Test
    fun AC41_the_fields_and_save_are_disabled_until_the_stored_profile_has_loaded() {
        show(ProfileUiState(isLoaded = false))

        composeRule.onNodeWithText("Name").assertIsNotEnabled()
        composeRule.onNodeWithText("Breed (optional)").assertIsNotEnabled()
        composeRule.onNodeWithText("Save").assertIsNotEnabled()
        composeRule.onNodeWithText("Choose photo").assertIsNotEnabled()
        composeRule.onNodeWithText("Normal").assertIsNotEnabled()
    }

    @Test
    fun AC41_the_fields_and_save_are_enabled_once_the_stored_profile_has_loaded() {
        show(ProfileUiState(isLoaded = true))

        composeRule.onAllNodes(hasSetTextAction())[0].assertIsEnabled()
        composeRule.onNodeWithText("Save").assertIsEnabled()
        composeRule.onNodeWithText("Normal").assertIsEnabled()
    }

    private val photo = ImageBitmap(PHOTO_PX, PHOTO_PX)

    @Test
    fun UX_AC50_the_title_is_your_dog_as_a_heading_and_there_is_no_back_control() {
        show(ProfileUiState(isLoaded = true))

        composeRule.assertHeadingsMarked("Your dog")
        composeRule.onNodeWithText("Dog profile").assertDoesNotExist()
        composeRule.onNodeWithText("Back").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Back").assertDoesNotExist()
    }

    @Test
    fun UX_AC50_with_a_photo_it_shows_the_88dp_round_photo_change_photo_and_remove_photo() {
        show(ProfileUiState(isLoaded = true, photo = photo))

        composeRule.onNodeWithTag(PROFILE_AVATAR_TAG).assertWidthIsEqualTo(88.dp).assertHeightIsEqualTo(88.dp)
        composeRule.onNodeWithContentDescription("Photo of your dog").assertIsDisplayed()
        composeRule.onNodeWithText("Change photo").assertIsDisplayed().assertTouchTargetIsAtLeast(composeRule, 48.dp, 48.dp)
        composeRule.onNodeWithText("Remove photo").assertIsDisplayed().assertTouchTargetIsAtLeast(composeRule, 48.dp, 48.dp)
        composeRule.onNodeWithText("Choose photo").assertDoesNotExist()
        composeRule.onNodeWithText("Replace photo").assertDoesNotExist()
    }

    @Test
    fun UX_AC50_with_no_photo_it_shows_the_placeholder_and_choose_photo_and_no_remove_control() {
        show(ProfileUiState(isLoaded = true))

        composeRule.onNodeWithTag(PROFILE_AVATAR_TAG).assertWidthIsEqualTo(88.dp).assertHeightIsEqualTo(88.dp)
        composeRule.assertHiddenFromAccessibility(PROFILE_PLACEHOLDER_TAG)
        composeRule.onNodeWithText("No photo").assertIsDisplayed()
        composeRule.onNodeWithText("Choose photo").assertIsDisplayed().assertTouchTargetIsAtLeast(composeRule, 48.dp, 48.dp)
        composeRule.onNodeWithText("Remove photo").assertDoesNotExist()
        composeRule.onNodeWithText("Change photo").assertDoesNotExist()
    }

    @Test
    fun UX_AC51_the_counters_read_7_of_30_and_8_of_50_and_follow_every_change() {
        var state by mutableStateOf(ProfileUiState(isLoaded = true, name = "Biscuit", breed = "Cockapoo"))
        composeRule.setContent {
            DogWalkBuddyTheme {
                ProfileScreen(
                    state = state,
                    onNameChange = { state = state.copy(name = it) },
                    onBreedChange = { state = state.copy(breed = it) },
                    onSensitivityChange = {},
                    onChoosePhoto = {},
                    onRemovePhoto = {},
                    onSave = {},
                )
            }
        }

        composeRule.onAllNodes(hasSetTextAction())[0].assert(hasText("7 / 30"))
        composeRule.onAllNodes(hasSetTextAction())[1].assert(hasText("8 / 50"))

        composeRule.runOnUiThread { state = state.copy(name = "Biscuits", breed = "") }
        composeRule.waitForIdle()

        composeRule.onAllNodes(hasSetTextAction())[0].assert(hasText("8 / 30"))
        composeRule.onAllNodes(hasSetTextAction())[1].assert(hasText("0 / 50"))
    }

    @Test
    fun UX_AC51_optional_is_a_muted_regular_weight_span_and_the_label_stays_one_text() {
        val muted = Color(0xFF4A5668)

        val breed = mutedOptionalLabel("Breed (optional)", muted)
        val name = mutedOptionalLabel("Name", muted)

        assertEquals("Breed (optional)", breed.text)
        val span = breed.spanStyles.single()
        assertEquals("(optional)", breed.text.substring(span.start, span.end))
        assertEquals(muted, span.item.color)
        assertEquals(FontWeight.Normal, span.item.fontWeight)
        assertEquals("Name", name.text)
        assertTrue(name.spanStyles.isEmpty())
    }

    @Test
    fun UX_AC51_each_field_error_shows_its_exact_text_below_the_field_it_names() {
        show(
            ProfileUiState(
                isLoaded = true,
                breed = "b".repeat(51),
                nameError = ProfileError.NAME_REQUIRED,
                breedError = ProfileError.BREED_TOO_LONG,
            ),
        )

        composeRule.onAllNodes(hasSetTextAction())[0].assert(hasText("Name is required. Enter 1 to 30 characters."))
        composeRule.onAllNodes(hasSetTextAction())[1].assert(hasText("Breed is too long. Use 50 characters or fewer."))
    }

    @Test
    fun UX_AC52_two_radio_cards_announce_name_bands_and_selection_with_normal_selected_by_default() {
        show(ProfileUiState(isLoaded = true))

        val inGroup = hasAnyAncestor(SemanticsMatcher.keyIsDefined(SemanticsProperties.SelectableGroup))
        composeRule
            .onNode(hasText("Normal") and inGroup)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
            .assert(hasText("Caution from 20°C / Avoid from 25°C"))
            .assertIsSelected()
            .assertHeightIsAtLeast(92.dp)
        composeRule
            .onNode(hasText("Sensitive") and inGroup)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
            .assert(hasText("Caution from 16°C / Avoid from 21°C"))
            .assertIsNotSelected()
            .assertHeightIsAtLeast(92.dp)
        composeRule
            .onNodeWithText(
                "Sensitive makes the heat and hot-pavement warnings start 4°C sooner. Rain, wind and cold stay the same.",
            ).assertIsDisplayed()
    }

    @Test
    fun UX_AC52_the_selected_state_moves_to_sensitive() {
        show(ProfileUiState(isLoaded = true, sensitivity = HeatSensitivity.SENSITIVE))

        composeRule.onNodeWithText("Sensitive").assertIsSelected()
        composeRule.onNodeWithText("Normal").assertIsNotSelected()
    }

    @Test
    fun UX_AC53_save_is_a_primary_button_at_least_56dp_high_and_saves() {
        show(ProfileUiState(isLoaded = true))

        composeRule.onNodeWithText("Save").assertHeightIsAtLeast(56.dp).assertIsEnabled()
        composeRule.onNodeWithText("Save").assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
        composeRule.onNodeWithText("Save").performClick()

        assertEquals(1, saves)
    }

    @Test
    fun UX_AC67_each_photo_message_shows_word_for_word_in_error_text_in_the_light_theme() = assertPhotoMessages(LightFieldKitColors)

    @Test
    fun UX_AC67_each_photo_message_shows_word_for_word_in_error_text_in_the_dark_theme() {
        RuntimeEnvironment.setQualifiers("+night")
        assertPhotoMessages(DarkFieldKitColors)
    }

    private fun assertPhotoMessages(colors: FieldKitColors) {
        assertTrue(Wcag.contrastRatio(colors.errorText, colors.background) >= MIN_TEXT_CONTRAST)
        var problem by mutableStateOf<PhotoProblem?>(null)
        composeRule.setContent {
            DogWalkBuddyTheme {
                ProfileScreen(
                    state = ProfileUiState(isLoaded = true, photoProblem = problem),
                    onNameChange = {},
                    onBreedChange = {},
                    onSensitivityChange = {},
                    onChoosePhoto = {},
                    onRemovePhoto = {},
                    onSave = {},
                )
            }
        }
        mapOf(
            PhotoProblem.TOO_LARGE to "That photo is too large. Choose one under 20 MB and 8000 pixels on each side.",
            PhotoProblem.NOT_AN_IMAGE to "That file isn't a photo the app can use. Choose a different one.",
            PhotoProblem.UNREADABLE to "Couldn't read that photo. Try another one.",
        ).forEach { (which, text) ->
            composeRule.runOnUiThread { problem = which }
            composeRule.waitForIdle()
            val node = composeRule.onNodeWithText(text).assertIsDisplayed()
            assertTrue("$which is drawn in errorText", node.captureToImage().containsColour(colors.errorText))
        }
    }

    private fun ImageBitmap.containsColour(expected: Color): Boolean {
        val pixels = toPixelMap()
        for (x in 0 until width) {
            for (y in 0 until height) {
                val pixel = pixels[x, y]
                if (abs(pixel.red - expected.red) < TOLERANCE &&
                    abs(pixel.green - expected.green) < TOLERANCE &&
                    abs(pixel.blue - expected.blue) < TOLERANCE
                ) {
                    return true
                }
            }
        }
        return false
    }

    @Test
    fun UX_AC60_the_dog_tab_has_no_truncated_text_at_200_percent_with_a_30_character_name() {
        show(
            ProfileUiState(
                isLoaded = true,
                name = "Sir Barkington Wigglesworth II",
                breed = "Cavalier King Charles Spaniel",
                photo = photo,
                photoProblem = PhotoProblem.TOO_LARGE,
                justSaved = true,
            ),
            fontScale = 2f,
        )

        composeRule.assertNoTextTruncation()
        composeRule.onAllNodes(hasSetTextAction())[0].assert(hasText("30 / 30"))
    }

    @Test
    fun UX_AC58_the_dog_tab_is_accessible_with_a_photo_in_the_dark_theme() {
        RuntimeEnvironment.setQualifiers("+night")
        show(ProfileUiState(isLoaded = true, photo = photo, photoProblem = PhotoProblem.UNREADABLE))

        composeRule.assertAccessibleScreen()
        composeRule.assertHeadingsMarked("Your dog", "Heat sensitivity")
        assertNull(
            composeRule
                .onNodeWithTag(PROFILE_AVATAR_TAG)
                .fetchSemanticsNode()
                .config
                .getOrNull(SemanticsProperties.Role),
        )
    }

    @Test
    fun UX_AC50_K4_change_photo_and_remove_photo_are_drawn_at_least_48dp_high_not_only_touchable() {
        show(ProfileUiState(isLoaded = true, photo = photo))

        // K4: the pack draws Remove photo 44 px high. The drawn control (its layout bounds, not the expanded touch
        // bounds) must be at least 48 dp.
        composeRule.onNodeWithText("Remove photo").assertHeightIsAtLeast(48.dp).assertWidthIsAtLeast(48.dp)
        composeRule.onNodeWithText("Change photo").assertHeightIsAtLeast(48.dp).assertWidthIsAtLeast(48.dp)
    }

    @Test
    fun UX_AC50_K4_choose_photo_is_drawn_at_least_48dp_high_with_no_photo() {
        show(ProfileUiState(isLoaded = true))

        composeRule.onNodeWithText("Choose photo").assertHeightIsAtLeast(48.dp).assertWidthIsAtLeast(48.dp)
    }

    @Test
    fun UX_AC51_a_31_character_name_shows_the_exact_too_long_text_under_the_name_field_only() {
        show(ProfileUiState(isLoaded = true, name = "a".repeat(31), nameError = ProfileError.NAME_TOO_LONG))

        val text = "Name is too long. Use 30 characters or fewer."
        composeRule.onAllNodes(hasSetTextAction())[0].assert(hasText(text))
        composeRule.onAllNodes(hasSetTextAction())[1].assert(!hasText(text)).assert(hasText("0 / 50"))
    }

    @Test
    fun UX_AC51_a_51_character_breed_error_is_under_the_breed_field_and_not_the_name_field() {
        show(ProfileUiState(isLoaded = true, name = "Biscuit", breed = "b".repeat(51), breedError = ProfileError.BREED_TOO_LONG))

        val text = "Breed is too long. Use 50 characters or fewer."
        composeRule.onAllNodes(hasSetTextAction())[1].assert(hasText(text))
        composeRule.onAllNodes(hasSetTextAction())[0].assert(!hasText(text)).assert(hasText("7 / 30"))
    }

    private companion object {
        const val PHOTO_PX = 64
        const val MIN_TEXT_CONTRAST = 4.5
        const val TOLERANCE = 0.02f
    }
}
