package io.github.greavemrsvg.dogwalkbuddy.ui.recording

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.greavemrsvg.dogwalkbuddy.R
import io.github.greavemrsvg.dogwalkbuddy.domain.location.LatLon
import io.github.greavemrsvg.dogwalkbuddy.domain.walk.LiveWalkFormat
import io.github.greavemrsvg.dogwalkbuddy.service.RecordingSession
import io.github.greavemrsvg.dogwalkbuddy.ui.common.BackButton
import io.github.greavemrsvg.dogwalkbuddy.ui.common.SpokenDuration
import io.github.greavemrsvg.dogwalkbuddy.ui.common.StatCard
import io.github.greavemrsvg.dogwalkbuddy.ui.common.StatCardColors
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.FieldKit
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.FieldKitShapes
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.FieldKitSizes
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.FieldKitSpacing
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.RecordingTheme
import kotlinx.coroutines.delay
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Test tag of the full-width Stop button. */
const val STOP_WALK_TAG = "stop_walk"

/** Test tags of the elapsed time, distance and start time values. */
const val ELAPSED_TAG = "recording_elapsed"
const val DISTANCE_TAG = "recording_distance"
const val STARTED_TAG = "recording_started"

/** Test tags of the timer text, the decorative live dot and the decorative stop icon. */
const val TIMER_TAG = "recording_timer"
const val LIVE_DOT_TAG = "recording_live_dot"
const val STOP_ICON_TAG = "recording_stop_icon"

private const val TICK_MS = 1_000L

/**
 * The recording screen: elapsed time, distance, start time, the route so far and Stop (AC28, AC33-AC37). It is
 * always dark. It closes itself when no walk is being recorded, which includes the moment after Stop has saved the
 * walk.
 */
@Composable
fun RecordingRoute(
    viewModel: RecordingViewModel,
    clock: Clock,
    startedFormat: StartedTimeFormat,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val session by viewModel.session.collectAsStateWithLifecycle()
    val route by viewModel.route.collectAsStateWithLifecycle()
    val stopping by viewModel.stopping.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var notificationsGranted by remember { mutableStateOf(notificationsAllowed(context)) }
    // A permission changed in Settings is noticed when the screen starts again.
    LifecycleStartEffect(Unit) {
        notificationsGranted = notificationsAllowed(context)
        onStopOrDispose {}
    }
    val current = session
    LaunchedEffect(current == null) { if (current == null) onClose() }
    RecordingSystemBars()
    if (current != null) {
        RecordingContent(
            session = current,
            now = rememberNow(clock),
            route = route,
            startedFormat = startedFormat,
            stopping = stopping,
            showNotificationsHint = !notificationsGranted,
            onStop = viewModel::stop,
            onBack = onClose,
            modifier = modifier,
        )
    }
}

private fun notificationsAllowed(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

/** The current time from [clock], refreshed every second while the screen is shown. */
@Composable
private fun rememberNow(clock: Clock): Instant {
    var now by remember(clock) { mutableStateOf(clock.instant()) }
    LaunchedEffect(clock) {
        while (true) {
            now = clock.instant()
            delay(TICK_MS)
        }
    }
    return now
}

/** The start time in the device's 12-hour or 24-hour format (AC34, C15). */
class StartedTimeFormat(
    zone: ZoneId,
    is24Hour: Boolean,
) {
    private val formatter = DateTimeFormatter.ofPattern(if (is24Hour) "HH:mm" else "h:mm a", Locale.UK).withZone(zone)

    fun format(instant: Instant): String = formatter.format(instant)
}

private val LiveDotSize = 8.dp
private val StopIconSize = 24.dp
private val StopShape = RoundedCornerShape(24.dp)
private val PillPadding = PaddingValues(start = 12.dp, top = 8.dp, end = 14.dp, bottom = 8.dp)
private val ContentGap = 20.dp
private val SmallGap = 8.dp
private val TipIconGap = 10.dp
private val StopIconGap = 12.dp

/** The timer never shrinks below this at large font sizes, so it stays at least the MVP's 200 % size (AC38). */
private val TimerMinSize = 36.sp

/** The " mi" after the distance is half the size of the number, as in the pack. */
private const val UNIT_SCALE = 0.5f

/** The Recording screen's content, always in the recording palette (AC4, AC33). */
@Composable
fun RecordingContent(
    session: RecordingSession,
    now: Instant,
    route: List<LatLon>,
    startedFormat: StartedTimeFormat,
    stopping: Boolean,
    showNotificationsHint: Boolean,
    onStop: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    RecordingTheme {
        val palette = FieldKit.recording
        Surface(modifier = modifier.fillMaxSize(), color = palette.background, contentColor = palette.onDark) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .safeDrawingPadding()
                        .verticalScroll(rememberScrollState())
                        .padding(FieldKitSpacing.gutter),
                verticalArrangement = Arrangement.spacedBy(ContentGap),
            ) {
                TopRow(onBack)
                Timer(Duration.between(session.startUtc, now))
                StatCards(session, startedFormat)
                RouteSketch(route = route)
                Tip(showNotificationsHint)
                StopButton(stopping, onStop)
            }
        }
    }
}

