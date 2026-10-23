package io.github.greavemrsvg.dogwalkbuddy.ui.profile

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.greavemrsvg.dogwalkbuddy.data.db.AppDatabase
import io.github.greavemrsvg.dogwalkbuddy.data.repo.PhotoStore
import io.github.greavemrsvg.dogwalkbuddy.data.repo.ProfileRepository
import io.github.greavemrsvg.dogwalkbuddy.domain.rating.HeatSensitivity
import io.github.greavemrsvg.dogwalkbuddy.testutil.SyntheticImages
import io.github.greavemrsvg.dogwalkbuddy.ui.a11y.assertAccessibleScreen
import io.github.greavemrsvg.dogwalkbuddy.ui.a11y.assertTouchTargetIsAtLeast
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.DogWalkBuddyTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.ByteArrayInputStream
import java.io.File

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
// A tall window, so the whole form is on screen and every control has real touch bounds.
@Config(qualifiers = "w411dp-h1500dp-xxhdpi")
class ProfileScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var filesDir: File
    private lateinit var databaseFile: File
    private lateinit var database: AppDatabase
    private lateinit var repository: ProfileRepository
    private var counter = 0
    private var viewModel by mutableStateOf<ProfileViewModel?>(null)

    @Before
    fun setUp() {
        filesDir = temporaryFolder.newFolder("files")
        databaseFile = File(temporaryFolder.newFolder("db"), "test.db")
        openDatabaseAndViewModel()
        composeRule.setContent {
            DogWalkBuddyTheme {
                viewModel?.let { current -> key(current) { ProfileRoute(viewModel = current) } }
            }
        }
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun openDatabaseAndViewModel() {
        database =
            Room
                .databaseBuilder(context, AppDatabase::class.java, databaseFile.path)
                .setQueryExecutor(Runnable::run)
                .setTransactionExecutor(Runnable::run)
                .build()
        val store = PhotoStore(filesDir, Dispatchers.Unconfined) { "00000000-0000-0000-0000-00000000000${counter++}" }
        repository = ProfileRepository(database.dogProfileDao(), store)
        viewModel = ProfileViewModel(repository)
    }

    /** Closes and reopens the database and shows a fresh screen, like the next app launch. */
    private fun relaunch() {
        composeRule.runOnUiThread {
            database.close()
            openDatabaseAndViewModel()
        }
        composeRule.waitForIdle()
    }

    private fun pickPhoto(size: Int = 64) {
        composeRule.runOnUiThread { viewModel!!.onPhotoPicked { ByteArrayInputStream(SyntheticImages.jpeg(size, size)) } }
        composeRule.waitForIdle()
    }

    private fun typeName(text: String) = composeRule.onAllNodes(hasSetTextAction())[0].performTextInput(text)

    private fun typeBreed(text: String) = composeRule.onAllNodes(hasSetTextAction())[1].performTextInput(text)

    private fun save() = composeRule.onNodeWithText("Save").performClick()

    private fun photoFiles() = File(filesDir, "profile").list().orEmpty().toList()

    @Test
    fun AC41_saving_name_breed_photo_and_sensitivity_shows_the_same_profile_on_the_next_launch() {
        typeName("Biscuit")
        typeBreed("Cockapoo")
        composeRule.onNodeWithText("Sensitive").performClick()
        pickPhoto()
        save()
        composeRule.onNodeWithText("Profile saved").assertIsDisplayed()

        relaunch()

        composeRule.onAllNodes(hasSetTextAction())[0].assert(hasText("Biscuit"))
        composeRule.onAllNodes(hasSetTextAction())[1].assert(hasText("Cockapoo"))
        composeRule.onNodeWithText("Sensitive").assertIsSelected()
        composeRule.onNodeWithContentDescription("Photo of your dog").assertIsDisplayed()
    }

    @Test
    fun AC41_heat_sensitivity_defaults_to_normal_when_not_changed() {
        composeRule.onNodeWithText("Normal").assertIsSelected()
        typeName("Biscuit")
        save()
        composeRule.onNodeWithText("Profile saved").assertIsDisplayed()

        relaunch()

        composeRule.onNodeWithText("Normal").assertIsSelected()
    }

    @Test
    fun AC42_an_empty_name_is_refused_with_a_message_naming_the_field_and_the_limit() {
        save()

        composeRule.onNodeWithText("Name is required. Enter 1 to 30 characters.").assertIsDisplayed()
        assertEquals(null, runBlocking { repository.observeProfile().first() })
    }

    @Test
    fun AC42_a_name_over_30_characters_is_refused_with_a_message_naming_the_limit() {
        typeName("a".repeat(31))
        save()

        composeRule.onNodeWithText("Name is too long. Use 30 characters or fewer.").assertIsDisplayed()
        assertEquals(null, runBlocking { repository.observeProfile().first() })
    }

    @Test
    fun AC42_a_breed_over_50_characters_is_refused_with_a_message_naming_the_limit() {
        typeName("Biscuit")
        typeBreed("b".repeat(51))
        save()

        composeRule.onNodeWithText("Breed is too long. Use 50 characters or fewer.").assertIsDisplayed()
        assertEquals(null, runBlocking { repository.observeProfile().first() })
    }

    @Test
    fun AC42_breed_and_photo_may_be_left_empty() {
        typeName("Biscuit")
        save()

        composeRule.onNodeWithText("Profile saved").assertIsDisplayed()
        assertEquals("Biscuit", runBlocking { repository.observeProfile().first() }!!.name)
    }

    @Test
    fun AC43_the_photo_can_be_replaced_and_only_the_new_copy_is_kept() {
        typeName("Biscuit")
        pickPhoto()
        save()
        val first = photoFiles()

        pickPhoto(32)
        save()
        relaunch()

        composeRule.onNodeWithContentDescription("Photo of your dog").assertIsDisplayed()
        assertEquals(1, photoFiles().size)
        assertTrue(photoFiles() != first)
    }

    @Test
    fun AC43_the_photo_can_be_removed_and_it_is_gone_after_a_relaunch() {
        typeName("Biscuit")
        pickPhoto()
        save()
        relaunch()

        composeRule.onNodeWithText("Remove photo").performClick()
        composeRule.onNodeWithText("No photo").assertIsDisplayed()
        save()
        relaunch()

        composeRule.onNodeWithText("No photo").assertIsDisplayed()
        assertTrue(photoFiles().isEmpty())
    }

    @Test
    fun AC41_a_photo_that_is_not_an_image_shows_a_message() {
        composeRule.runOnUiThread { viewModel!!.onPhotoPicked { ByteArrayInputStream("not an image".toByteArray()) } }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("That file isn't a photo the app can use. Choose a different one.").assertIsDisplayed()
    }

    @Test
    fun AC50_every_control_has_a_spoken_label_on_the_empty_form() {
        composeRule.assertAccessibleScreen()
    }

    @Test
    fun AC50_every_control_has_a_spoken_label_with_a_photo_and_validation_errors() {
        typeBreed("b".repeat(51))
        pickPhoto()
        save()

        composeRule.assertAccessibleScreen()
        composeRule.onNodeWithText("Remove photo").assertIsDisplayed()
    }

    @Test
    fun AC50_the_photo_the_sensitivity_choices_and_the_errors_are_announced() {
        pickPhoto()
        save()

        composeRule.onNodeWithContentDescription("Photo of your dog").assertIsDisplayed()
        composeRule.onNodeWithText("Normal").assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
        composeRule.onNodeWithText("Sensitive").assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
        composeRule.onNodeWithText("Heat sensitivity").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        composeRule.onAllNodes(hasSetTextAction())[0].assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Error))
    }

    @Test
    fun AC50_a_refused_save_moves_focus_to_the_first_field_in_error() {
        save()

        composeRule.onAllNodes(hasSetTextAction())[0].assertIsFocused()
    }

    @Test
    fun AC50_a_refused_save_with_only_a_breed_error_moves_focus_to_the_breed_field() {
        typeName("Biscuit")
        typeBreed("b".repeat(51))
        save()

        composeRule.onAllNodes(hasSetTextAction())[1].assertIsFocused()
    }

    @Test
    fun AC50_a_second_refused_save_moves_focus_back_to_the_field_in_error() {
        save()
        composeRule.onAllNodes(hasSetTextAction())[1].performClick()
        composeRule.onAllNodes(hasSetTextAction())[0].assertIsNotFocused()

        save()

        composeRule.onAllNodes(hasSetTextAction())[0].assertIsFocused()
    }

    @Test
    fun AC50_a_refused_photo_is_announced_as_a_polite_live_region() {
        composeRule.runOnUiThread { viewModel!!.onPhotoPicked { ByteArrayInputStream("not an image".toByteArray()) } }
        composeRule.waitForIdle()

        composeRule
            .onNodeWithText("That file isn't a photo the app can use. Choose a different one.")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
    }

    @Test
    fun AC50_a_successful_save_is_announced_as_a_polite_live_region() {
        typeName("Biscuit")
        save()

        composeRule
            .onNodeWithText("Profile saved")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
    }

    @Test
    fun AC41_a_photo_over_the_size_limit_names_the_limits_from_the_photo_store() {
        composeRule.runOnUiThread { viewModel!!.onPhotoPicked { ByteArrayInputStream(SyntheticImages.png(8001, 1)) } }
        composeRule.waitForIdle()

        composeRule
            .onNodeWithText("That photo is too large. Choose one under 20 MB and 8000 pixels on each side.")
            .assertIsDisplayed()
    }

    @Test
    fun AC41_the_sensitivity_hint_only_says_what_the_setting_changes() {
        composeRule
            .onNodeWithText(
                "Sensitive makes the heat and hot-pavement warnings start 4°C sooner. Rain, wind and cold stay the same.",
            ).assertIsDisplayed()
    }

    @Test
    fun AC51_the_controls_are_at_least_48dp_high_and_save_is_larger() {
        composeRule.assertAccessibleScreen()
        composeRule.onNodeWithText("Choose photo").assertTouchTargetIsAtLeast(composeRule, 48.dp, 48.dp)
        composeRule.onNodeWithText("Save").assertTouchTargetIsAtLeast(composeRule, 48.dp, 48.dp)
        composeRule.onNodeWithText("Normal").assertTouchTargetIsAtLeast(composeRule, 48.dp, 48.dp)
        composeRule.onNodeWithText("Privacy & data").assertTouchTargetIsAtLeast(composeRule, 48.dp, 48.dp)
        composeRule.onNodeWithText("About the advice").assertTouchTargetIsAtLeast(composeRule, 48.dp, 48.dp)
    }

    @Test
    fun UX_AC52_tapping_a_radio_card_saves_that_sensitivity_end_to_end() {
        typeName("Biscuit")
        composeRule.onNodeWithText("Sensitive").performClick()
        composeRule.onNodeWithText("Sensitive").assertIsSelected()
        composeRule.onNodeWithText("Normal").assertIsNotSelected()
        save()
        composeRule.onNodeWithText("Profile saved").assertIsDisplayed()
        assertEquals(HeatSensitivity.SENSITIVE, runBlocking { repository.observeProfile().first() }!!.heatSensitivity)

        composeRule.onNodeWithText("Normal").performClick()
        composeRule.onNodeWithText("Normal").assertIsSelected()
        composeRule.onNodeWithText("Sensitive").assertIsNotSelected()
        save()
        assertEquals(HeatSensitivity.NORMAL, runBlocking { repository.observeProfile().first() }!!.heatSensitivity)
    }

    @Test
    fun UX_AC67_a_photo_that_cannot_be_read_shows_the_exact_message_end_to_end() {
        composeRule.runOnUiThread { viewModel!!.onPhotoPicked { null } }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Couldn't read that photo. Try another one.").assertIsDisplayed()
        assertTrue(photoFiles().isEmpty())
    }

    @Test
    fun UX_AC67_a_file_that_is_not_an_image_shows_the_exact_message_end_to_end() {
        composeRule.runOnUiThread { viewModel!!.onPhotoPicked { ByteArrayInputStream("not an image".toByteArray()) } }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("That file isn't a photo the app can use. Choose a different one.").assertIsDisplayed()
    }
}
