package io.github.greavemrsvg.dogwalkbuddy.ui.profile

import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.DogWalkBuddyTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The screen drawn from a fixed [ProfileUiState], for states the real view model only passes through briefly. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h1500dp-xxhdpi")
class ProfileScreenStateTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun show(state: ProfileUiState) {
        composeRule.setContent {
            DogWalkBuddyTheme {
                ProfileScreen(
                    state = state,
                    onNameChange = {},
                    onBreedChange = {},
                    onSensitivityChange = {},
                    onChoosePhoto = {},
                    onRemovePhoto = {},
                    onSave = {},
                    onBack = {},
                )
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
        composeRule.onNodeWithText("Save profile").assertIsNotEnabled()
        composeRule.onNodeWithText("Choose photo").assertIsNotEnabled()
        composeRule.onNodeWithText("Normal").assertIsNotEnabled()
    }

    @Test
    fun AC41_the_fields_and_save_are_enabled_once_the_stored_profile_has_loaded() {
        show(ProfileUiState(isLoaded = true))

        composeRule.onAllNodes(hasSetTextAction())[0].assertIsEnabled()
        composeRule.onNodeWithText("Save profile").assertIsEnabled()
        composeRule.onNodeWithText("Normal").assertIsEnabled()
    }
}
