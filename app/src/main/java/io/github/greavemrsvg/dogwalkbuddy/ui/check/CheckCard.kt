package io.github.greavemrsvg.dogwalkbuddy.ui.check

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.greavemrsvg.dogwalkbuddy.R
import io.github.greavemrsvg.dogwalkbuddy.domain.rating.Rating
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.LocalRatingColors
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.MinTouchTarget

/** Test tag of the rating and reason block, whose spoken description is "<Rating>. <Reason>." (AC50). */
const val RATING_BLOCK_TAG = "check_rating"

/** Prefix of the rating icon's test tag. The tag ends with the drawable resource id (AC53). */
const val RATING_ICON_TAG_PREFIX = "check_rating_icon_"

private val RatingIconSize = 40.dp

/** Connects the check card to its ViewModel and to the system permission dialog. */
@Composable
fun CheckRoute(
    viewModel: CheckViewModel,
    windowFormatter: BestWindowFormatter,
    onOpenAbout: () -> Unit,
    onOpenProfile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // Opening the screen is one of the only two triggers for a check (AC24). The ViewModel decides whether a
    // check is needed, so coming back from another screen does not make a second request.
    LaunchedEffect(viewModel) { viewModel.onOpened() }
    // Every time the screen starts again, the shown age is re-evaluated and a permission granted in Settings is
    // noticed. This makes no network request.
    LifecycleStartEffect(viewModel) {
        viewModel.onShown()
        onStopOrDispose {}
    }
    val context = LocalContext.current
    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            // After a request, "no rationale" and not granted means the system will not show the dialog again.
            val activity = context.findActivity()
            val canAskAgain =
                activity == null ||
                    ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_COARSE_LOCATION)
            viewModel.onPermissionResult(granted, canAskAgain)
        }
    CheckCard(
        state = state,
        windowFormatter = windowFormatter,
        onRefresh = viewModel::refresh,
        onGrantPermission = { permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION) },
        onOpenAbout = onOpenAbout,
        onOpenProfile = onOpenProfile,
        onOpenSettings = { context.openAppSettings() },
        modifier = modifier,
    )
}

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

/** Opens this app's page in system Settings, where the permission can be granted. It needs no permission. */
private fun Context.openAppSettings() {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
    if (this !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        // No Settings app to open: nothing more can be done here.
    }
}

/** The check card: the rating, its reason, the best window, and the guidance and attribution notes. */
@Composable
fun CheckCard(
    state: CheckUiState,
    windowFormatter: BestWindowFormatter,
    onRefresh: () -> Unit,
    onGrantPermission: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenProfile: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenSettings: () -> Unit = {},
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.check_title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.semantics { heading() },
        )
        when (state) {
            CheckUiState.Loading -> StatusMessage(stringResource(R.string.check_loading))
            CheckUiState.NeedsLocationPermission -> LocationPrompt(onGrantPermission)
            CheckUiState.LocationPermissionBlocked -> LocationBlocked(onOpenSettings)
            CheckUiState.CantCheck -> CantCheck(onRefresh)
            is CheckUiState.Rated -> RatedResult(state, windowFormatter, onRefresh, onOpenAbout, onOpenProfile)
        }
        if (state !is CheckUiState.Rated) GuidanceNote(onOpenAbout)
        Text(text = stringResource(R.string.check_attribution), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun RatedResult(
    state: CheckUiState.Rated,
    windowFormatter: BestWindowFormatter,
    onRefresh: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenProfile: () -> Unit,
) {
    RatingBlock(state.current.rating, stringResource(state.current.reason.textRes()))
    // The disclaimer sits directly under the rating and reason, so it is on screen without scrolling.
    GuidanceNote(onOpenAbout)
    state.ageMinutes?.let { Text(ageText(it), style = MaterialTheme.typography.bodyLarge) }
    WindowBlock(windowFormatter.format(state.window))
    OutlinedButton(onClick = onRefresh, modifier = Modifier.heightIn(min = MinTouchTarget)) {
        Text(stringResource(R.string.check_refresh))
    }
    if (state.usingDefaultSensitivity) {
        TextButton(onClick = onOpenProfile, modifier = Modifier.heightIn(min = MinTouchTarget)) {
            Text(stringResource(R.string.check_default_sensitivity))
        }
    }
    Text(stringResource(R.string.check_pollen_not_included), style = MaterialTheme.typography.bodyMedium)
}

/**
 * The rating label with its icon, and the reason. They are spoken as one live-region phrase, "<Rating>. <Reason>."
 * (AC50). Rating is shown by text and icon as well as colour (AC53).
 */
@Composable
private fun RatingBlock(
    rating: Rating,
    reason: String,
) {
    val label = stringResource(rating.labelRes())
    val spoken = stringResource(R.string.check_rating_announcement, label, reason)
    val colour =
        LocalRatingColors.current.let {
            when (rating) {
                Rating.GO -> it.go
                Rating.CAUTION -> it.caution
                Rating.AVOID -> it.avoid
            }
        }
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier =
            Modifier
                .fillMaxWidth()
                .testTag(RATING_BLOCK_TAG)
                .clearAndSetSemantics {
                    contentDescription = spoken
                    liveRegion = LiveRegionMode.Polite
                },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(
                painter = painterResource(rating.iconRes()),
                contentDescription = null,
                tint = colour,
                modifier = Modifier.size(RatingIconSize).testTag(RATING_ICON_TAG_PREFIX + rating.iconRes()),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = colour,
            )
        }
        Text(text = reason, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun WindowBlock(window: BestWindowText) {
    Column(modifier = Modifier.semantics(mergeDescendants = true) {}) {
        Text(stringResource(R.string.check_window_heading), style = MaterialTheme.typography.titleMedium)
        window.label?.let { Text(it, style = MaterialTheme.typography.bodyLarge) }
        Text(window.text, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun StatusMessage(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
    )
}

@Composable
private fun LocationPrompt(onGrantPermission: () -> Unit) {
    StatusMessage(stringResource(R.string.check_needs_location))
    Button(onClick = onGrantPermission, modifier = Modifier.fillMaxWidth().heightIn(min = MinTouchTarget)) {
        Text(stringResource(R.string.check_grant_location))
    }
}

@Composable
private fun LocationBlocked(onOpenSettings: () -> Unit) {
    StatusMessage(stringResource(R.string.check_needs_location))
    Button(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth().heightIn(min = MinTouchTarget)) {
        Text(stringResource(R.string.check_open_settings))
    }
}

@Composable
private fun CantCheck(onRetry: () -> Unit) {
    StatusMessage(stringResource(R.string.check_cant_check))
    Button(onClick = onRetry, modifier = Modifier.heightIn(min = MinTouchTarget)) {
        Text(stringResource(R.string.check_retry))
    }
}

@Composable
private fun GuidanceNote(onOpenAbout: () -> Unit) {
    Column {
        Text(stringResource(R.string.check_disclaimer), style = MaterialTheme.typography.bodyMedium)
        TextButton(onClick = onOpenAbout, modifier = Modifier.heightIn(min = MinTouchTarget)) {
            Text(stringResource(R.string.home_about))
        }
    }
}

@Composable
private fun ageText(minutes: Long): String {
    val hours = minutes / MINUTES_PER_HOUR
    return if (hours == 0L) {
        stringResource(R.string.check_age_minutes, minutes)
    } else {
        stringResource(R.string.check_age_hours_minutes, hours, minutes % MINUTES_PER_HOUR)
    }
}

private const val MINUTES_PER_HOUR = 60
