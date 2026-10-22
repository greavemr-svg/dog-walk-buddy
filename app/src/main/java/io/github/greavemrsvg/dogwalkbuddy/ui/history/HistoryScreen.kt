package io.github.greavemrsvg.dogwalkbuddy.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.greavemrsvg.dogwalkbuddy.R
import io.github.greavemrsvg.dogwalkbuddy.domain.walk.DistanceFormat
import io.github.greavemrsvg.dogwalkbuddy.domain.walk.DurationFormat
import io.github.greavemrsvg.dogwalkbuddy.domain.walk.WalkSummary
import io.github.greavemrsvg.dogwalkbuddy.domain.walk.WeekGroup
import io.github.greavemrsvg.dogwalkbuddy.ui.common.ErrorSnackbarEffect
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.MinTouchTarget
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Test tag of the "This week" summary. */
const val THIS_WEEK_TAG = "history_this_week"

/** Test tag of the list of weeks and walks. */
const val HISTORY_LIST_TAG = "history_list"

/** Formats the date and the start time of a walk in the device zone. */
class WalkDateFormats(
    private val zoneId: ZoneId,
    is24Hour: Boolean,
) {
    private val dateFormat = DateTimeFormatter.ofPattern("EEE d MMM yyyy", Locale.UK)
    private val timeFormat = DateTimeFormatter.ofPattern(if (is24Hour) "HH:mm" else "h:mm a", Locale.UK)

    fun date(walk: WalkSummary): String = dateFormat.format(walk.startUtc.atZone(zoneId))

    fun time(walk: WalkSummary): String = timeFormat.format(walk.startUtc.atZone(zoneId))

    fun day(day: LocalDate): String = dateFormat.format(day)
}

/** An action offered only in debug builds, in the History overflow menu. */
class DebugAction(
    val label: String,
    val run: () -> Unit,
)

/**
 * The history: a "This week" summary, then the walks newest first grouped by week with their totals (AC35, AC36,
 * AC37), each with a Delete that asks for confirmation (AC40). [debugActions] is non-empty only in debug builds.
 */
@Composable
fun HistoryRoute(
    viewModel: HistoryViewModel,
    zoneId: ZoneId,
    is24Hour: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    debugActions: List<DebugAction> = emptyList(),
    onOpenWalk: (Long) -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val formats = remember(zoneId, is24Hour) { WalkDateFormats(zoneId, is24Hour) }
    // The week boundary can pass while the app is in the background: recompute "This week" on return.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }
    HistoryContent(
        state = state,
        formats = formats,
        onBack = onBack,
        onRequestDelete = viewModel::requestDelete,
        onConfirmDelete = viewModel::confirmDelete,
        onCancelDelete = viewModel::cancelDelete,
        onDismissDeleteFailure = viewModel::dismissDeleteFailure,
        onOpenWalk = onOpenWalk,
        debugActions = debugActions,
        modifier = modifier,
    )
}

@Composable
fun HistoryContent(
    state: HistoryUiState,
    formats: WalkDateFormats,
    onBack: () -> Unit,
    onRequestDelete: (WalkSummary) -> Unit,
    onConfirmDelete: () -> Unit,
    onCancelDelete: () -> Unit,
    modifier: Modifier = Modifier,
    onDismissDeleteFailure: () -> Unit = {},
    onOpenWalk: (Long) -> Unit = {},
    debugActions: List<DebugAction> = emptyList(),
) {
    val snackbarHost = remember { SnackbarHostState() }
    // A failed delete is shown over the list where the user is, and announced. It stays until the user dismisses it.
    ErrorSnackbarEffect(
        host = snackbarHost,
        show = state.deleteFailed,
        message = stringResource(R.string.history_delete_failed),
        dismissLabel = stringResource(R.string.history_delete_failed_dismiss),
        onDismissed = onDismissDeleteFailure,
    )
    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbarHost, modifier = Modifier.safeDrawingPadding()) },
    ) { scaffoldPadding ->
        HistoryBody(
            state,
            formats,
            onBack,
            onRequestDelete,
            onConfirmDelete,
            onCancelDelete,
            debugActions,
            onOpenWalk,
            Modifier.padding(scaffoldPadding),
        )
    }
}

