package io.github.greavemrsvg.dogwalkbuddy.ui.check

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.greavemrsvg.dogwalkbuddy.R
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.FieldKit
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.FieldKitSizes

/** Test tag of the header's refresh icon button (AC20). */
const val REFRESH_TAG = "check_refresh"

/**
 * The Today header (AC20): the meta line (rated states only), the check title as a heading, and the refresh icon
 * button (rated states only). With no [ageMinutes] the meta line reads "Your area · checked just now" (D1),
 * otherwise the exact age text after a clock icon.
 */
@Composable
fun TodayHeader(
    dogName: String?,
    rated: Boolean,
    ageMinutes: Long?,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = FieldKit.colors
    Row(
        modifier = modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 6.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (rated) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val icon = if (ageMinutes == null) R.drawable.ic_location else R.drawable.ic_schedule
                    Icon(
                        painter = painterResource(icon),
                        contentDescription = null,
                        tint = colors.onSurfaceVariant,
                        modifier = Modifier.size(FieldKitSizes.iconSmall),
                    )
                    Text(
                        text = if (ageMinutes == null) stringResource(R.string.check_meta_just_now) else ageText(ageMinutes),
                        style = FieldKit.typography.label,
                        color = colors.onSurfaceVariant,
                    )
                }
            }
            Text(
                text = if (dogName == null) stringResource(R.string.check_title) else stringResource(R.string.check_title_named, dogName),
                style = FieldKit.typography.checkQuestion,
                color = colors.onSurface,
                modifier = Modifier.semantics { heading() },
            )
        }
        if (rated) {
            IconButton(
                onClick = onRefresh,
                modifier =
                    Modifier
                        .size(FieldKitSizes.iconButton)
                        .border(BorderStroke(1.dp, colors.outline), CircleShape)
                        .testTag(REFRESH_TAG),
                colors = IconButtonDefaults.iconButtonColors(containerColor = colors.surface, contentColor = colors.onSurface),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_refresh),
                    contentDescription = stringResource(R.string.check_refresh),
                    modifier = Modifier.size(FieldKitSizes.iconNav),
                )
            }
        }
    }
}

@Composable
internal fun ageText(minutes: Long): String {
    val hours = minutes / MINUTES_PER_HOUR
    return if (hours == 0L) {
        stringResource(R.string.check_age_minutes, minutes)
    } else {
        stringResource(R.string.check_age_hours_minutes, hours, minutes % MINUTES_PER_HOUR)
    }
}

private const val MINUTES_PER_HOUR = 60
