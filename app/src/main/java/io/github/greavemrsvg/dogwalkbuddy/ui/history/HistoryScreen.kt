package io.github.greavemrsvg.dogwalkbuddy.ui.history

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.greavemrsvg.dogwalkbuddy.R
import io.github.greavemrsvg.dogwalkbuddy.domain.walk.DistanceFormat
import io.github.greavemrsvg.dogwalkbuddy.domain.walk.DurationFormat
import io.github.greavemrsvg.dogwalkbuddy.domain.walk.WalkSummary
import io.github.greavemrsvg.dogwalkbuddy.domain.walk.WeekGroup
import io.github.greavemrsvg.dogwalkbuddy.ui.common.ConfirmSheet
import io.github.greavemrsvg.dogwalkbuddy.ui.common.ErrorSnackbarEffect
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.FieldKit
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.FieldKitShapes
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.FieldKitSizes
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.FieldKitSpacing
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.MinTouchTarget
import java.time.Clock
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Test tag of the "This week" summary. */
const val THIS_WEEK_TAG = "history_this_week"

/** Test tag of the list of weeks and walks. */
const val HISTORY_LIST_TAG = "history_list"

/** Test tag of the decorative icon of the empty state (AC42). */
const val HISTORY_EMPTY_ICON_TAG = "history_empty_icon"

private val RowTileSize = 44.dp
private val RowPaddingVertical = 12.dp
private val RowPaddingHorizontal = 14.dp
private val SectionGap = 8.dp
private val SectionHeaderSide = 4.dp
private val CardRadius = 22.dp
private val EmptyIconSize = 48.dp
private val MenuBorderWidth = 1.dp
private val EmptyPaddingTop = 32.dp
private val RowTitleSize = 16.sp
private val TitlePaddingTop = 26.dp

/**
 * Formats the date and the start time of a walk in the device zone, with the year. The delete labels and messages
 * keep this format (AC44, K11).
 */
class WalkDateFormats(
    val zoneId: ZoneId,
    val is24Hour: Boolean,
) {
    private val dateFormat = DateTimeFormatter.ofPattern("EEE d MMM yyyy", Locale.UK)
    private val timeFormat = DateTimeFormatter.ofPattern(if (is24Hour) "HH:mm" else "h:mm a", Locale.UK)

    fun date(walk: WalkSummary): String = dateFormat.format(walk.startUtc.atZone(zoneId))

    fun time(walk: WalkSummary): String = timeFormat.format(walk.startUtc.atZone(zoneId))
}

/** An action offered only in debug builds, in the History overflow menu. */
class DebugAction(
    val label: String,
    val run: () -> Unit,
)

/**
 * The Walks tab: a "This week" card, then the walks newest first grouped by week with their totals (AC39-AC41), each
 * row with a "More" menu whose Delete asks for confirmation in a sheet (AC44, OD2). [debugActions] is non-empty only
 * in debug builds.
 */
@Composable
fun HistoryRoute(
    viewModel: HistoryViewModel,
    zoneId: ZoneId,
    is24Hour: Boolean,
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
        onRequestDelete = viewModel::requestDelete,
        onConfirmDelete = viewModel::confirmDelete,
        onCancelDelete = viewModel::cancelDelete,
        onDismissDeleteFailure = viewModel::dismissDeleteFailure,
        onOpenWalk = onOpenWalk,
        debugActions = debugActions,
        clock = viewModel.clock,
        modifier = modifier,
    )
}

/** The Walks tab content. [clock] decides the current year for the dates (D7). */
@Composable
fun HistoryContent(
    state: HistoryUiState,
    formats: WalkDateFormats,
    onRequestDelete: (WalkSummary) -> Unit,
    onConfirmDelete: () -> Unit,
    onCancelDelete: () -> Unit,
    modifier: Modifier = Modifier,
    onDismissDeleteFailure: () -> Unit = {},
    onOpenWalk: (Long) -> Unit = {},
    debugActions: List<DebugAction> = emptyList(),
    clock: Clock = Clock.systemDefaultZone(),
) {
    val rangeTemplate = stringResource(R.string.history_week_range_span)
    val dateText = remember(formats, clock, rangeTemplate) { WalkDateText(formats.zoneId, clock, formats.is24Hour, rangeTemplate) }
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
        containerColor = FieldKit.colors.background,
        snackbarHost = { SnackbarHost(snackbarHost, modifier = Modifier.safeDrawingPadding()) },
    ) { scaffoldPadding ->
        HistoryBody(
            state = state,
            texts = RowTexts(dateText, formats),
            onRequestDelete = onRequestDelete,
            debugActions = debugActions,
            onOpenWalk = onOpenWalk,
            modifier = Modifier.padding(scaffoldPadding),
        )
    }
    state.pendingDelete?.let { walk ->
        // The confirmation keeps the MVP title, message (with the year) and buttons (AC44, K11).
        ConfirmSheet(
            title = stringResource(R.string.history_delete_title),
            message = stringResource(R.string.history_delete_message, formats.date(walk), formats.time(walk)),
            confirmLabel = stringResource(R.string.history_delete_confirm),
            cancelLabel = stringResource(R.string.history_delete_cancel),
            onConfirm = onConfirmDelete,
            onCancel = onCancelDelete,
        )
    }
}

