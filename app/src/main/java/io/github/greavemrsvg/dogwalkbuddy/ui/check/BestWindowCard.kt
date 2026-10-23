package io.github.greavemrsvg.dogwalkbuddy.ui.check

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import io.github.greavemrsvg.dogwalkbuddy.R
import io.github.greavemrsvg.dogwalkbuddy.domain.rating.HourRating
import io.github.greavemrsvg.dogwalkbuddy.domain.rating.Rating
import io.github.greavemrsvg.dogwalkbuddy.domain.window.BestWindow
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.FieldKit
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.FieldKitShapes

/** Test tag of the best-window card (AC22). */
const val BEST_WINDOW_CARD_TAG = "best_window_card"

/** Test tag of the chip with the window's rating word (AC22). */
const val BEST_WINDOW_CHIP_TAG = "best_window_chip"

private val ChipPaddingHorizontal = 10.dp
private val ChipPaddingVertical = 4.dp

/**
 * The best-window card (AC22, AC23): "Best window, next 12 hours" (C2), the "Best available: Caution" label when it
 * applies, the window text exactly as [BestWindowFormatter] gives it, a chip with the window's rating word (none for
 * "No good time in the next 12 hours"), then the 12-hour strip. The texts and the chip are one accessibility node,
 * as in the MVP; the strip is hidden.
 */
@Composable
fun BestWindowCard(
    window: BestWindow,
    text: BestWindowText,
    hours: List<HourRating>,
    hourLabels: HourLabelFormat,
    modifier: Modifier = Modifier,
) {
    val colors = FieldKit.colors
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .testTag(BEST_WINDOW_CARD_TAG)
                .background(colors.surface, FieldKitShapes.bestWindowCard)
                .border(1.dp, colors.outlineVariant, FieldKitShapes.bestWindowCard)
                .padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(R.string.check_window_heading),
                    style = FieldKit.typography.label,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.semantics { heading() },
                )
                text.label?.let { Text(it, style = FieldKit.typography.label, color = colors.onSurfaceVariant) }
                Text(text.text, style = FieldKit.typography.bestWindow, color = colors.onSurface)
            }
            if (window is BestWindow.Window) RatingChip(window.rating)
        }
        HourStrip(hours = hours, window = window, labels = hourLabels)
    }
}

/**
 * The rating word on the rating's container colour. It is drawn rather than set as a text node: the hero card's
 * rating word must stay the only text with that word on Today (MVP tests find it as exactly one node). The word is
 * still spoken, as the chip's description inside the card's merged node.
 */
@Composable
private fun RatingChip(rating: Rating) {
    val palette = FieldKit.ratingPalettes.of(rating)
    val word = stringResource(rating.labelRes())
    val measurer = rememberTextMeasurer()
    val style = FieldKit.typography.label.copy(fontWeight = FontWeight.Bold, color = palette.onContainer)
    val layout = remember(measurer, word, style) { measurer.measure(word, style) }
    val density = LocalDensity.current
    val width = with(density) { layout.size.width.toDp() } + ChipPaddingHorizontal * 2
    val height = with(density) { layout.size.height.toDp() } + ChipPaddingVertical * 2
    Canvas(
        modifier =
            Modifier
                .size(width, height)
                .testTag(BEST_WINDOW_CHIP_TAG)
                .semantics { contentDescription = word },
    ) {
        drawRoundRect(palette.container, cornerRadius = CornerRadius(size.height / 2))
        drawText(layout, topLeft = Offset(ChipPaddingHorizontal.toPx(), ChipPaddingVertical.toPx()))
    }
}
