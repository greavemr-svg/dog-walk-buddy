package io.github.greavemrsvg.dogwalkbuddy.ui.check

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import io.github.greavemrsvg.dogwalkbuddy.R
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.FieldKit
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.FieldKitShapes

/** Test tag of the Can't check card (AC29). */
const val CANT_CHECK_CARD_TAG = "cant_check_card"

/** Test tag of the "You can still record a walk" panel (AC29, C13). */
const val CAN_STILL_RECORD_TAG = "cant_check_can_still_record"

private val IconCircleSize = 64.dp
private val IconSize = 30.dp
private val RetryHeight = 52.dp
private val RetryIconSize = 20.dp
private val DashedBorderWidth = 2.dp
private val DashOn = 8.dp
private val DashOff = 6.dp
private val CardRadius = 28.dp
private val PanelShape = RoundedCornerShape(20.dp)

/**
 * Can't check (AC29, MVP AC15 and AC22): a dashed card with the cloud-off icon, "Can't check right now", the D4
 * line and the "Try again" pill (C4), then the panel saying a walk can still be recorded (C13). There is no rating,
 * reason, best window or strip.
 */
@Composable
fun CantCheckContent(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        CantCheckCard(onRetry)
        CanStillRecordPanel()
    }
}

@Composable
private fun CantCheckCard(onRetry: () -> Unit) {
    val colors = FieldKit.colors
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .testTag(CANT_CHECK_CARD_TAG)
                .background(colors.surface, FieldKitShapes.hero)
                .drawBehind {
                    val corner = CornerRadius(CardRadius.toPx())
                    val stroke = DashedBorderWidth.toPx()
                    drawRoundRect(
                        color = colors.outline,
                        topLeft = Offset(stroke / 2, stroke / 2),
                        size = Size(size.width - stroke, size.height - stroke),
                        cornerRadius = corner,
                        style = Stroke(width = stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(DashOn.toPx(), DashOff.toPx()))),
                    )
                }.padding(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier.size(IconCircleSize).background(colors.surfaceMuted, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_cloud_off),
                contentDescription = null,
                tint = colors.onSurface,
                modifier = Modifier.size(IconSize),
            )
        }
        Text(
            text = stringResource(R.string.check_cant_check),
            style = FieldKit.typography.screenTitle.copy(lineHeight = 1.05.em),
            color = colors.onSurface,
            modifier =
                Modifier.semantics {
                    heading()
                    liveRegion = LiveRegionMode.Polite
                },
        )
        Text(stringResource(R.string.check_cant_check_explanation), style = FieldKit.typography.bodyLarge, color = colors.onSurfaceVariant)
        Button(
            onClick = onRetry,
            modifier = Modifier.heightIn(min = RetryHeight),
            shape = FieldKitShapes.pill,
            contentPadding = PaddingValues(horizontal = 22.dp),
            colors = ButtonDefaults.buttonColors(containerColor = colors.onSurface, contentColor = colors.surface),
        ) {
            Icon(painter = painterResource(R.drawable.ic_refresh), contentDescription = null, modifier = Modifier.size(RetryIconSize))
            Spacer(Modifier.width(10.dp))
            Text(stringResource(R.string.check_retry), style = FieldKit.typography.button.copy(fontSize = 16.sp))
        }
    }
}

@Composable
private fun CanStillRecordPanel() {
    val colors = FieldKit.colors
    val lead = stringResource(R.string.check_can_still_record_lead)
    val rest = stringResource(R.string.check_can_still_record)
    Text(
        text =
            buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(lead) }
                append(' ')
                append(rest)
            },
        style = FieldKit.typography.body.copy(lineHeight = 1.45.em),
        color = colors.onSurface,
        modifier =
            Modifier
                .fillMaxWidth()
                .testTag(CAN_STILL_RECORD_TAG)
                .background(colors.primaryContainer, PanelShape)
                .padding(horizontal = 18.dp, vertical = 16.dp),
    )
}
