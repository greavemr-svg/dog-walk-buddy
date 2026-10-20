package io.github.greavemrsvg.dogwalkbuddy.ui.a11y

import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val MinTouchTarget = 48.dp

/**
 * Automated accessibility checks for one screen (AC50, AC51): every control has a spoken label, and every
 * interactive control is at least 48 x 48 dp.
 */
fun ComposeContentTestRule.assertAccessibleScreen() {
    val interactive = onAllNodes(hasClickAction() or hasSetTextAction())
    val nodes = interactive.fetchSemanticsNodes()
    check(nodes.isNotEmpty()) { "The screen has no interactive controls to check" }
    nodes.indices.forEach { index ->
        val node = nodes[index]
        check(node.hasSpokenLabel()) { "Interactive node ${node.id} has no content description or text" }
        val interaction: SemanticsNodeInteraction = interactive[index]
        interaction.assertTouchTargetIsAtLeast(this, MinTouchTarget, MinTouchTarget)
    }
}

/** Asserts the node's touch bounds, which include Material's minimum interactive padding, are at least this big. */
fun SemanticsNodeInteraction.assertTouchTargetIsAtLeast(
    rule: ComposeContentTestRule,
    minWidth: Dp,
    minHeight: Dp,
): SemanticsNodeInteraction {
    val bounds = fetchSemanticsNode().touchBoundsInRoot
    with(rule.density) {
        check(bounds.width.toDp() >= minWidth) { "Touch width ${bounds.width.toDp()} is below $minWidth" }
        check(bounds.height.toDp() >= minHeight) { "Touch height ${bounds.height.toDp()} is below $minHeight" }
    }
    return this
}

private fun SemanticsNode.hasSpokenLabel(): Boolean {
    val description = config.getOrNull(SemanticsProperties.ContentDescription).orEmpty()
    val text = config.getOrNull(SemanticsProperties.Text).orEmpty()
    return description.any { it.isNotBlank() } || text.any { it.text.isNotBlank() }
}
