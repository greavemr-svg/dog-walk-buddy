package io.github.greavemrsvg.dogwalkbuddy.ui.privacy

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.greavemrsvg.dogwalkbuddy.R
import io.github.greavemrsvg.dogwalkbuddy.ui.common.BackButton
import io.github.greavemrsvg.dogwalkbuddy.ui.common.ConfirmSheet
import io.github.greavemrsvg.dogwalkbuddy.ui.common.DestructiveOutlinedButton
import io.github.greavemrsvg.dogwalkbuddy.ui.common.ErrorSnackbarEffect
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.FieldKit
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.FieldKitShapes
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.FieldKitSpacing

private val TopBarGap = 8.dp
private val HeadlineSize = 30.sp
private val SectionHeadingSize = 18.sp
private val CardGap = 10.dp
private val CardIconSize = 22.dp
private val EntryGap = 6.dp

/**
 * What the app sends and keeps, in two cards with the exact MVP sentences (AC54, K10), and "Delete all my data" with
 * its confirmation sheet and status texts (AC55).
 */
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
        containerColor = FieldKit.colors.background,
        snackbarHost = { SnackbarHost(snackbarHost, modifier = Modifier.safeDrawingPadding()) },
    ) { scaffoldPadding ->
        Column(
            modifier =
                Modifier
                    .padding(scaffoldPadding)
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = FieldKitSpacing.gutter, vertical = FieldKitSpacing.stackGap),
            verticalArrangement = Arrangement.spacedBy(FieldKitSpacing.sectionGap),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(TopBarGap)) {
                BackButton(onClick = onBack, contentDescription = stringResource(R.string.privacy_back))
                Heading(R.string.privacy_title, FieldKit.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
            }
            Heading(R.string.privacy_headline, FieldKit.typography.screenTitle.copy(fontSize = HeadlineSize))
            PrivacyCard(icon = R.drawable.ic_phone, heading = R.string.privacy_card_local_heading) {
                Body(R.string.privacy_local)
                Body(R.string.privacy_no_tracking)
            }
            PrivacyCard(icon = R.drawable.ic_send_out, heading = R.string.privacy_card_sent_heading) {
                Heading(R.string.privacy_entry_met_heading, FieldKit.typography.label)
                Body(R.string.privacy_weather)
                Heading(R.string.privacy_entry_map_heading, FieldKit.typography.label, Modifier.padding(top = EntryGap))
                Body(R.string.privacy_map)
            }
            Column(verticalArrangement = Arrangement.spacedBy(FieldKitSpacing.stackGap)) {
                Heading(R.string.privacy_delete_heading)
                Body(R.string.privacy_delete_explanation)
                DeleteControls(state, onRequestDelete)
            }
        }
    }
    if (state.confirming) {
        ConfirmSheet(
            title = stringResource(R.string.privacy_delete_title),
            message = stringResource(R.string.privacy_delete_message),
            confirmLabel = stringResource(R.string.privacy_delete_confirm),
            cancelLabel = stringResource(R.string.privacy_delete_cancel),
            onConfirm = onConfirmDelete,
            onCancel = onCancelDelete,
        )
    }
}

/** A `surface` card with a decorative icon beside its [heading], then [content] (AC54). */
@Composable
private fun PrivacyCard(
    @DrawableRes icon: Int,
    @StringRes heading: Int,
    content: @Composable () -> Unit,
) {
    val colors = FieldKit.colors
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(colors.surface, FieldKitShapes.card)
                .padding(FieldKitSpacing.cardPadding),
        verticalArrangement = Arrangement.spacedBy(CardGap),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(CardGap)) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(CardIconSize),
            )
            Heading(heading, FieldKit.typography.bodyLarge.copy(fontWeight = FontWeight.ExtraBold))
        }
        content()
    }
}

@Composable
private fun DeleteControls(
    state: PrivacyUiState,
    onRequestDelete: () -> Unit,
) {
    DestructiveOutlinedButton(
        label = stringResource(R.string.privacy_delete_all),
        onClick = onRequestDelete,
        enabled = !state.deleting,
    )
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
        style = FieldKit.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
        color = FieldKit.colors.onSurface,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
    )
}

@Composable
private fun Heading(
    @StringRes resId: Int,
    style: TextStyle = FieldKit.typography.bodyLarge.copy(fontWeight = FontWeight.ExtraBold, fontSize = SectionHeadingSize),
    modifier: Modifier = Modifier,
) {
    Text(text = stringResource(resId), style = style, color = FieldKit.colors.onSurface, modifier = modifier.semantics { heading() })
}

@Composable
private fun Body(
    @StringRes resId: Int,
) {
    Text(text = stringResource(resId), style = FieldKit.typography.body, color = FieldKit.colors.onSurfaceVariant)
}
