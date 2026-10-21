package io.github.greavemrsvg.dogwalkbuddy.ui.walkdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.greavemrsvg.dogwalkbuddy.R
import io.github.greavemrsvg.dogwalkbuddy.domain.walk.DistanceFormat
import io.github.greavemrsvg.dogwalkbuddy.domain.walk.DurationFormat
import io.github.greavemrsvg.dogwalkbuddy.map.RouteMap
import io.github.greavemrsvg.dogwalkbuddy.ui.history.WalkDateFormats
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.MinTouchTarget

/** Test tag of the area where the map, or the message that replaces it, is shown. */
const val WALK_MAP_AREA_TAG = "walk_map_area"
const val WALK_MAP_ATTRIBUTION_TAG = "walk_map_attribution"

private val MapHeight = 360.dp
private const val ATTRIBUTION_MAX_WIDTH_FRACTION = 0.6f

/**
 * One saved walk: its date, duration and distance, which are always shown, and the route on a street map with its
 * attribution (AC38). Where the map would be, offline or when it fails to load, "Map not available offline"
 * is shown instead (AC39).
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
    WalkDetailContent(state, routeMap, formats, onBack, viewModel::onMapLoadFailed, modifier)
}

@Composable
fun WalkDetailContent(
    state: WalkDetailUiState,
    routeMap: RouteMap,
    formats: WalkDateFormats,
    onBack: () -> Unit,
    onMapLoadFailed: () -> Unit,
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TextButton(onClick = onBack, modifier = Modifier.heightIn(min = MinTouchTarget)) {
                Text(stringResource(R.string.walk_detail_back))
            }
            Text(
                text = stringResource(R.string.walk_detail_title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.semantics { heading() },
            )
            when (state) {
                WalkDetailUiState.Loading -> Unit
                WalkDetailUiState.NotFound -> Text(stringResource(R.string.walk_detail_not_found))
                WalkDetailUiState.LoadFailed -> Text(stringResource(R.string.walk_detail_load_failed))
                is WalkDetailUiState.Loaded -> {
                    WalkFacts(state, formats)
                    MapArea(state, routeMap, onMapLoadFailed)
                }
            }
        }
    }
}

@Composable
private fun WalkFacts(
    state: WalkDetailUiState.Loaded,
    formats: WalkDateFormats,
) {
    val walk = state.walk
    Column(modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}) {
        Text(
            stringResource(R.string.walk_detail_when, formats.date(walk), formats.time(walk)),
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            stringResource(R.string.walk_detail_duration, DurationFormat.hoursMinutes(walk.durationSeconds)),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            stringResource(R.string.walk_detail_distance, DistanceFormat.miles(walk.distanceM)),
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Composable
private fun MapArea(
    state: WalkDetailUiState.Loaded,
    routeMap: RouteMap,
    onMapLoadFailed: () -> Unit,
) {
    when {
        state.route.isEmpty() -> Text(stringResource(R.string.walk_detail_no_route), modifier = Modifier.testTag(WALK_MAP_AREA_TAG))
        !state.mapAvailable ->
            Text(
                text = stringResource(R.string.walk_detail_map_unavailable),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.testTag(WALK_MAP_AREA_TAG),
            )
        else -> {
            val description = stringResource(R.string.walk_detail_map_description)
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = MapHeight)
                        .testTag(WALK_MAP_AREA_TAG),
            ) {
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
                        color = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ) {
                        Text(
                            text = stringResource(R.string.map_attribution),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }
            }
        }
    }
}
