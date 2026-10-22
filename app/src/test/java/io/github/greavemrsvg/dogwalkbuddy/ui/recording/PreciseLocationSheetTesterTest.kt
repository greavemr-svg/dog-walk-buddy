package io.github.greavemrsvg.dogwalkbuddy.ui.recording

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.greavemrsvg.dogwalkbuddy.ui.a11y.assertAccessibleScreen
import io.github.greavemrsvg.dogwalkbuddy.ui.a11y.assertHeadingsMarked
import io.github.greavemrsvg.dogwalkbuddy.ui.a11y.assertHiddenFromAccessibility
import io.github.greavemrsvg.dogwalkbuddy.ui.a11y.assertTouchTargetIsAtLeast
import io.github.greavemrsvg.dogwalkbuddy.ui.common.PERMISSION_ILLUSTRATION_TAG
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.DogWalkBuddyTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Tester checks for the precise-location sheet (AC32, AC58) in light and in dark: the exact heading, a hidden
 * illustration, the action at least 48 dp, and each control calls only its own callback.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class PreciseLocationSheetTesterTest {
    @get:Rule
    val composeRule = createComposeRule()

    private var starts = 0
    private var grants = 0
    private var settingsOpens = 0

    private fun show(
        state: StartWalkUiState,
        night: Boolean,
    ) {
        if (night) RuntimeEnvironment.setQualifiers("+night")
        composeRule.setContent {
            DogWalkBuddyTheme {
                StartWalkContent(
                    state = state,
                    onStart = { starts++ },
                    onGrantPrecise = { grants++ },
                    onOpenSettings = { settingsOpens++ },
                )
            }
        }
    }

    private fun assertSheet(
        state: StartWalkUiState,
        action: String,
        night: Boolean,
    ) {
        show(state, night)

        composeRule.onNodeWithText("Recording a walk needs precise location.").assertIsDisplayed()
        composeRule.assertHeadingsMarked("Recording a walk needs precise location.")
        composeRule.assertHiddenFromAccessibility(PERMISSION_ILLUSTRATION_TAG)
        composeRule.onNodeWithText(action).assertIsDisplayed().assertTouchTargetIsAtLeast(composeRule, 48.dp, 48.dp)
        composeRule.assertAccessibleScreen()
        composeRule.onNodeWithText(action).performClick()
        assertEquals(0, starts)
    }

    @Test
    fun UX_AC32_UX_AC58_the_allow_sheet_is_accessible_in_light() {
        assertSheet(StartWalkUiState.NeedsPreciseLocation, "Allow precise location", night = false)
        assertEquals(1, grants)
        assertEquals(0, settingsOpens)
    }

    @Test
    fun UX_AC32_UX_AC58_the_allow_sheet_is_accessible_in_dark() {
        assertSheet(StartWalkUiState.NeedsPreciseLocation, "Allow precise location", night = true)
        assertEquals(1, grants)
    }

    @Test
    fun UX_AC32_UX_AC58_the_open_settings_sheet_is_accessible_in_light() {
        assertSheet(StartWalkUiState.PreciseLocationBlocked, "Open settings", night = false)
        assertEquals(1, settingsOpens)
        assertEquals(0, grants)
        composeRule.onNodeWithText("Allow precise location").assertDoesNotExist()
    }

    @Test
    fun UX_AC32_UX_AC58_the_open_settings_sheet_is_accessible_in_dark() {
        assertSheet(StartWalkUiState.PreciseLocationBlocked, "Open settings", night = true)
        assertEquals(1, settingsOpens)
    }
}
