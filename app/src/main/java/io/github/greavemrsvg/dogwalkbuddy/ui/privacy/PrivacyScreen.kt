package io.github.greavemrsvg.dogwalkbuddy.ui.privacy

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.greavemrsvg.dogwalkbuddy.R
import io.github.greavemrsvg.dogwalkbuddy.ui.common.ErrorSnackbarEffect
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.MinTouchTarget

/** What the app sends and keeps (AC46), and "Delete all my data" (AC47). */
@Composable
fun PrivacyRoute(
    viewModel: PrivacyViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    PrivacyContent(
        state = state,
        onBack = onBack,
        onRequestDelete = viewModel::requestDelete,
        onConfirmDelete = viewModel::confirmDelete,
        onCancelDelete = viewModel::cancelDelete,
        onDismissDeleteFailure = viewModel::dismissDeleteFailure,
        modifier = modifier,
    )
}

@Composable
fun PrivacyContent(
    state: PrivacyUiState,
    onBack: () -> Unit,
    onRequestDelete: () -> Unit,
    onConfirmDelete: () -> Unit,
    onCancelDelete: () -> Unit,
    modifier: Modifier = Modifier,
    onDismissDeleteFailure: () -> Unit = {},
) {
    val snackbarHost = remember { SnackbarHostState() }
    ErrorSnackbarEffect(
        host = snackbarHost,
        show = state.deleteFailed,
        message = stringResource(R.string.privacy_delete_failed),
        dismissLabel = stringResource(R.string.privacy_delete_failed_dismiss),
        onDismissed = onDismissDeleteFailure,
    )
    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbarHost, modifier = Modifier.safeDrawingPadding()) },
    ) { scaffoldPadding ->
        Surface(modifier = Modifier.padding(scaffoldPadding).fillMaxSize()) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .safeDrawingPadding()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TextButton(onClick = onBack, modifier = Modifier.heightIn(min = MinTouchTarget)) {
                    Text(stringResource(R.string.privacy_back))
                }
                Heading(R.string.privacy_title, MaterialTheme.typography.headlineMedium)
                Heading(R.string.privacy_weather_heading)
                Body(R.string.privacy_weather)
                Heading(R.string.privacy_map_heading)
                Body(R.string.privacy_map)
                Heading(R.string.privacy_local_heading)
                Body(R.string.privacy_local)
                Body(R.string.privacy_no_tracking)
                Heading(R.string.privacy_delete_heading)
                Body(R.string.privacy_delete_explanation)
                DeleteControls(state, onRequestDelete)
            }
        }
    }
    if (state.confirming) DeleteDialog(onConfirm = onConfirmDelete, onCancel = onCancelDelete)
}

@Composable
private fun DeleteControls(
    state: PrivacyUiState,
    onRequestDelete: () -> Unit,
) {
    Button(
        onClick = onRequestDelete,
        enabled = !state.deleting,
        colors =
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
            ),
        modifier = Modifier.heightIn(min = MinTouchTarget),
    ) {
        Text(stringResource(R.string.privacy_delete_all))
    }
    // Announced when it appears, so a TalkBack user hears that the deletion is running and when it is done.
    if (state.deleting) Status(R.string.privacy_deleting)
    if (state.deleted) Status(R.string.privacy_deleted)
}

@Composable
private fun Status(
    @StringRes resId: Int,
) {
    Text(
        text = stringResource(resId),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
    )
}

@Composable
private fun DeleteDialog(
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.privacy_delete_title)) },
        text = { Text(stringResource(R.string.privacy_delete_message)) },
        confirmButton = {
            TextButton(onClick = onConfirm, modifier = Modifier.heightIn(min = MinTouchTarget)) {
                Text(stringResource(R.string.privacy_delete_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel, modifier = Modifier.heightIn(min = MinTouchTarget)) {
                Text(stringResource(R.string.privacy_delete_cancel))
            }
        },
    )
}

@Composable
private fun Heading(
    @StringRes resId: Int,
    style: TextStyle = MaterialTheme.typography.titleLarge,
) {
    Text(text = stringResource(resId), style = style, modifier = Modifier.semantics { heading() })
}

@Composable
private fun Body(
    @StringRes resId: Int,
) {
    Text(text = stringResource(resId), style = MaterialTheme.typography.bodyLarge)
}
