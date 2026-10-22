package io.github.greavemrsvg.dogwalkbuddy.testutil

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import io.github.greavemrsvg.dogwalkbuddy.data.net.ConnectivityStatus
import io.github.greavemrsvg.dogwalkbuddy.domain.location.LatLon
import io.github.greavemrsvg.dogwalkbuddy.map.MapCacheClearer
import io.github.greavemrsvg.dogwalkbuddy.map.RouteBounds
import io.github.greavemrsvg.dogwalkbuddy.map.RouteMap

/** Test tag of the fake map. */
const val FAKE_MAP_TAG = "fake_map"

/** A map that draws nothing but records the route it was given and the bounds a real map would fit (ADR 0003). */
class FakeRouteMap : RouteMap {
    @Volatile
    var drawCount = 0
        private set

    @Volatile
    var lastRoute: List<LatLon>? = null
        private set

    @Volatile
    var lastBounds: RouteBounds? = null
        private set

    @Volatile
    private var failureCallback: (() -> Unit)? = null

    /** Makes the map report that it could not load, as MapLibre does when tiles cannot be fetched. */
    fun failLoading() {
        checkNotNull(failureCallback) { "The map is not shown" }.invoke()
    }

    @Composable
    override fun Draw(
        route: List<LatLon>,
        onLoadFailed: () -> Unit,
        modifier: Modifier,
    ) {
        drawCount++
        lastRoute = route
        lastBounds = RouteBounds.of(route)
        failureCallback = onLoadFailed
        Box(modifier = modifier.testTag(FAKE_MAP_TAG))
    }
}

class FakeConnectivity(
    var online: Boolean = true,
) : ConnectivityStatus {
    override fun isOnline(): Boolean = online
}

class FakeMapCacheClearer : MapCacheClearer {
    var clearCount = 0
        private set
    var failure: Exception? = null

    override suspend fun clear() {
        failure?.let { throw it }
        clearCount++
    }
}
