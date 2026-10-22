package io.github.greavemrsvg.dogwalkbuddy.domain.walk

import io.github.greavemrsvg.dogwalkbuddy.domain.location.LatLon
import kotlin.math.cos
import kotlin.math.min

/** A projected route point in panel pixels, with y growing downwards. */
data class PanelPoint(
    val x: Float,
    val y: Float,
)

/**
 * Fits a recorded route into a panel for the live route line (AC35). An equirectangular projection, with longitude
 * scaled by the cosine of the mean latitude, is accurate enough over a walk. The route keeps its shape, is scaled
 * uniformly to fit inside the padding, is centred, and has north up. Pure Kotlin.
 */
object RouteProjection {
    /**
     * Projects [points] into a [width] x [height] panel, keeping [padding] clear on every side. The first result is
     * the start marker and the last is the current marker. A route with a single position (or no extent) is drawn at
     * the centre.
     */
    fun fit(
        points: List<LatLon>,
        width: Float,
        height: Float,
        padding: Float,
    ): List<PanelPoint> {
        require(width >= 0f && height >= 0f) { "Panel size must not be negative" }
        require(padding >= 0f) { "Padding must not be negative" }
        if (points.isEmpty()) return emptyList()
        val xScale = cos(Math.toRadians(points.map { it.latitude }.average()))
        val xs = points.map { it.longitude * xScale }
        val ys = points.map { it.latitude }
        val minX = xs.min()
        val maxY = ys.max()
        val spanX = xs.max() - minX
        val spanY = maxY - ys.min()
        val usableWidth = (width - 2 * padding).coerceAtLeast(0f).toDouble()
        val usableHeight = (height - 2 * padding).coerceAtLeast(0f).toDouble()
        val scale =
            when {
                spanX > 0 && spanY > 0 -> min(usableWidth / spanX, usableHeight / spanY)
                spanX > 0 -> usableWidth / spanX
                spanY > 0 -> usableHeight / spanY
                else -> 0.0
            }
        val left = (width - spanX * scale) / 2
        val top = (height - spanY * scale) / 2
        return xs.indices.map { index ->
            PanelPoint(
                x = (left + (xs[index] - minX) * scale).toFloat(),
                y = (top + (maxY - ys[index]) * scale).toFloat(),
            )
        }
    }
}