/** The date texts a row needs: the D7 dates it shows, and the year format of its delete label (AC44). */
private class RowTexts(
    val dateText: WalkDateText,
    val formats: WalkDateFormats,
)

@Composable
private fun HistoryBody(
    state: HistoryUiState,
    texts: RowTexts,
    onRequestDelete: (WalkSummary) -> Unit,
    debugActions: List<DebugAction>,
    onOpenWalk: (Long) -> Unit,
    modifier: Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = FieldKit.colors.background) {
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .padding(horizontal = FieldKitSpacing.gutter)
                    .testTag(HISTORY_LIST_TAG),
        ) {
            item { TitleRow(debugActions) }
            if (state.loadFailed) {
                item {
                    Text(
                        text = stringResource(R.string.history_load_failed),
                        style = FieldKit.typography.bodyLarge,
                        color = FieldKit.colors.onSurface,
                        modifier = Modifier.padding(horizontal = SectionHeaderSide),
                    )
                }
            } else {
                item { ThisWeekCard(state.thisWeek, state.thisWeekDays, texts.dateText) }
            }
            if (state.isLoaded && state.isEmpty && !state.loadFailed) {
                item { EmptyState() }
            }
            state.weeks.forEach { week -> weekSection(week, texts, onOpenWalk, onRequestDelete) }
        }
    }
}

@Composable
private fun TitleRow(debugActions: List<DebugAction>) {
    // Walks is a tab, so it has no back control (AC11, AC39); system Back returns to Today.
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = TitlePaddingTop, bottom = FieldKitSpacing.rowGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.history_title),
            style = FieldKit.typography.screenTitle,
            color = FieldKit.colors.onSurface,
            modifier = Modifier.weight(1f).padding(horizontal = SectionHeaderSide).semantics { heading() },
        )
        if (debugActions.isNotEmpty()) DebugMenu(debugActions)
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = EmptyPaddingTop),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(FieldKitSpacing.stackGap),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_nav_walks),
            contentDescription = null,
            tint = FieldKit.colors.iconMuted,
            modifier = Modifier.size(EmptyIconSize).testTag(HISTORY_EMPTY_ICON_TAG),
        )
        Text(
            text = stringResource(R.string.history_empty),
            style = FieldKit.typography.factorValue,
            color = FieldKit.colors.onSurface,
            textAlign = TextAlign.Center,
        )
    }
}

private fun LazyListScope.weekSection(
    week: WeekGroup,
    texts: RowTexts,
    onOpenWalk: (Long) -> Unit,
    onRequestDelete: (WalkSummary) -> Unit,
) {
    item(key = "week-${week.weekStart}") { WeekHeader(week, texts.dateText) }
    itemsIndexed(week.walks, key = { _, walk -> walk.id }) { index, walk ->
        WalkRow(
            walk = walk,
            texts = texts,
            position = RowPosition.of(index, week.walks.size),
            onOpenWalk = onOpenWalk,
            onRequestDelete = onRequestDelete,
        )
    }
}

@Composable
private fun WeekHeader(
    week: WeekGroup,
    dateText: WalkDateText,
) {
    val colors = FieldKit.colors
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = FieldKitSpacing.sectionGap, bottom = SectionGap)
                .padding(horizontal = SectionHeaderSide),
        horizontalArrangement = Arrangement.spacedBy(SectionGap),
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            text = dateText.weekRange(week.weekStart, week.weekEnd),
            style = FieldKit.typography.label.copy(fontWeight = FontWeight.Bold),
            color = colors.onSurfaceVariant,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        Text(
            text =
                stringResource(
                    R.string.history_totals_dotted,
                    pluralStringResource(R.plurals.history_walk_count, week.count, week.count),
                    DurationFormat.hoursMinutes(week.totalSeconds),
                    DistanceFormat.miles(week.totalM),
                ),
            style = FieldKit.typography.label,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f, fill = false),
        )
    }
}

