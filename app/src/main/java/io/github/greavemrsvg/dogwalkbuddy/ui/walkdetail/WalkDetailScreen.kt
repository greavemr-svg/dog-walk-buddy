package io.github.greavemrsvg.dogwalkbuddy.ui.walkdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.greavemrsvg.dogwalkbuddy.R
import io.github.greavemrsvg.dogwalkbuddy.domain.walk.DistanceFormat
import io.github.greavemrsvg.dogwalkbuddy.domain.walk.DurationFormat
import io.github.greavemrsvg.dogwalkbuddy.map.RouteMap
import io.github.greavemrsvg.dogwalkbuddy.ui.common.BackButton
import io.github.greavemrsvg.dogwalkbuddy.ui.common.ConfirmSheet
import io.github.greavemrsvg.dogwalkbuddy.ui.common.DestructiveOutlinedButton
import io.github.greavemrsvg.dogwalkbuddy.ui.common.ErrorSnackbarEffect
import io.github.greavemrsvg.dogwalkbuddy.ui.common.StatCard
import io.github.greavemrsvg.dogwalkbuddy.ui.common.StatCardColors
import io.github.greavemrsvg.dogwalkbuddy.ui.history.WalkDateFormats
import io.github.greavemrsvg.dogwalkbuddy.ui.history.WalkDateText
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.FieldKit
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.FieldKitSpacing
import java.time.Clock

/** Test tag of the area where the map, or the message that replaces it, is shown. */
const val WALK_MAP_AREA_TAG = "walk_map_area"
const val WALK_MAP_ATTRIBUTION_TAG = "walk_map_attribution"

/** Test tag of the sheet below the map that holds the date, the time range, the cards and the delete button. */
const val WALK_DETAIL_SHEET_TAG = "walk_detail_sheet"

/** The map's full height at the default size, drawn under the status bar (AC45). */
private val MapHeight = 470.dp

/**
 * How far the sheet overlaps the map (AC46). The map is drawn only above the overlap, so MapLibre's logo and "i"
 * button and the app's attribution stay visible above the sheet (owner decision t7_map_attribution).
 */
private val SheetOverlap = 28.dp
private val SheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
private val SheetTopPadding = 24.dp
private val SheetBottomPadding = 24.dp
private val BackInset = 12.dp
private val MapOffIconSize = 40.dp
private val HeadingSize = 28.sp
private val AttributionSize = 11.sp
private const val ATTRIBUTION_MAX_WIDTH_FRACTION = 0.6f
private const val ATTRIBUTION_BACKGROUND_ALPHA = 0.9f

/**
 * One saved walk (AC45-AC49): the route on an edge-to-edge street map with a floating back control and the map
 * attribution, then a sheet with the date heading, the time range, the duration and distance cards and "Delete this
 * walk". Offline, or when the map fails, the map area says "Map not available offline" instead. Once the walk is
 * deleted the screen goes back to Walks through [onBack].
 */
@Composable
fun WalkDetailRoute(
    viewModel: WalkDetailViewModel,
    routeMap: RouteMap,
    formats: WalkDateFormats,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state = viewModel.state.collectAsStateWithLifecycle().value
    val deletion = viewModel.deletion.collectAsStateWithLifecycle().value
    WalkDetailContent(
        state = state,
        routeMap = routeMap,
        formats = formats,
        onBack = onBack,
        onMapLoadFailed = viewModel::onMapLoadFailed,
        modifier = modifier,
        clock = viewModel.clock,
        deletion = deletion,
        onRequestDelete = viewModel::requestDelete,
        onConfirmDelete = viewModel::confirmDelete,
        onCancelDelete = viewModel::cancelDelete,
        onDismissDeleteFailure = viewModel::dismissDeleteFailure,
    )
}

