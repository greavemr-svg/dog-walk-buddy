package io.github.greavemrsvg.dogwalkbuddy.ui.a11y

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
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

/** The largest system font setting (200 %, AC52). */
const val LARGEST_FONT_SCALE = 2.0f

/** Shows [content] with the system font scaled by [scale], as when the user sets the largest font size. */
@Composable
fun WithFontScale(
    scale: Float = LARGEST_FONT_SCALE,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = scale), content = content)
}

/**
 * AC52: no text on the screen is cut off, ellipsized or clipped (scrolling is allowed, so show the screen in a
 * scrolling container). Run it on a screen shown
 * with [WithFontScale]. It also checks that the screen has text to check, so it cannot pass by finding nothing.
 *
 * [allowEllipsisTags] names the text nodes, by test tag, that may instead stay on one line and end in "…" (AC60:
 * only the dog-name nav label). Such a node still fails if it is cut off any other way. The default allows none.
 */
fun ComposeContentTestRule.assertNoTextTruncation(allowEllipsisTags: Set<String> = emptySet()) {
    val truncated = mutableListOf<String>()
    var checked = 0
    // The unmerged tree, because the rating block replaces its children's semantics with one spoken phrase.
    onAllNodes(SemanticsMatcher("has a text layout") { it.hasTextLayout() }, useUnmergedTree = true)
        .fetchSemanticsNodes()
        .forEach { node ->
            val layouts = mutableListOf<TextLayoutResult>()
            node.config
                .getOrNull(SemanticsActions.GetTextLayoutResult)
                ?.action
                ?.invoke(layouts)
            val mayEllipsize = node.config.getOrNull(SemanticsProperties.TestTag) in allowEllipsisTags
            layouts.forEach { layout ->
                val allowed = mayEllipsize && layout.isOneEllipsizedLine()
                if (layout.isTruncated() && !allowed) truncated += layout.layoutInput.text.text
                checked++
            }
        }
    check(truncated.isEmpty()) { "Text is truncated or clipped at font scale $LARGEST_FONT_SCALE: $truncated" }
    check(checked > 0) { "The screen has no text layouts to check" }
}

/**
 * True when some of the text is cut off: more lines than allowed, an ellipsis or clip hiding the end, or a height
 * smaller than the text needs. `hasVisualOverflow` is not used because it also reports short text that is merely
 * narrower than the width it was offered.
 */
private fun TextLayoutResult.isTruncated(): Boolean {
    val lastLine = lineCount - 1
    return multiParagraph.didExceedMaxLines ||
        getLineEnd(lastLine, visibleEnd = true) < layoutInput.text.length ||
        size.height < multiParagraph.height.toInt()
}

/** One line whose end is replaced by "…", and nothing else cut off: the line is as tall as the text needs. */
private fun TextLayoutResult.isOneEllipsizedLine(): Boolean =
    lineCount == 1 && isLineEllipsized(0) && size.height >= multiParagraph.height.toInt()

/**
 * AC58: each of [texts] is on the screen as text marked as a heading for accessibility services, so TalkBack users
 * can jump between sections.
 */
fun ComposeContentTestRule.assertHeadingsMarked(vararg texts: String) {
    check(texts.isNotEmpty()) { "No headings to check" }
    texts.forEach { text ->
        val headings = onAllNodes(hasText(text) and isHeading(), useUnmergedTree = true).fetchSemanticsNodes()
        check(headings.isNotEmpty()) { "\"$text\" is not shown as a heading" }
    }
}

/**
 * AC58: the decorative graphic tagged [tag] is hidden from accessibility services. It is hidden when it or an
 * ancestor is marked hidden, or when neither it nor anything inside it has a spoken label or an action, so
 * TalkBack has nothing to focus or read.
 */
fun ComposeContentTestRule.assertHiddenFromAccessibility(tag: String) {
    val node = onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode()
    check(node.isMarkedHidden() || node.exposesNothing()) { "The node tagged $tag is exposed to accessibility services" }
}

private fun SemanticsNode.isMarkedHidden(): Boolean =
    generateSequence(this) { it.parent }.any { SemanticsProperties.HideFromAccessibility in it.config }

private fun SemanticsNode.exposesNothing(): Boolean =
    !hasSpokenLabel() && SemanticsActions.OnClick !in config && children.all { it.exposesNothing() }

/** AC50: the node is spoken as "<Rating>. <Reason>." (rating first, then the reason). */
fun SemanticsNodeInteraction.assertAnnouncesRatingThenReason(
    rating: String,
    reason: String,
): SemanticsNodeInteraction = assertContentDescriptionEquals("$rating. $reason.")

private fun SemanticsNode.hasTextLayout(): Boolean = config.getOrNull(SemanticsActions.GetTextLayoutResult) != null
