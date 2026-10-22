package io.github.greavemrsvg.dogwalkbuddy.ui.privacy

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
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

/** The Privacy screen (AC46, AC47, AC50, AC51, AC52). */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class PrivacyScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val current = mutableStateOf(PrivacyUiState())
    private var backs = 0
    private var requests = 0
    private var confirms = 0
    private var cancels = 0
    private var dismissals = 0

    private fun show(
        state: PrivacyUiState = PrivacyUiState(),
        fontScale: Float = 1f,
    ) {
        current.value = state
        composeRule.setContent {
            DogWalkBuddyTheme {
                WithFontScale(fontScale) {
                    PrivacyContent(
                        state = current.value,
                        onBack = { backs++ },
                        onRequestDelete = { requests++ },
                        onConfirmDelete = { confirms++ },
                        onCancelDelete = { cancels++ },
                        onDismissDeleteFailure = {
                            dismissals++
                            current.value = current.value.copy(deleteFailed = false)
                        },
                    )
                }
            }
        }
    }

    private fun assertShown(vararg fragments: String) {
        fragments.forEach { composeRule.onNodeWithText(it, substring = true).assertExists() }
    }

    @Test
    fun AC46_it_says_approximate_location_is_rounded_to_about_1_km_and_sent_to_MET_Norway_only_when_a_check_runs() {
        show()

        assertShown(
            "When a check runs, your approximate location (rounded to about 1 km) is sent to MET Norway",
            "Nothing is sent to MET Norway at any other time",
        )
    }

    @Test
    fun AC46_it_says_MET_Norway_also_receives_the_phones_IP_address() {
        show()

        assertShown("MET Norway also receives your phone's IP address")
    }

    @Test
    fun AC46_it_names_OpenFreeMap_with_its_CDN_and_says_it_receives_the_map_area_and_the_IP_address() {
        show()

        assertShown(
            "When you view a walk map, the map provider, OpenFreeMap (served through its CDN provider, Cloudflare), " +
                "receives requests for the map area shown and your phone's IP address",
        )
    }

    @Test
    fun AC46_it_says_routes_profile_and_history_never_leave_the_phone() {
        show()

        assertShown("Your walk routes, your dog's profile and your walk history never leave your phone")
    }

    @Test
    fun AC46_it_says_there_are_no_accounts_analytics_or_tracking() {
        show()

        assertShown("There are no accounts, analytics or tracking.")
    }

    @Test
    fun AC48_it_explains_that_the_data_is_left_out_of_backup_and_device_transfer() {
        show()

        assertShown("left out of Android cloud backup and device-to-device transfer")
    }

    @Test
    fun the_back_control_goes_back() {
        show()

        composeRule.onNodeWithContentDescription("Back").performClick()

        assertEquals(1, backs)
    }

    @Test
    fun AC47_the_delete_control_asks_without_deleting_anything_yet() {
        show()

        composeRule.onNodeWithText("Delete all my data").performScrollTo().performClick()

        assertEquals(1, requests)
        assertEquals(0, confirms)
    }

    @Test
    fun AC47_the_confirmation_names_what_will_be_removed_and_offers_delete_and_cancel() {
        show(PrivacyUiState(confirming = true))

        composeRule.onNodeWithText("Delete all your data?").assertIsDisplayed()
        composeRule.onNodeWithText("This can't be undone.", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Delete everything").performClick()
        assertEquals(1, confirms)
        composeRule.onNodeWithText("Cancel").performClick()
        assertEquals(1, cancels)
    }

    @Test
    fun AC47_no_confirmation_is_shown_until_asked() {
        show()

        assertTrue(composeRule.onAllNodes(hasText("Delete all your data?")).fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun AC47_while_deleting_the_control_is_off_and_the_progress_is_announced() {
        show(PrivacyUiState(deleting = true))

        composeRule.onNodeWithText("Delete all my data").performScrollTo().assertIsNotEnabled()
        composeRule.onNodeWithText("Deleting your data").assertExists()
    }

    @Test
    fun AC47_after_deleting_the_result_is_announced_and_the_control_is_on_again() {
        show(PrivacyUiState(deleted = true))

        composeRule.onNodeWithText("All your data has been deleted from this phone.").assertExists()
        composeRule.onNodeWithText("Delete all my data").performScrollTo().assertIsEnabled()
    }

    @Test
    fun AC47_a_failed_deletion_is_shown_in_a_snackbar_until_dismissed() {
        show(PrivacyUiState(deleteFailed = true))

        composeRule.onNodeWithText("Couldn't delete all your data. Try again.").assertIsDisplayed()
        composeRule.onNodeWithText("OK").performClick()
        composeRule.waitForIdle()

        assertEquals(1, dismissals)
    }

    @Test
    fun AC51_every_control_is_labelled_and_at_least_48dp() {
        show()

        composeRule.assertAccessibleScreen()
    }

    @Test
    fun AC51_the_confirmation_controls_are_labelled_and_at_least_48dp() {
        show(PrivacyUiState(confirming = true))

        composeRule.assertAccessibleScreen()
    }

    @Test
    fun AC52_the_screen_at_the_largest_font_size_is_not_truncated() {
        show(fontScale = 2.0f)

        composeRule.assertNoTextTruncation()
    }
}