/** The Walk detail content. [clock] decides the current year of the date heading (D8). */
@Composable
fun WalkDetailContent(
    state: WalkDetailUiState,
    routeMap: RouteMap,
    formats: WalkDateFormats,
    onBack: () -> Unit,
    onMapLoadFailed: () -> Unit,
    modifier: Modifier = Modifier,
    clock: Clock = Clock.systemDefaultZone(),
    deletion: WalkDeletionState = WalkDeletionState(),
    onRequestDelete: () -> Unit = {},
    onConfirmDelete: () -> Unit = {},
    onCancelDelete: () -> Unit = {},
    onDismissDeleteFailure: () -> Unit = {},
) {
    val snackbarHost = remember { SnackbarHostState() }
    ErrorSnackbarEffect(
        host = snackbarHost,
        show = deletion.failed,
        message = stringResource(R.string.history_delete_failed),
        dismissLabel = stringResource(R.string.history_delete_failed_dismiss),
        onDismissed = onDismissDeleteFailure,
    )
    // The walk is gone: return to Walks, where the Room flow has already updated the totals (AC48).
    LaunchedEffect(deletion.deleted) { if (deletion.deleted) onBack() }
    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0),
        containerColor = FieldKit.colors.background,
        snackbarHost = { SnackbarHost(snackbarHost, modifier = Modifier.safeDrawingPadding()) },
    ) { scaffoldPadding ->
        val back = stringResource(R.string.walk_detail_back)
        when (state) {
            is WalkDetailUiState.Loaded ->
                LoadedWalk(
                    state = state,
                    routeMap = routeMap,
                    formats = formats,
                    clock = clock,
                    onBack = onBack,
                    onMapLoadFailed = onMapLoadFailed,
                    onRequestDelete = onRequestDelete,
                    modifier = Modifier.padding(scaffoldPadding),
                )
            else ->
                Column(
                    modifier =
                        Modifier
                            .padding(scaffoldPadding)
                            .fillMaxSize()
                            .safeDrawingPadding()
                            .verticalScroll(rememberScrollState())
                            .padding(FieldKitSpacing.gutter),
                    verticalArrangement = Arrangement.spacedBy(FieldKitSpacing.stackGap),
                ) {
                    BackButton(onClick = onBack, contentDescription = back, floating = true)
                    when (state) {
                        WalkDetailUiState.NotFound -> Message(stringResource(R.string.walk_detail_not_found))
                        WalkDetailUiState.LoadFailed -> Message(stringResource(R.string.walk_detail_load_failed))
                        else -> Unit
                    }
                }
        }
    }
    if (state is WalkDetailUiState.Loaded && deletion.confirming) {
        // The confirmation keeps the MVP title, message (with the year) and buttons (AC48, K11).
        ConfirmSheet(
            title = stringResource(R.string.history_delete_title),
            message = stringResource(R.string.history_delete_message, formats.date(state.walk), formats.time(state.walk)),
            confirmLabel = stringResource(R.string.history_delete_confirm),
            cancelLabel = stringResource(R.string.history_delete_cancel),
            onConfirm = onConfirmDelete,
            onCancel = onCancelDelete,
        )
    }
}

@Composable
private fun Message(text: String) {
    Text(text = text, style = FieldKit.typography.bodyLarge, color = FieldKit.colors.onSurface)
}

@Composable
private fun LoadedWalk(
    state: WalkDetailUiState.Loaded,
    routeMap: RouteMap,
    formats: WalkDateFormats,
    clock: Clock,
    onBack: () -> Unit,
    onMapLoadFailed: () -> Unit,
    onRequestDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // The whole screen scrolls, so nothing is cut off at large font sizes (AC60).
    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        // Offline at large font sizes the message may need more than the map's height, so the area grows.
        val areaHeight = if (state.route.isEmpty() || !state.mapAvailable) Modifier.heightIn(min = MapHeight) else Modifier.height(MapHeight)
        Box(modifier = Modifier.fillMaxWidth()) {
            MapArea(state, routeMap, onMapLoadFailed, Modifier.fillMaxWidth().then(areaHeight).padding(bottom = SheetOverlap))
            BackButton(
                onClick = onBack,
                contentDescription = stringResource(R.string.walk_detail_back),
                floating = true,
                modifier = Modifier.statusBarsPadding().padding(start = FieldKitSpacing.gutter, top = BackInset),
            )
        }
        WalkSheet(state, formats, clock, onRequestDelete, Modifier.overlapAbove(SheetOverlap))
    }
}

/** Draws this element [overlap] higher, over the bottom of the element above it, and takes that much less height. */
private fun Modifier.overlapAbove(overlap: Dp): Modifier =
    layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        val shift = overlap.roundToPx()
        layout(placeable.width, (placeable.height - shift).coerceAtLeast(0)) { placeable.place(0, -shift) }
    }

