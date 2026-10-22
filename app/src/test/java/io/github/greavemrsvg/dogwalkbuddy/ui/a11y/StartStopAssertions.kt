package io.github.greavemrsvg.dogwalkbuddy.ui.a11y

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp

/** AC51: Start walk and Stop are at least 72 dp high and fill the width of the screen inside its 16 dp margins. The size is used, not the visible bounds, so a control scrolled out of view is still measured. */
fun SemanticsNodeInteraction.assertIsLargeFullWidthControl(rule: ComposeContentTestRule) {
    val node = fetchSemanticsNode()
    val root = rule.onRoot().fetchSemanticsNode()
    with(rule.density) {
        val height = node.size.height.toDp()
        check(height >= 72.dp) { "Control is $height high, below 72.dp" }
        val width = node.size.width.toDp()
        val screenWidth = root.size.width.toDp()
        check(width >= screenWidth - 32.dp) { "Control is $width wide on a $screenWidth wide screen, so it is not full width" }
    }
}
