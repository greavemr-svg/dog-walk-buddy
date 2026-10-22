package io.github.greavemrsvg.dogwalkbuddy.ui.history

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.greavemrsvg.dogwalkbuddy.domain.walk.DayDot
import io.github.greavemrsvg.dogwalkbuddy.domain.walk.DayDotState
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.FieldKit
import java.time.format.TextStyle
import java.util.Locale

/** Test tag of the this-week card's dot row. */
const val WEEK_DOTS_TAG = "week_dots"

/** Test tag prefix of each dot, Monday `week_dot_0` to Sunday `week_dot_6`. */
const val WEEK_DOT_TAG_PREFIX = "week_dot_"

/** Test-only: the day a dot shows, so tests can check its state and the today mark (AC41). */
val DayDotKey = SemanticsPropertyKey<DayDot>("DayDot")
private var SemanticsPropertyReceiver.dayDot by DayDotKey

/** The dot colours (design, Walks): light uses the pack's values, dark the swapped roles. */
private data class WeekDotColors(
    val walked: Color,
    val pastNoWalk: Color,
    val futureRing: Color,
    val todayRing: Color,
)

private val LightWalkedDot = Color(0xFF8CC2FF)
private val LightPastNoWalkDot = Color(0xFF2E5C94)
private val DotSize = 26.dp
private val TodayRingWidth = 2.dp
private val FutureRingWidth = 1.5.dp
private val DashLength = 3.dp
private val DotLabelGap = 6.dp

@Composable
private fun weekDotColors(): WeekDotColors {
    val colors = FieldKit.colors
    return if (isSystemInDarkTheme()) {
        WeekDotColors(colors.onPrimary, colors.primaryPressed, colors.summaryCaption, colors.onPrimary)
    } else {
        WeekDotColors(LightWalkedDot, LightPastNoWalkDot, colors.summaryCaption, colors.onPrimary)
    }
}

/**
 * The seven days of the current week, Monday to Sunday, as dots with the day letter below (AC41): walked, past with
 * no walk, or future, with a ring around today. Decorative: the card's spoken summary carries the content, so the row
 * is hidden from accessibility services.
 */
@Composable
fun WeekDots(
    days: List<DayDot>,
    modifier: Modifier = Modifier,
) {
    val dotColors = weekDotColors()
    val labelColor = FieldKit.colors.summaryCaption
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .testTag(WEEK_DOTS_TAG)
                .clearAndSetSemantics { hideFromAccessibility() },
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        days.forEachIndexed { index, day ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(DotLabelGap)) {
                Dot(
                    day = day,
                    colors = dotColors,
                    modifier = Modifier.testTag(WEEK_DOT_TAG_PREFIX + index).semantics { dayDot = day },
                )
                Text(
                    text = day.date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.UK),
                    style = FieldKit.typography.axisLabel,
                    color = labelColor,
                )
            }
        }
    }
}

@Composable
private fun Dot(
    day: DayDot,
    colors: WeekDotColors,
    modifier: Modifier,
) {
    Canvas(modifier = modifier.size(DotSize)) {
        val radius = size.minDimension / 2
        when (day.state) {
            DayDotState.WALKED -> drawCircle(colors.walked, radius)
            DayDotState.PAST_NO_WALK -> drawCircle(colors.pastNoWalk, radius)
            DayDotState.FUTURE -> {
                val stroke = FutureRingWidth.toPx()
                val dash = DashLength.toPx()
                drawCircle(
                    color = colors.futureRing,
                    radius = radius - stroke / 2,
                    style = Stroke(width = stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, dash))),
                )
            }
        }
        if (day.isToday) {
            val ring = TodayRingWidth.toPx()
            drawCircle(
                color = colors.todayRing,
                radius = radius + ring / 2,
                center = Offset(size.width / 2, size.height / 2),
                style = Stroke(width = ring),
            )
        }
    }
}
