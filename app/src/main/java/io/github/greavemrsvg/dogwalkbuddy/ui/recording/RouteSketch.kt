package io.github.greavemrsvg.dogwalkbuddy.ui.recording

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import io.github.greavemrsvg.dogwalkbuddy.R
import io.github.greavemrsvg.dogwalkbuddy.domain.location.LatLon
import io.github.greavemrsvg.dogwalkbuddy.domain.walk.PanelPoint
import io.github.greavemrsvg.dogwalkbuddy.domain.walk.RouteProjection
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.FieldKit
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.FieldKitShapes
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.RecordingColors

/** Test tag of the route panel. */
const val ROUTE_SKETCH_TAG = "recording_route_sketch"

/** The projected points the route panel draws, in panel pixels. Lets tests check the drawing without pixels. */
val RouteSketchPoints = SemanticsPropertyKey<List<PanelPoint>>("RouteSketchPoints")
private var SemanticsPropertyReceiver.routeSketchPoints by RouteSketchPoints

private val PanelHeight = 200.dp
private val PanelBorder = 1.dp

/** Keeps the start ring and the current marker's halo inside the panel. */
private val PanelPadding = 24.dp
private val CasingWidth = 8.dp
private val LineWidth = 4.5.dp
private val StartRingRadius = 6.dp
private val StartRingWidth = 3.dp
private val CurrentDotRadius = 6.dp
private val CurrentHaloRadius = 11.dp
private const val HALO_ALPHA = 0.25f

/** The fewest fixes that make a line. With fewer the panel stays empty (AC35). */
private const val MIN_LINE_POINTS = 2

/**
 * The live route line (AC35): the walk's fixes as one line scaled to fit the panel, with a start ring at the first
 * fix and the current position at the last. It is drawn locally, with no map tiles and no network (MVP AC49).
 */
@Composable
fun RouteSketch(
    route: List<LatLon>,
    modifier: Modifier = Modifier,
) {
    val palette = FieldKit.recording
    val density = LocalDensity.current
    var size by remember { mutableStateOf(IntSize.Zero) }
    val points =
        remember(route, size) {
            if (route.size < MIN_LINE_POINTS) {
                emptyList()
            } else {
                val padding = with(density) { PanelPadding.toPx() }
                RouteProjection.fit(route, size.width.toFloat(), size.height.toFloat(), padding)
            }
        }
    val label = stringResource(R.string.recording_route_label)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(PanelHeight)
                    .background(palette.mapPanel, FieldKitShapes.card)
                    .border(PanelBorder, palette.mapPanelBorder, FieldKitShapes.card),
        ) {
            Canvas(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .onSizeChanged { size = it }
                        .testTag(ROUTE_SKETCH_TAG)
                        .semantics {
                            contentDescription = label
                            routeSketchPoints = points
                        },
            ) {
                drawRoute(points, palette)
            }
        }
        Text(
            text = stringResource(R.string.recording_route_caption),
            style = FieldKit.typography.caption,
            color = palette.onDarkMuted,
        )
    }
}

private fun DrawScope.drawRoute(
    points: List<PanelPoint>,
    palette: RecordingColors,
) {
    if (points.size < MIN_LINE_POINTS) return
    val offsets = points.map { Offset(it.x, it.y) }
    val path =
        Path().apply {
            moveTo(offsets.first().x, offsets.first().y)
            offsets.drop(1).forEach { lineTo(it.x, it.y) }
        }
    drawPath(path, palette.routeCasing, style = Stroke(CasingWidth.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    drawPath(path, palette.route, style = Stroke(LineWidth.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    val start = offsets.first()
    drawCircle(palette.background, StartRingRadius.toPx(), start)
    drawCircle(palette.route, StartRingRadius.toPx(), start, style = Stroke(StartRingWidth.toPx()))
    val current = offsets.last()
    drawCircle(palette.route.copy(alpha = HALO_ALPHA), CurrentHaloRadius.toPx(), current)
    drawCircle(palette.onDark, CurrentDotRadius.toPx(), current)
}
