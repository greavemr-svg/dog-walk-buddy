package io.github.greavemrsvg.dogwalkbuddy.map

import okhttp3.OkHttpClient
import java.time.Duration

/**
 * The single place that names the map tile source (ADR 0003, spec Assumption 13). To change provider, change
 * [STYLE_URL] and the `map_attribution` string.
 */
object MapConfig {
    const val STYLE_URL = "https://tiles.openfreemap.org/styles/liberty"

    /**
     * The HTTP client the map uses: the app's client, so it keeps the HTTPS-only checks and the app User-Agent,
     * and sends no other identifier. It has no HTTP cache of its own, because the map keeps its own tile cache
     * (which "Delete all my data" clears) and map tiles must not fill the weather cache.
     */
    fun httpClient(appClient: OkHttpClient): OkHttpClient =
        appClient
            .newBuilder()
            .cache(null)
            // Style and tile downloads are not bounded by the 10 s limit of the weather call.
            .callTimeout(Duration.ZERO)
            .build()
}
