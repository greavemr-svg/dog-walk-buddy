package io.github.greavemrsvg.dogwalkbuddy.ui.recording

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.greavemrsvg.dogwalkbuddy.R
import io.github.greavemrsvg.dogwalkbuddy.domain.walk.LiveWalkFormat
import io.github.greavemrsvg.dogwalkbuddy.service.RecordingSession
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.MinTouchTarget
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.StartStopMinHeight
import kotlinx.coroutines.delay
import java.time.Clock
import java.time.Duration
import java.time.Instant

/** Test tag of the full-width Stop button. */
const val STOP_WALK_TAG = "stop_walk"

/** Test tags of the elapsed time and distance values. */
const val ELAPSED_TAG = "recording_elapsed"
const val DISTANCE_TAG = "recording_distance"

private const val TICK_MS = 1_000L

/**
 * The recording screen: elapsed time, distance and Stop (AC28). It closes itself when no walk is being recorded,
 * which includes the moment after Stop has saved the walk.
 */
@Composable
fun RecordingRoute(
    viewModel: RecordingViewModel,
    clock: Clock,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val session by viewModel.session.collectAsStateWithLifecycle()
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
    if (current != null) {
        RecordingContent(
            session = current,
            now = rememberNow(clock),
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

@Composable
fun RecordingContent(
    session: RecordingSession,
    now: Instant,
    stopping: Boolean,
    showNotificationsHint: Boolean,
    onStop: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TextButton(onClick = onBack, modifier = Modifier.heightIn(min = MinTouchTarget)) {
                Text(stringResource(R.string.recording_back))
            }
            Text(
                text = stringResource(R.string.recording_title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.semantics { heading() },
            )
            Metric(R.string.recording_elapsed_label, LiveWalkFormat.elapsed(Duration.between(session.startUtc, now)), ELAPSED_TAG)
            Metric(
                R.string.recording_distance_label,
                stringResource(R.string.recording_distance_miles, LiveWalkFormat.miles(session.distanceM)),
                DISTANCE_TAG,
            )
            if (showNotificationsHint) {
                Text(stringResource(R.string.recording_notifications_off), style = MaterialTheme.typography.bodyLarge)
            }
            Button(
                onClick = onStop,
                enabled = !stopping,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                modifier = Modifier.fillMaxWidth().heightIn(min = StartStopMinHeight).testTag(STOP_WALK_TAG),
            ) {
                Text(
                    stringResource(if (stopping) R.string.recording_stopping else R.string.recording_stop),
                    style = MaterialTheme.typography.titleLarge,
                )
            }
        }
    }
}

@Composable
private fun Metric(
    @StringRes label: Int,
    value: String,
    tag: String,
) {
    Column(modifier = Modifier.testTag(tag).semantics(mergeDescendants = true) {}) {
        Text(stringResource(label), style = MaterialTheme.typography.titleMedium)
        Text(value, style = MaterialTheme.typography.displayMedium)
    }
}
