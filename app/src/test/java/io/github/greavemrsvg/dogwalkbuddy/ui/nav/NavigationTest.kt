package io.github.greavemrsvg.dogwalkbuddy.ui.nav

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.greavemrsvg.dogwalkbuddy.MainActivity
import io.github.greavemrsvg.dogwalkbuddy.ui.a11y.assertAccessibleScreen
import io.github.greavemrsvg.dogwalkbuddy.ui.a11y.assertTouchTargetIsAtLeast
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w411dp-h1500dp-xxhdpi")
class NavigationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private fun systemBack() = composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }

    @Test
    fun AC41_home_opens_the_dog_profile_and_back_returns_home() {
        composeRule.onNodeWithTag(NAV_DOG_TAG).performClick()
        composeRule.onNodeWithText("Save").assertIsDisplayed()
        composeRule.onNodeWithTag(NAV_DOG_TAG).assertIsSelected()
        composeRule.onNodeWithTag(NAV_DOG_TAG).assertTouchTargetIsAtLeast(composeRule, 48.dp, 48.dp)

        systemBack()

        composeRule.onNodeWithTag(NAV_TODAY_TAG).assertIsSelected()
        composeRule.onNodeWithText("Good time to walk?").assertIsDisplayed()
    }

    @Test
    fun AC35_home_opens_the_walk_history_and_back_returns_home() {
        composeRule.onNodeWithTag(NAV_WALKS_TAG).performClick()
        composeRule.onNodeWithText("This week").assertIsDisplayed()

        systemBack()

        composeRule.onNodeWithTag(NAV_TODAY_TAG).assertIsSelected()
        composeRule.onNodeWithTag(NAV_WALKS_TAG).assertIsNotSelected()
        composeRule.onNodeWithText("Good time to walk?").assertIsDisplayed()
        assertFalse(composeRule.activity.isFinishing)
    }

    @Test
    fun AC51_the_home_link_to_the_profile_is_labelled_and_at_least_48dp() {
        composeRule.assertAccessibleScreen()
    }
}