@Composable
private fun TopRow(onBack: () -> Unit) {
    val palette = FieldKit.recording
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(SmallGap)) {
        BackButton(onClick = onBack, contentDescription = stringResource(R.string.recording_back))
        Row(
            modifier = Modifier.background(palette.card, FieldKitShapes.pill).padding(PillPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SmallGap),
        ) {
            Box(Modifier.size(LiveDotSize).background(palette.liveDot, CircleShape).testTag(LIVE_DOT_TAG))
            Text(
                text = stringResource(R.string.recording_title),
                style = FieldKit.typography.label.copy(fontWeight = FontWeight.Bold),
                color = palette.onDark,
                modifier = Modifier.semantics { heading() },
            )
        }
    }
}

/** "Elapsed time" over the h:mm:ss timer, which is spoken as the duration in words (AC34, D6). */
@Composable
private fun Timer(elapsed: Duration) {
    val palette = FieldKit.recording
    val spoken = SpokenDuration.format(LocalResources.current, elapsed)
    val style = FieldKit.typography.timer
    Column(modifier = Modifier.testTag(ELAPSED_TAG).semantics(mergeDescendants = true) {}) {
        Text(
            text = stringResource(R.string.recording_elapsed_label),
            style = FieldKit.typography.body.copy(fontWeight = FontWeight.SemiBold),
            color = palette.onDarkMuted,
        )
        BasicText(
            text = LiveWalkFormat.elapsed(elapsed),
            style = style.copy(color = palette.onDark),
            maxLines = 1,
            autoSize = TextAutoSize.StepBased(minFontSize = TimerMinSize, maxFontSize = style.fontSize),
            modifier = Modifier.testTag(TIMER_TAG).semantics { contentDescription = spoken },
        )
    }
}

@Composable
private fun StatCards(
    session: RecordingSession,
    startedFormat: StartedTimeFormat,
) {
    val palette = FieldKit.recording
    val colors = StatCardColors(container = palette.card, caption = palette.onDarkMuted, value = palette.onDark)
    Row(horizontalArrangement = Arrangement.spacedBy(FieldKitSpacing.stackGap)) {
        StatCard(
            caption = stringResource(R.string.recording_distance_label),
            value = distanceValue(LiveWalkFormat.miles(session.distanceM), palette.onDarkMuted),
            colors = colors,
            modifier = Modifier.weight(1f).testTag(DISTANCE_TAG),
        )
        StatCard(
            caption = stringResource(R.string.recording_started_label),
            value = AnnotatedString(startedFormat.format(session.startUtc)),
            colors = colors,
            modifier = Modifier.weight(1f).testTag(STARTED_TAG),
        )
    }
}

/** The existing "{x.x} mi" text, with everything but the number smaller and muted. */
@Composable
private fun distanceValue(
    miles: String,
    unitColor: Color,
): AnnotatedString {
    val text = stringResource(R.string.recording_distance_miles, miles)
    val numberStart = text.indexOf(miles)
    val numberEnd = numberStart + miles.length
    val unitStyle = SpanStyle(fontSize = UNIT_SCALE.em, fontWeight = FontWeight.SemiBold, color = unitColor)
    return buildAnnotatedString {
        append(text)
        if (numberStart > 0) addStyle(unitStyle, 0, numberStart)
        if (numberStart >= 0 && numberEnd < text.length) addStyle(unitStyle, numberEnd, text.length)
    }
}

/** The tip, or the notifications-off notice in its place (AC36). */
@Composable
private fun Tip(showNotificationsHint: Boolean) {
    val palette = FieldKit.recording
    Row(horizontalArrangement = Arrangement.spacedBy(TipIconGap)) {
        Icon(
            painter = painterResource(R.drawable.ic_walk),
            contentDescription = null,
            tint = palette.onDarkMuted,
            modifier = Modifier.size(FieldKitSizes.iconInline),
        )
        Text(
            text = stringResource(if (showNotificationsHint) R.string.recording_notifications_off else R.string.recording_tip),
            style = FieldKit.typography.body,
            color = palette.onDarkMuted,
        )
    }
}

/** Stop: full width, 76 dp, radius 24, with a decorative stop icon (AC37). Disabled while the walk saves. */
@Composable
private fun StopButton(
    stopping: Boolean,
    onStop: () -> Unit,
) {
    val palette = FieldKit.recording
    Button(
        onClick = onStop,
        enabled = !stopping,
        shape = StopShape,
        colors =
            ButtonDefaults.buttonColors(
                containerColor = palette.stopBackground,
                contentColor = palette.stopForeground,
                disabledContainerColor = palette.card,
                disabledContentColor = palette.onDarkMuted,
            ),
        modifier = Modifier.fillMaxWidth().heightIn(min = FieldKitSizes.stopHeight).testTag(STOP_WALK_TAG),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_stop),
            contentDescription = null,
            modifier = Modifier.size(StopIconSize).testTag(STOP_ICON_TAG),
        )
        Spacer(Modifier.width(StopIconGap))
        Text(
            text = stringResource(if (stopping) R.string.recording_stopping else R.string.recording_stop),
            style = FieldKit.typography.buttonStop,
            textAlign = TextAlign.Center,
        )
    }
}