@Composable
private fun WalkSheet(
    state: WalkDetailUiState.Loaded,
    formats: WalkDateFormats,
    clock: Clock,
    onRequestDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = FieldKit.colors
    val walk = state.walk
    val rangeTemplate = stringResource(R.string.walk_detail_time_range)
    val dateText = remember(formats, clock, rangeTemplate) { WalkDateText(formats.zoneId, clock, formats.is24Hour, rangeTemplate) }
    val end = walk.startUtc.plusSeconds(walk.durationSeconds)
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(colors.background, SheetShape)
                .testTag(WALK_DETAIL_SHEET_TAG)
                .navigationBarsPadding()
                .padding(horizontal = FieldKitSpacing.headerSide)
                .padding(top = SheetTopPadding, bottom = SheetBottomPadding),
        verticalArrangement = Arrangement.spacedBy(FieldKitSpacing.stackGap),
    ) {
        Text(
            text = dateText.fullDate(walk.startUtc),
            style = FieldKit.typography.screenTitle.copy(fontSize = HeadingSize),
            color = colors.onSurface,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = dateText.timeRange(walk.startUtc, end),
            style = FieldKit.typography.bodyLarge,
            color = colors.onSurfaceVariant,
        )
        val cardColors = StatCardColors(container = colors.surface, caption = colors.onSurfaceVariant, value = colors.onSurface)
        Row(horizontalArrangement = Arrangement.spacedBy(FieldKitSpacing.stackGap)) {
            StatCard(
                caption = stringResource(R.string.walk_detail_duration_caption),
                value = AnnotatedString(DurationFormat.hoursMinutes(walk.durationSeconds)),
                colors = cardColors,
                modifier = Modifier.weight(1f),
            )
            StatCard(
                caption = stringResource(R.string.walk_detail_distance_caption),
                value = AnnotatedString(stringResource(R.string.walk_detail_distance_value, DistanceFormat.miles(walk.distanceM))),
                colors = cardColors,
                modifier = Modifier.weight(1f),
            )
        }
        DestructiveOutlinedButton(
            label = stringResource(R.string.walk_detail_delete),
            onClick = onRequestDelete,
            modifier = Modifier.padding(top = FieldKitSpacing.stackGap),
        )
    }
}

@Composable
private fun MapArea(
    state: WalkDetailUiState.Loaded,
    routeMap: RouteMap,
    onMapLoadFailed: () -> Unit,
    modifier: Modifier,
) {
    val colors = FieldKit.colors
    when {
        state.route.isEmpty() ->
            Box(modifier.background(colors.surfaceMuted).testTag(WALK_MAP_AREA_TAG), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.walk_detail_no_route),
                    style = FieldKit.typography.bodyLarge,
                    color = colors.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(FieldKitSpacing.gutter),
                )
            }
        !state.mapAvailable -> MapUnavailable(modifier)
        else -> {
            val description = stringResource(R.string.walk_detail_map_description)
            Box(modifier = modifier.testTag(WALK_MAP_AREA_TAG)) {
                routeMap.Draw(
                    route = state.route,
                    onLoadFailed = onMapLoadFailed,
                    modifier = Modifier.matchParentSize().semantics { contentDescription = description },
                )
                // The attribution the tile licences require, drawn by the app so it is always visible and readable.
                // It sits at the end and takes at most 60% of the width, so MapLibre's own logo and attribution button
                // (bottom-start) stay visible and are never covered; long text wraps instead of growing leftwards.
                Row(
                    modifier =
                        Modifier
                            .align(Alignment.BottomEnd)
                            .fillMaxWidth(ATTRIBUTION_MAX_WIDTH_FRACTION)
                            .testTag(WALK_MAP_ATTRIBUTION_TAG),
                    horizontalArrangement = Arrangement.End,
                ) {
                    Surface(
                        color = colors.surface.copy(alpha = ATTRIBUTION_BACKGROUND_ALPHA),
                        contentColor = colors.onSurface,
                    ) {
                        Text(
                            text = stringResource(R.string.map_attribution),
                            style = FieldKit.typography.caption.copy(fontSize = AttributionSize),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }
            }
        }
    }
}

/** Offline or after a map failure: the map area says so, with the C17 sub-line (AC47). */
@Composable
private fun MapUnavailable(modifier: Modifier) {
    val colors = FieldKit.colors
    Column(
        modifier =
            modifier
                .background(colors.surfaceMuted)
                .testTag(WALK_MAP_AREA_TAG)
                .statusBarsPadding()
                .padding(FieldKitSpacing.gutter),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_map_off),
            contentDescription = null,
            tint = colors.iconMuted,
            modifier = Modifier.size(MapOffIconSize),
        )
        Text(
            text = stringResource(R.string.walk_detail_map_unavailable),
            style = FieldKit.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            color = colors.onSurface,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.walk_detail_map_unavailable_sub),
            style = FieldKit.typography.body,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