@Composable
private fun HistoryBody(
    state: HistoryUiState,
    formats: WalkDateFormats,
    onBack: () -> Unit,
    onRequestDelete: (WalkSummary) -> Unit,
    onConfirmDelete: () -> Unit,
    onCancelDelete: () -> Unit,
    debugActions: List<DebugAction>,
    onOpenWalk: (Long) -> Unit,
    modifier: Modifier,
) {
    Surface(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = onBack, modifier = Modifier.heightIn(min = MinTouchTarget)) {
                    Text(stringResource(R.string.history_back))
                }
                if (debugActions.isNotEmpty()) DebugMenu(debugActions)
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize().testTag(HISTORY_LIST_TAG),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    Text(
                        text = stringResource(R.string.history_title),
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.semantics { heading() },
                    )
                }
                if (state.loadFailed) {
                    item { Text(stringResource(R.string.history_load_failed)) }
                } else {
                    item { ThisWeek(state.thisWeek) }
                }
                if (state.isLoaded && state.isEmpty && !state.loadFailed) {
                    item { Text(stringResource(R.string.history_empty), style = MaterialTheme.typography.titleMedium) }
                }
                state.weeks.forEach { week ->
                    item(key = "week-${week.weekStart}") { WeekHeader(week, formats) }
                    items(week.walks, key = { it.id }) { walk -> WalkRow(walk, formats, onOpenWalk, onRequestDelete) }
                }
            }
        }
    }
    state.pendingDelete?.let { walk ->
        DeleteDialog(walk, formats, onConfirm = onConfirmDelete, onCancel = onCancelDelete)
    }
}

@Composable
private fun ThisWeek(week: WeekGroup) {
    Column(modifier = Modifier.fillMaxWidth().testTag(THIS_WEEK_TAG).semantics(mergeDescendants = true) {}) {
        Text(stringResource(R.string.history_this_week), style = MaterialTheme.typography.titleLarge)
        Text(totalsText(week), style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun totalsText(week: WeekGroup): String =
    stringResource(
        R.string.history_totals,
        pluralStringResource(R.plurals.history_walk_count, week.count, week.count),
        DurationFormat.hoursMinutes(week.totalSeconds),
        DistanceFormat.miles(week.totalM),
    )

@Composable
private fun WeekHeader(
    week: WeekGroup,
    formats: WalkDateFormats,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Text(
            text = stringResource(R.string.history_week_range, formats.day(week.weekStart), formats.day(week.weekEnd)),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() },
        )
        Text(totalsText(week), style = MaterialTheme.typography.bodyLarge)
        HorizontalDivider()
    }
}

@Composable
private fun WalkRow(
    walk: WalkSummary,
    formats: WalkDateFormats,
    onOpenWalk: (Long) -> Unit,
    onRequestDelete: (WalkSummary) -> Unit,
) {
    val description = stringResource(R.string.history_delete_description, formats.date(walk), formats.time(walk))
    val openLabel = stringResource(R.string.history_open_walk)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // The date, time, duration and distance are spoken as one label. Opening the walk is the row's action, and
        // Delete stays a separate control beside it.
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .heightIn(min = MinTouchTarget)
                    .clickable(onClickLabel = openLabel, role = Role.Button) { onOpenWalk(walk.id) }
                    .semantics(mergeDescendants = true) {},
        ) {
            Text(
                stringResource(R.string.history_walk_when, formats.date(walk), formats.time(walk)),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                stringResource(
                    R.string.history_walk_stats,
                    DurationFormat.hoursMinutes(walk.durationSeconds),
                    DistanceFormat.miles(walk.distanceM),
                ),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        OutlinedButton(
            onClick = { onRequestDelete(walk) },
            modifier = Modifier.heightIn(min = MinTouchTarget).semantics { contentDescription = description },
        ) {
            Text(stringResource(R.string.history_delete))
        }
    }
}

@Composable
private fun DeleteDialog(
    walk: WalkSummary,
    formats: WalkDateFormats,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.history_delete_title)) },
        text = { Text(stringResource(R.string.history_delete_message, formats.date(walk), formats.time(walk))) },
        confirmButton = {
            TextButton(onClick = onConfirm, modifier = Modifier.heightIn(min = MinTouchTarget)) {
                Text(stringResource(R.string.history_delete_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel, modifier = Modifier.heightIn(min = MinTouchTarget)) {
                Text(stringResource(R.string.history_delete_cancel))
            }
        },
    )
}

@Composable
private fun DebugMenu(actions: List<DebugAction>) {
    var open by remember { mutableStateOf(false) }
    Column {
        TextButton(onClick = { open = true }, modifier = Modifier.heightIn(min = MinTouchTarget)) {
            Text(stringResource(R.string.history_more))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            actions.forEach { action ->
                DropdownMenuItem(
                    text = { Text(action.label) },
                    onClick = {
                        open = false
                        action.run()
                    },
                )
            }
        }
    }
}
