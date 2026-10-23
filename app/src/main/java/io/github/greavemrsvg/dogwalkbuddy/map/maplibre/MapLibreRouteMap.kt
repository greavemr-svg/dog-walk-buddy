package io.github.greavemrsvg.dogwalkbuddy.map.maplibre

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import io.github.greavemrsvg.dogwalkbuddy.domain.location.LatLon
import io.github.greavemrsvg.dogwalkbuddy.map.MapConfig
import io.github.greavemrsvg.dogwalkbuddy.map.RouteBounds
import io.github.greavemrsvg.dogwalkbuddy.map.RouteGeoJson
import io.github.greavemrsvg.dogwalkbuddy.map.RouteMap
import io.github.greavemrsvg.dogwalkbuddy.map.RouteStyle
import okhttp3.OkHttpClient
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.module.http.HttpRequestUtil
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource

/**
 * Draws the route with MapLibre Native on the OpenFreeMap style (ADR 0003). It is excluded from coverage and
 * checked by hand (AC38, AC45). The route line is built on the device from the stored points: only the style and the
 * tiles are downloaded, through the app's own HTTP client, and nothing about the route is logged.
 */
class MapLibreRouteMap(
    private val appContext: Context,
    private val httpClient: OkHttpClient,
) : RouteMap {
    @Composable
    override fun Draw(
        route: List<LatLon>,
        onLoadFailed: () -> Unit,
        modifier: Modifier,
    ) {
        val context = LocalContext.current
        val lifecycle = LocalLifecycleOwner.current.lifecycle
        val latestOnLoadFailed by rememberUpdatedState(onLoadFailed)
        val mapView =
            remember(route) {
                MapLibreRuntime.ensureInitialised(appContext, httpClient)
                MapView(context).also { view ->
                    view.addOnDidFailLoadingMapListener { latestOnLoadFailed() }
                    view.getMapAsync { map ->
                        map.setStyle(Style.Builder().fromUri(MapConfig.STYLE_URL)) { style -> showRoute(map, style, route, view) }
                    }
                }
            }
        DisposableEffect(mapView, lifecycle) {
            val observer =
                LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_CREATE -> mapView.onCreate(null)
                        Lifecycle.Event.ON_START -> mapView.onStart()
                        Lifecycle.Event.ON_RESUME -> mapView.onResume()
                        Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                        Lifecycle.Event.ON_STOP -> mapView.onStop()
                        Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                        else -> Unit
                    }
                }
            // Adding the observer replays the lifecycle up to its current state, so the map is created and started.
            lifecycle.addObserver(observer)
            onDispose {
                lifecycle.removeObserver(observer)
                // The screen left while the activity lives on: take the map down the rest of the way.
                val state = lifecycle.currentState
                if (state.isAtLeast(Lifecycle.State.RESUMED)) mapView.onPause()
                if (state.isAtLeast(Lifecycle.State.STARTED)) mapView.onStop()
                if (state != Lifecycle.State.DESTROYED) mapView.onDestroy()
            }
        }
        AndroidView(factory = { mapView }, modifier = modifier)
    }

    private fun showRoute(
        map: org.maplibre.android.maps.MapLibreMap,
        style: Style,
        route: List<LatLon>,
        view: MapView,
    ) {
        if (route.isEmpty()) return
        style.addSource(GeoJsonSource(ROUTE_SOURCE_ID, RouteGeoJson.lineString(route)))
        // A light casing under the line keeps the route readable on any tile colour (AC45).
        style.addLayer(
            LineLayer(ROUTE_CASING_LAYER_ID, ROUTE_SOURCE_ID).withProperties(
                PropertyFactory.lineColor(RouteStyle.CASING_COLOUR),
                PropertyFactory.lineWidth(RouteStyle.CASING_WIDTH_DP),
                PropertyFactory.lineCap("round"),
                PropertyFactory.lineJoin("round"),
            ),
        )
        style.addLayer(
            LineLayer(ROUTE_LAYER_ID, ROUTE_SOURCE_ID).withProperties(
                PropertyFactory.lineColor(RouteStyle.LINE_COLOUR),
                PropertyFactory.lineWidth(RouteStyle.LINE_WIDTH_DP),
                PropertyFactory.lineCap("round"),
                PropertyFactory.lineJoin("round"),
            ),
        )
        style.addSource(GeoJsonSource(START_SOURCE_ID, RouteStyle.startMarkerGeoJson(route)))
        style.addLayer(
            CircleLayer(START_LAYER_ID, START_SOURCE_ID).withProperties(
                PropertyFactory.circleColor(RouteStyle.START_MARKER_FILL),
                PropertyFactory.circleRadius(RouteStyle.START_MARKER_RADIUS_DP),
                PropertyFactory.circleStrokeColor(RouteStyle.START_MARKER_STROKE),
                PropertyFactory.circleStrokeWidth(RouteStyle.START_MARKER_STROKE_WIDTH_DP),
            ),
        )
        val bounds = RouteBounds.of(route) ?: return
        val box = LatLngBounds.from(bounds.north, bounds.east, bounds.south, bounds.west)
        val padding = (FIT_PADDING_DP * view.resources.displayMetrics.density).toInt()
        map.moveCamera(CameraUpdateFactory.newLatLngBounds(box, padding))
    }

    private companion object {
        const val ROUTE_SOURCE_ID = "walk-route"
        const val ROUTE_LAYER_ID = "walk-route-line"
        const val ROUTE_CASING_LAYER_ID = "walk-route-casing"
        const val START_SOURCE_ID = "walk-route-start"
        const val START_LAYER_ID = "walk-route-start-marker"
        const val FIT_PADDING_DP = 32
    }
}

/** Starts MapLibre once, with the app's HTTP client, the first time a map or the cache needs it. */
internal object MapLibreRuntime {
    private var initialised = false

    @Synchronized
    fun ensureInitialised(
        context: Context,
        httpClient: OkHttpClient,
    ) {
        if (initialised) return
        // No API key and no account: the OpenFreeMap style needs none.
        start(
            startMapLibre = { MapLibre.getInstance(context.applicationContext) },
            setHttpClient = { HttpRequestUtil.setOkHttpClient(httpClient) },
        )
        initialised = true
    }

    /** MapLibre must be started before its HTTP client is set: the client's class initialiser needs the instance. */
    internal fun start(
        startMapLibre: () -> Unit,
        setHttpClient: () -> Unit,
    ) {
        startMapLibre()
        setHttpClient()
    }
}
