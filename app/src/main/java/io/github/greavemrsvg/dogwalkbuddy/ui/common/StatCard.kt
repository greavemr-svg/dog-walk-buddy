package io.github.greavemrsvg.dogwalkbuddy.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.FieldKit
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.FieldKitShapes
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.FieldKitSpacing

/** The colours of a [StatCard]: Recording uses the recording palette, Walk detail uses `surface`. */
data class StatCardColors(
    val container: Color,
    val caption: Color,
    val value: Color,
)

/** The smallest the value may shrink to at large font sizes, so it is never smaller than the MVP's 200 % text. */
private val StatValueMinSize = 24.sp
private val StatCardVerticalPadding = 16.dp
private val StatCardGap = 4.dp

/**
 * A caption over a large single-line value, read by accessibility services as one node (AC34). The value shrinks
 * to fit rather than truncate at large font sizes (AC38).
 */
@Composable
fun StatCard(
    caption: String,
    value: AnnotatedString,
    colors: StatCardColors,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .semantics(mergeDescendants = true) {}
                .background(colors.container, FieldKitShapes.card)
                .padding(horizontal = FieldKitSpacing.cardPadding, vertical = StatCardVerticalPadding),
        verticalArrangement = Arrangement.spacedBy(StatCardGap),
    ) {
        Text(text = caption, style = FieldKit.typography.label, color = colors.caption)
        val style = FieldKit.typography.statLarge
        BasicText(
            text = value,
            style = style.copy(color = colors.value),
            maxLines = 1,
            autoSize = TextAutoSize.StepBased(minFontSize = StatValueMinSize, maxFontSize = style.fontSize),
        )
    }
}
