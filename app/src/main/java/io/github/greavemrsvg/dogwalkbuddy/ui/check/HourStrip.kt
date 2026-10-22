package io.github.greavemrsvg.dogwalkbuddy.ui.check

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.greavemrsvg.dogwalkbuddy.domain.rating.HourRating
import io.github.greavemrsvg.dogwalkbuddy.domain.rating.Rating
import io.github.greavemrsvg.dogwalkbuddy.domain.window.BestWindow
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.FieldKit
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.FieldKitSizes
import kotlin.math.ceil

/** Test tag of the 12-hour strip (AC23). */
const val HOUR_STRIP_TAG = "hour_strip"

/** Test tag prefix of the strip's bars, followed by the column index from 0 (AC23). */
const val STRIP_BAR_TAG_PREFIX = "strip_bar_"

/** Test tag prefix of the strip's hour labels, followed by the column index from 0 (AC23). */
const val STRIP_LABEL_TAG_PREFIX = "strip_label_"

/** Test tag prefix of the marker line's segments above the bars of the best window, followed by the column index. */
const val STRIP_MARKER_TAG_PREFIX = "strip_marker_"

/** Test-only: the rating a strip bar shows, so tests can check heights against ratings (AC23, AC61). */
val BarRating = SemanticsPropertyKey<Rating>("BarRating")
private var SemanticsPropertyReceiver.barRating by BarRating

/** The strip always has this many columns; fewer hours leave the last columns empty (design, 12-hour strip). */
const val STRIP_COLUMNS = 12

private val ColumnGap = 4.dp
private val MarkerHeight = 3.dp
private val BarAreaHeight = 46.dp
private val BarShape = RoundedCornerShape(6.dp)
private val RowGap = 4.dp

// Every label is shown up to this font scale (AC23 needs all 12 at the default size); above it labels are thinned.
private const val ALL_LABELS_MAX_FONT_SCALE = 1.3f
private const val MIN_THINNED_STEP = 2

/** Bar height for [rating]: 44, 28 or 14 dp, so ratings differ by height as well as colour (AC23, AC61). */
fun barHeight(rating: Rating): Dp =
    when (rating) {
        Rating.GO -> FieldKitSizes.barGo
        Rating.CAUTION -> FieldKitSizes.barCaution
        Rating.AVOID -> FieldKitSizes.barAvoid
    }

/** The columns whose hour starts inside the window, or an empty range when there is no good time (AC23). */
fun markedColumns(
    hours: List<HourRating>,
    window: BestWindow,
): IntRange {
    if (window !is BestWindow.Window) return IntRange.EMPTY
    val inside = hours.take(STRIP_COLUMNS).indices.filter { hours[it].startUtc >= window.startUtc && hours[it].startUtc < window.endUtc }
    return if (inside.isEmpty()) IntRange.EMPTY else inside.first()..inside.last()
}

/**
 * The 12-hour strip (AC23, AC61): a marker line over the best window, one bottom-aligned bar per hour in the
 * rating's bar colour and height, and an hour label under each column. It is decorative, so the whole strip is
 * hidden from accessibility services; the window text carries the meaning.
 */
@Composable
fun HourStrip(
    hours: List<HourRating>,
    window: BestWindow,
    labels: HourLabelFormat,
    modifier: Modifier = Modifier,
) {
    val shown = hours.take(STRIP_COLUMNS)
    val texts = remember(shown, labels) { shown.map { labels.format(it.startUtc) } }
    BoxWithConstraints(modifier = modifier.fillMaxWidth().testTag(HOUR_STRIP_TAG).semantics { hideFromAccessibility() }) {
        val pitch = (maxWidth + ColumnGap) / STRIP_COLUMNS
        val step = labelStep(texts, pitch)
        Column(verticalArrangement = Arrangement.spacedBy(RowGap)) {
            val marked = markedColumns(shown, window)
            Columns(Modifier.height(MarkerHeight), Modifier.fillMaxHeight(), Alignment.Center) { index ->
                if (index in marked) MarkerSegment(index, marked)
            }
            Columns(Modifier.height(BarAreaHeight), Modifier.fillMaxHeight(), Alignment.BottomCenter) { index ->
                shown.getOrNull(index)?.let { Bar(index, it.rating) }
            }
            Columns(Modifier, Modifier, Alignment.TopCenter) { index ->
                texts.getOrNull(index)?.takeIf { index % step == 0 }?.let { HourLabel(index, it) }
            }
        }
    }
}

/** 1 (every label) at normal sizes, otherwise the smallest step at which the widest label clears its neighbours. */
@Composable
private fun labelStep(
    texts: List<String>,
    pitch: Dp,
): Int {
    val density = LocalDensity.current
    if (density.fontScale <= ALL_LABELS_MAX_FONT_SCALE || texts.isEmpty()) return 1
    val measurer = rememberTextMeasurer()
    val style = FieldKit.typography.axisLabel
    val widest = texts.maxOf { measurer.measure(it, style).size.width }
    val needed = ceil((widest + with(density) { ColumnGap.toPx() }) / with(density) { pitch.toPx() }).toInt()
    return maxOf(MIN_THINNED_STEP, needed)
}

/**
 * One segment per marked column, laid out like the bars so it lines up with them exactly. Each segment but the last
 * also covers the gap to its right, so the marker reads as one line.
 */
@Composable
private fun MarkerSegment(
    index: Int,
    columns: IntRange,
) {
    val color = FieldKit.colors.onSurface
    Box(
        Modifier
            .fillMaxSize()
            .testTag(STRIP_MARKER_TAG_PREFIX + index)
            .drawBehind {
                val extra = if (index < columns.last) ColumnGap.toPx() else 0f
                drawRect(color, size = Size(size.width + extra, size.height))
            },
    )
}

@Composable
private fun Columns(
    modifier: Modifier,
    cellModifier: Modifier,
    alignment: Alignment,
    cell: @Composable (index: Int) -> Unit,
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ColumnGap)) {
        repeat(STRIP_COLUMNS) { index ->
            Box(modifier = Modifier.weight(1f).then(cellModifier), contentAlignment = alignment) { cell(index) }
        }
    }
}

@Composable
private fun Bar(
    index: Int,
    rating: Rating,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(barHeight(rating))
            .testTag(STRIP_BAR_TAG_PREFIX + index)
            .semantics { barRating = rating }
            .background(FieldKit.ratingPalettes.of(rating).bar, BarShape),
    )
}

@Composable
private fun HourLabel(
    index: Int,
    text: String,
) {
    // Measured without the column's width limit, so a label is never cut or wrapped (AC23).
    Text(
        text = text,
        style = FieldKit.typography.axisLabel,
        color = FieldKit.colors.onSurfaceVariant,
        softWrap = false,
        maxLines = 1,
        modifier = Modifier.wrapContentWidth(unbounded = true).testTag(STRIP_LABEL_TAG_PREFIX + index),
    )
}
