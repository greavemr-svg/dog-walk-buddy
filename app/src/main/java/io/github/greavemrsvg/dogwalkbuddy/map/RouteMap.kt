package io.github.greavemrsvg.dogwalkbuddy.map

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.greavemrsvg.dogwalkbuddy.domain.location.LatLon

/**
 * The slot that draws a route on a street map, zoomed to fit it (AC38). The production implementation is the
 * MapLibre adapter (ADR 0003). Tests supply a fake, because the real map cannot run on the JVM.
 */
fun interface RouteMap {
    /** Draws [route]. [onLoadFailed] is called if the map cannot load, so the screen can show a message (AC39). */
    @Composable
    fun Draw(
        route: List<LatLon>,
        onLoadFailed: () -> Unit,
        modifier: Modifier,
    )
}
