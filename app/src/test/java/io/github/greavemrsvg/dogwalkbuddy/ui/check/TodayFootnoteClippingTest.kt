package io.github.greavemrsvg.dogwalkbuddy.ui.check

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.DogWalkBuddyTheme
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.FieldKit
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Regression for the no-profile sensitivity link (AC24): it wraps onto two lines, and every glyph at the start of a
 * line must be drawn in full. The link is compared with the same text drawn as plain, unclipped text at the same
 * width; the ink in the left strip of the link must match the reference.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class TodayFootnoteClippingTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val line = "Using normal heat sensitivity. Set up your dog's profile to change this."

    @Test
    fun UX_AC24_the_no_profile_link_draws_the_first_letter_of_every_line_in_full() {
        composeRule.setContent {
            DogWalkBuddyTheme {
                val colors = FieldKit.colors
                Column(Modifier.fillMaxWidth().background(colors.background)) {
                    Box(Modifier.testTag(FOOTNOTE)) {
                        TodayFootnote(
                            rated = FootnoteRatedLines(usingDefaultSensitivity = true, sensitiveDogName = null),
                            onOpenAbout = {},
                            onOpenProfile = {},
                        )
                    }
                    // The same text, style, colour and width as the link, drawn with nothing around it.
                    Box(Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                        Text(
                            line,
                            style = FieldKit.typography.body.copy(fontSize = 13.sp, lineHeight = 18.sp),
                            color = colors.primary,
                            modifier = Modifier.testTag(REFERENCE),
                        )
                    }
                }
            }
        }
        composeRule.waitForIdle()

        val image = composeRule.onRoot().captureToImage().toPixelMap()
        val link =
            composeRule
                .onNode(hasText(line) and hasAnyAncestor(hasTestTag(FOOTNOTE)), useUnmergedTree = true)
                .fetchSemanticsNode()
                .boundsInRoot
        val reference = composeRule.onNode(hasTestTag(REFERENCE)).fetchSemanticsNode().boundsInRoot
        val strip = with(composeRule.density) { 8.dp.toPx() }.roundToInt()
        val background = image[image.width - 1, image.height - 1]

        val referenceInk = image.inkIn(reference.left, reference.top, strip, reference.height, background)
        val linkInk = image.inkIn(link.left, link.top, strip, link.height, background)
        check(referenceInk > 0) { "The reference text drew nothing in its left strip" }
        check(linkInk >= referenceInk * 0.95) {
            "The link's first letters are clipped: $linkInk inked pixels in its left strip, expected $referenceInk"
        }
    }

    private fun PixelMap.inkIn(
        left: Float,
        top: Float,
        width: Int,
        height: Float,
        background: Color,
    ): Int {
        val x0 = left.roundToInt()
        val y0 = top.roundToInt()
        var count = 0
        for (y in y0 until (y0 + height.roundToInt()).coerceAtMost(this.height)) {
            for (x in x0 until (x0 + width).coerceAtMost(this.width)) {
                if (this[x, y].differsFrom(background)) count++
            }
        }
        return count
    }

    private fun Color.differsFrom(other: Color) = abs(red - other.red) + abs(green - other.green) + abs(blue - other.blue) > INK_THRESHOLD

    private companion object {
        const val FOOTNOTE = "footnote"
        const val REFERENCE = "reference"
        const val INK_THRESHOLD = 0.3f
    }
}
