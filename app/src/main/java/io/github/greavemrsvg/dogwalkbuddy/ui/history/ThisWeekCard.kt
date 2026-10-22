package io.github.greavemrsvg.dogwalkbuddy.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.greavemrsvg.dogwalkbuddy.R
import io.github.greavemrsvg.dogwalkbuddy.domain.walk.DayDot
import io.github.greavemrsvg.dogwalkbuddy.domain.walk.DistanceFormat
import io.github.greavemrsvg.dogwalkbuddy.domain.walk.DurationFormat
import io.github.greavemrsvg.dogwalkbuddy.domain.walk.WeekGroup
import io.github.greavemrsvg.dogwalkbuddy.ui.common.SpokenDuration
import io.github.greavemrsvg.dogwalkbuddy.ui.common.SpokenUnit
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.FieldKit
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.FieldKitShapes
import java.time.Duration

private val CardPaddingHorizontal = 20.dp
private val CardPaddingTop = 20.dp
private val CardPaddingBottom = 18.dp
private val CardGap = 16.dp
private val StatGap = 8.dp
private val StatValueMinSize = 24.sp

/** At or above this font scale the three stats stack, so each value keeps the full card width (AC60). */
private const val STACK_STATS_FONT_SCALE = 1.5f

/**
 * The this-week summary (AC40, AC41): "This week" and the week range, the count, `h:mm` and miles with their
 * captions, and the day dots. It is one accessibility node spoken as "This week: 3 walks, 1 hour 25 minutes, 4.2
 * miles"; the visible texts stay on the node for tests and the dots are hidden.
 */
@Composable
fun ThisWeekCard(
    week: WeekGroup,
    days: List<DayDot>,
    dateText: WalkDateText,
    modifier: Modifier = Modifier,
) {
    val colors = FieldKit.colors
    val resources = LocalResources.current
    val hoursMinutes = DurationFormat.hoursMinutes(week.totalSeconds)
    val miles = DistanceFormat.miles(week.totalM)
    val spoken =
        stringResource(
            R.string.history_summary_spoken,
            pluralStringResource(R.plurals.history_walk_count, week.count, week.count),
            SpokenDuration.format(resources, Duration.ofSeconds(week.totalSeconds), SpokenUnit.MINUTES),
            miles,
        )
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .testTag(THIS_WEEK_TAG)
                .semantics(mergeDescendants = true) { contentDescription = spoken }
                .background(colors.primary, FieldKitShapes.hero)
                .padding(start = CardPaddingHorizontal, end = CardPaddingHorizontal, top = CardPaddingTop, bottom = CardPaddingBottom),
        verticalArrangement = Arrangement.spacedBy(CardGap),
    ) {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(StatGap)) {
            Text(
                text = stringResource(R.string.history_this_week),
                style = FieldKit.typography.body.copy(fontWeight = FieldKit.typography.button.fontWeight),
                color = colors.summaryCaption,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = dateText.weekRange(week.weekStart, week.weekEnd),
                style = FieldKit.typography.label,
                color = colors.summaryCaption,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f),
            )
        }
        val stats =
            listOf(
                week.count.toString() to pluralStringResource(R.plurals.history_caption_walks, week.count),
                hoursMinutes to stringResource(R.string.history_caption_hmm),
                miles to stringResource(R.string.history_caption_miles),
            )
        if (LocalDensity.current.fontScale >= STACK_STATS_FONT_SCALE) {
            Column(verticalArrangement = Arrangement.spacedBy(StatGap)) {
                stats.forEach { (value, caption) -> Stat(value, caption, Modifier.fillMaxWidth()) }
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(StatGap)) {
                stats.forEach { (value, caption) -> Stat(value, caption, Modifier.weight(1f)) }
            }
        }
        if (days.isNotEmpty()) WeekDots(days)
    }
}

@Composable
private fun Stat(
    value: String,
    caption: String,
    modifier: Modifier,
) {
    val colors = FieldKit.colors
    val style = FieldKit.typography.statLarge
    Column(modifier = modifier) {
        BasicText(
            text = value,
            style = style.copy(color = colors.onPrimary),
            maxLines = 1,
            autoSize = TextAutoSize.StepBased(minFontSize = StatValueMinSize, maxFontSize = style.fontSize),
        )
        Text(text = caption, style = FieldKit.typography.label, color = colors.summaryCaption)
    }
}
