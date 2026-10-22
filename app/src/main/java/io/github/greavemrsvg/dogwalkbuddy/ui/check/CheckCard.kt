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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.core.app.ActivityCompat
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.greavemrsvg.dogwalkbuddy.R
import io.github.greavemrsvg.dogwalkbuddy.domain.rating.HeatSensitivity
import io.github.greavemrsvg.dogwalkbuddy.ui.common.InfoBanner
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.FieldKitSpacing
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.MinTouchTarget

/**
 * Test tag of the hero card, whose spoken description starts "<Rating>. <Reason>." (MVP AC50) and goes on with the
 * four factor cells (AC19).
 */
const val RATING_BLOCK_TAG = "check_rating"

/** Prefix of the rating icon's test tag. The tag ends with the drawable resource id (AC53). */
const val RATING_ICON_TAG_PREFIX = "check_rating_icon_"

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
    val dogName by viewModel.dogName.collectAsStateWithLifecycle()
    CheckCard(
        state = state,
        dogName = dogName,
        windowFormatter = windowFormatter,
        onRefresh = viewModel::refresh,
        onGrantPermission = { permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION) },
        onOpenAbout = onOpenAbout,
        onOpenProfile = onOpenProfile,
        onOpenSettings = { context.openAppSettings() },
        modifier = modifier,
    )
}

internal tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

/** Opens this app's page in system Settings, where the permission can be granted. It needs no permission. */
internal fun Context.openAppSettings() {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
    if (this !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        // No Settings app to open: nothing more can be done here.
    }
}

/**
 * The Today check content, in the binding order of the design (AC25): header, then for a rated result the banner
 * (cached only), the hero card, the footnote and the best window; in the other states the header (title only),
 * the state's card and the footnote. [dogName] gives the title in every state (C1).
 */
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
    dogName: String? = null,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(FieldKitSpacing.stackGap)) {
        val rated = state as? CheckUiState.Rated
        TodayHeader(
            dogName = rated?.dogName ?: dogName,
            rated = rated != null,
            ageMinutes = rated?.ageMinutes,
            onRefresh = onRefresh,
        )
        when (state) {
            CheckUiState.Loading -> LoadingHeroCard()
            CheckUiState.NeedsLocationPermission -> LocationPrompt(onGrantPermission)
            CheckUiState.LocationPermissionBlocked -> LocationBlocked(onOpenSettings)
            CheckUiState.CantCheck -> CantCheck(onRefresh)
            is CheckUiState.Rated -> RatedResult(state)
        }
        TodayFootnote(
            rated =
                rated?.let {
                    FootnoteRatedLines(
                        usingDefaultSensitivity = it.usingDefaultSensitivity,
                        sensitiveDogName = it.dogName.takeIf { _ -> it.sensitivity == HeatSensitivity.SENSITIVE },
                    )
                },
            onOpenAbout = onOpenAbout,
            onOpenProfile = onOpenProfile,
        )
        if (rated != null) WindowBlock(windowFormatter.format(rated.window))
    }
}

@Composable
private fun RatedResult(state: CheckUiState.Rated) {
    if (state.isCached) {
        InfoBanner(
            icon = R.drawable.ic_wifi_off,
            lead = if (state.isOffline) stringResource(R.string.check_banner_offline_lead) else null,
            text = stringResource(R.string.check_banner_cached),
        )
    }
    RatingHeroCard(rating = state.current.rating, reason = state.current.reason, factors = state.factors)
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
