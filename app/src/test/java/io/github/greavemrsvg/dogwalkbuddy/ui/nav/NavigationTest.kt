package io.github.greavemrsvg.dogwalkbuddy.ui.nav

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.greavemrsvg.dogwalkbuddy.MainActivity
import io.github.greavemrsvg.dogwalkbuddy.ui.a11y.assertAccessibleScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w411dp-h1500dp-xxhdpi")
class NavigationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun AC41_home_opens_the_dog_profile_and_back_returns_home() {
        composeRule.onNodeWithText("Dog profile").performClick()
        composeRule.onNodeWithText("Save profile").assertIsDisplayed()

        composeRule.onNodeWithText("Back").performClick()

        composeRule.onNodeWithText("Dog Walk Buddy").assertIsDisplayed()
    }

    @Test
    fun AC51_the_home_link_to_the_profile_is_labelled_and_at_least_48dp() {
        composeRule.assertAccessibleScreen()
    }
}
