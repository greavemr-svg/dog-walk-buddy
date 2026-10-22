package io.github.greavemrsvg.dogwalkbuddy.ui.check

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.DogWalkBuddyTheme
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.FieldKit
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.FieldKitSizes
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Controls for the AC24 clipping regression check in [TodayFootnoteClippingTest]. Both render a copy of the footnote
 * link button inside the test, so no application code changes: with the default pill shape (the code before the fix)
 * the left-strip ink comparison must report clipping, and with a rectangle shape it must not. This proves the
 * measurement can tell a clipped link from an unclipped one rather than passing whatever is drawn.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class TodayFootnoteClippingControlTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val line = "Using normal heat sensitivity. Set up your dog's profile to change this."

    @Test
    fun UX_AC24_control_the_ink_check_detects_a_pill_shaped_link_clipping_its_first_letters() {
        val (linkInk, referenceInk) = measure(shape = null)
        check(referenceInk > 0) { "The reference text drew nothing in its left strip" }
        check(linkInk < referenceInk * 0.95) {
            "The ink check did not detect clipping: $linkInk inked pixels in the pill link, reference $referenceInk"
        }
    }

    @Test
    fun UX_AC24_control_the_ink_check_accepts_a_rectangle_link_that_draws_in_full() {
        val (linkInk, referenceInk) = measure(shape = RectangleShape)
        check(referenceInk > 0) { "The reference text drew nothing in its left strip" }
        check(linkInk >= referenceInk * 0.95) {
            "A rectangle link was reported as clipped: $linkInk inked pixels, reference $referenceInk"
        }
    }

    /** Returns the ink in the left strip of the link and of the plain reference text. A null [shape] is the default pill shape. */
    private fun measure(shape: Shape?): Pair<Int, Int> {
        composeRule.setContent {
            DogWalkBuddyTheme {
                val colors = FieldKit.colors
                val small = FieldKit.typography.body.copy(fontSize = 13.sp, lineHeight = 18.sp)
                Column(Modifier.fillMaxWidth().background(colors.background)) {
                    // A copy of TodayFootnote's column padding and FootnoteButton, with the shape under test.
                    Column(Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                        TextButton(
                            onClick = {},
                            modifier = Modifier.heightIn(min = FieldKitSizes.minTouchTarget),
                            shape = shape ?: ButtonDefaults.textShape,
                            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp),
                            colors = ButtonDefaults.textButtonColors(contentColor = colors.primary),
                        ) {
                            Text(line, style = small, modifier = Modifier.testTag(LINK))
                        }
                    }
                    Box(Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                        Text(line, style = small, color = colors.primary, modifier = Modifier.testTag(REFERENCE))
                    }
                }
            }
        }
        composeRule.waitForIdle()

        val image = composeRule.onRoot().captureToImage().toPixelMap()
        val link = composeRule.onNode(hasTestTag(LINK), useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val reference = composeRule.onNode(hasTestTag(REFERENCE)).fetchSemanticsNode().boundsInRoot
        val strip = with(composeRule.density) { 8.dp.toPx() }.roundToInt()
        val background = image[image.width - 1, image.height - 1]
        return image.inkIn(link.left, link.top, strip, link.height, background) to
            image.inkIn(reference.left, reference.top, strip, reference.height, background)
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
        const val LINK = "link"
        const val REFERENCE = "reference"
        const val INK_THRESHOLD = 0.3f
    }
}