/** Where a row sits in its week's card, so the card's corners can be drawn row by row in the lazy list. */
private enum class RowPosition(
    val top: Boolean,
    val bottom: Boolean,
) {
    ONLY(true, true),
    FIRST(true, false),
    MIDDLE(false, false),
    LAST(false, true),
    ;

    fun shape() =
        RoundedCornerShape(
            topStart = if (top) CardRadius else 0.dp,
            topEnd = if (top) CardRadius else 0.dp,
            bottomStart = if (bottom) CardRadius else 0.dp,
            bottomEnd = if (bottom) CardRadius else 0.dp,
        )

    companion object {
        fun of(
            index: Int,
            count: Int,
        ): RowPosition =
            when {
                count == 1 -> ONLY
                index == 0 -> FIRST
                index == count - 1 -> LAST
                else -> MIDDLE
            }
    }
}

@Composable
private fun WalkRow(
    walk: WalkSummary,
    texts: RowTexts,
    position: RowPosition,
    onOpenWalk: (Long) -> Unit,
    onRequestDelete: (WalkSummary) -> Unit,
) {
    val colors = FieldKit.colors
    val date = texts.dateText.rowDate(walk.startUtc)
    val time = texts.dateText.time(walk.startUtc)
    val duration = DurationFormat.hoursMinutes(walk.durationSeconds)
    val miles = DistanceFormat.miles(walk.distanceM)
    val spoken = stringResource(R.string.history_row_spoken, date, time, duration, miles)
    val openLabel = stringResource(R.string.history_open_walk)
    val shape = position.shape()
    Column(modifier = Modifier.fillMaxWidth().background(colors.surface, shape)) {
        // The row is one control spoken as "<date> at <time>, <h:mm>, <x.x> mi" that opens the walk (AC39). "More"
        // stays a separate control inside it (AC44).
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = FieldKitSizes.listRowMinHeight)
                    .clickable(onClickLabel = openLabel, role = Role.Button) { onOpenWalk(walk.id) }
                    .semantics(mergeDescendants = true) { contentDescription = spoken }
                    .padding(start = RowPaddingHorizontal, top = RowPaddingVertical, bottom = RowPaddingVertical),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RowPaddingHorizontal),
        ) {
            Box(
                modifier = Modifier.size(RowTileSize).background(colors.primaryContainer, FieldKitShapes.iconTile),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_nav_walks),
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(FieldKitSizes.iconNav),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(date, style = FieldKit.typography.bodyLarge.copy(fontSize = RowTitleSize, fontWeight = FontWeight.Bold))
                Text(
                    stringResource(R.string.history_walk_at, time),
                    style = FieldKit.typography.label.copy(fontWeight = FontWeight.Medium),
                    color = colors.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    stringResource(R.string.recording_distance_miles, miles),
                    style =
                        FieldKit.typography.bodyLarge.copy(
                            fontSize = RowTitleSize,
                            fontWeight = FontWeight.Bold,
                            fontFeatureSettings = "tnum",
                        ),
                )
                Text(
                    duration,
                    style = FieldKit.typography.label.copy(fontWeight = FontWeight.Medium, fontFeatureSettings = "tnum"),
                    color = colors.onSurfaceVariant,
                )
            }
            Icon(
                painter = painterResource(R.drawable.ic_chevron_right),
                contentDescription = null,
                tint = colors.iconMuted,
                modifier = Modifier.size(FieldKitSizes.iconInline),
            )
            RowMoreMenu(walk, texts.formats, onRequestDelete)
        }
        if (!position.bottom) HorizontalDivider(color = colors.divider, modifier = Modifier.padding(horizontal = RowPaddingHorizontal))
    }
}

@Composable
private fun RowMoreMenu(
    walk: WalkSummary,
    formats: WalkDateFormats,
    onRequestDelete: (WalkSummary) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    // The delete label keeps the year format (AC44): "Delete walk on Mon 19 Oct 2026 at 08:00".
    val description = stringResource(R.string.history_delete_description, formats.date(walk), formats.time(walk))
    Box {
        IconButton(onClick = { open = true }, modifier = Modifier.size(FieldKitSizes.iconButton)) {
            Icon(
                painter = painterResource(R.drawable.ic_more),
                contentDescription = stringResource(R.string.history_more),
                tint = FieldKit.colors.iconMuted,
            )
        }
        // The menu sits on row cards of the same surface colour, so a 1 dp control-border edge (iconMuted, at least
        // 3:1 on the card in both themes) keeps it distinct, mainly in dark mode (owner_answers.t6_dark_menu).
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            containerColor = FieldKit.colors.surface,
            border = BorderStroke(MenuBorderWidth, FieldKit.colors.controlBorder),
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.history_delete)) },
                leadingIcon = { Icon(painterResource(R.drawable.ic_delete), contentDescription = null) },
                onClick = {
                    open = false
                    onRequestDelete(walk)
                },
                modifier = Modifier.semantics { contentDescription = description },
            )
        }
    }
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
