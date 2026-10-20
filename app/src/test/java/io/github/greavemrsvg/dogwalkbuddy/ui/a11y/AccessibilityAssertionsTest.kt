package io.github.greavemrsvg.dogwalkbuddy.ui.a11y

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

/** The test helpers must be able to fail, or a passing AC50 or AC52 test would prove nothing. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AccessibilityAssertionsTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun AC52_the_truncation_check_fails_for_text_cut_off_by_an_ellipsis() {
        composeRule.setContent {
            WithFontScale {
                Text(
                    "A long line that cannot fit on one line",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.width(80.dp),
                )
            }
        }

        assertThrows(IllegalStateException::class.java) { composeRule.assertNoTextTruncation() }
    }

    @Test
    fun AC52_the_truncation_check_passes_for_text_that_wraps_in_full() {
        composeRule.setContent {
            WithFontScale {
                Column(Modifier.width(80.dp)) { Text("A long line that wraps over several lines") }
            }
        }

        composeRule.assertNoTextTruncation()
    }

    @Test
    fun AC52_the_truncation_check_fails_when_the_screen_has_no_text() {
        composeRule.setContent { Column {} }

        assertThrows(IllegalStateException::class.java) { composeRule.assertNoTextTruncation() }
    }

    @Test
    fun AC50_the_announcement_check_fails_when_the_reason_comes_before_the_rating() {
        composeRule.setContent {
            Text("x", modifier = Modifier.testTag("node").semantics { contentDescription = "Windy. Caution." })
        }

        assertThrows(AssertionError::class.java) {
            composeRule.onNodeWithTag("node").assertAnnouncesRatingThenReason("Caution", "Windy")
        }
        composeRule.onNodeWithText("x").assertExists()
    }
}
