package io.github.greavemrsvg.dogwalkbuddy.ui.recording

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.greavemrsvg.dogwalkbuddy.R
import io.github.greavemrsvg.dogwalkbuddy.ui.check.findActivity
import io.github.greavemrsvg.dogwalkbuddy.ui.check.openAppSettings
import io.github.greavemrsvg.dogwalkbuddy.ui.common.StartWalkButton
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.MinTouchTarget

/** Test tag of the full-width Start walk (or "Walk in progress") button on Today. */
const val START_WALK_TAG = "start_walk"

/**
 * Connects the Start walk control to its ViewModel and to the system permission dialogs. Precise location is
 * requested in context, only after the explanation, and the notification permission (Android 13 and later) is
 * requested at the start. Recording runs whatever the answer to the notification request is (ADR 0005).
 */
@Composable
fun StartWalkRoute(
    viewModel: StartWalkViewModel,
    onOpenRecording: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val notificationLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { viewModel.start() }
    val begin = {
        if (needsNotificationPermission(context)) {
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            viewModel.start()
        }
    }
    val preciseLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            val granted = results[Manifest.permission.ACCESS_FINE_LOCATION] == true
            val activity = context.findActivity()
            val canAskAgain =
                activity == null ||
                    ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_FINE_LOCATION)
            if (viewModel.onPreciseLocationResult(granted, canAskAgain)) begin()
        }
    LaunchedEffect(viewModel) { viewModel.started.collect { onOpenRecording() } }
    LifecycleStartEffect(viewModel) {
        viewModel.onShown()
        onStopOrDispose {}
    }
    StartWalkContent(
        state = state,
        onStart = {
            if (state == StartWalkUiState.InProgress) {
                onOpenRecording()
            } else if (viewModel.onStartTapped()) {
                begin()
            }
        },
        // Requesting precise together with approximate is required from Android 12 on.
        onGrantPrecise = {
            preciseLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        },
        onOpenSettings = { context.openAppSettings() },
        modifier = modifier,
    )
}

private fun needsNotificationPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED

/** The Start walk button, with the precise location explanation under it when the walk could not start (AC33). */
@Composable
fun StartWalkContent(
    state: StartWalkUiState,
    onStart: () -> Unit,
    onGrantPrecise: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val label =
            if (state == StartWalkUiState.InProgress) R.string.home_walk_in_progress else R.string.home_start_walk
        StartWalkButton(
            label = stringResource(label),
            icon = R.drawable.ic_play,
            onClick = onStart,
            modifier = Modifier.testTag(START_WALK_TAG),
        )
        when (state) {
            StartWalkUiState.NeedsPreciseLocation -> PrecisePrompt(R.string.walk_grant_precise_location, onGrantPrecise)
            StartWalkUiState.PreciseLocationBlocked -> PrecisePrompt(R.string.walk_open_settings, onOpenSettings)
            StartWalkUiState.Idle, StartWalkUiState.InProgress -> Unit
        }
    }
}

@Composable
private fun PrecisePrompt(
    @StringRes actionLabel: Int,
    onAction: () -> Unit,
) {
    Text(stringResource(R.string.walk_needs_precise_location), style = MaterialTheme.typography.bodyLarge)
    Button(onClick = onAction, modifier = Modifier.fillMaxWidth().heightIn(min = MinTouchTarget)) {
        Text(stringResource(actionLabel))
    }
}
