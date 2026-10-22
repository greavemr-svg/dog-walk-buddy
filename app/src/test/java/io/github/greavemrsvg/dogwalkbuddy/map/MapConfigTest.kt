package io.github.greavemrsvg.dogwalkbuddy.map

import io.github.greavemrsvg.dogwalkbuddy.data.weather.MetNorwayClient
import io.github.greavemrsvg.dogwalkbuddy.testutil.StubWeatherServer
import mockwebserver3.MockResponse
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException

/** The tile source and the HTTP client the map uses (ADR 0003, AC49). */
class MapConfigTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var stub: StubWeatherServer

    @Before
    fun setUp() {
        stub = StubWeatherServer(temporaryFolder.newFolder())
    }

    @After
    fun tearDown() {
        stub.close()
    }

    @Test
    fun AC49_the_only_style_is_served_over_https_from_the_openfreemap_host() {
        val url = MapConfig.STYLE_URL.toHttpUrl()

        assertEquals("https", url.scheme)
        assertEquals("tiles.openfreemap.org", url.host)
    }

    @Test
    fun the_map_client_has_no_http_cache_so_tiles_never_fill_the_weather_cache() {
        assertNotNull(stub.httpClient.cache)

        assertNull(MapConfig.httpClient(stub.httpClient).cache)
    }

    @Test
    fun the_map_client_has_no_call_timeout_because_styles_and_tiles_can_be_slow() {
        assertEquals(0, MapConfig.httpClient(stub.httpClient).callTimeoutMillis)
    }

    @Test
    fun AC49_map_requests_carry_the_app_user_agent_and_no_other_identifying_header() {
        stub.server.enqueue(MockResponse.Builder().body("{}").build())
        val client = MapConfig.httpClient(stub.httpClient)

        client
            .newCall(
                Request
                    .Builder()
                    .url(stub.baseUrl)
                    .header("User-Agent", "MapLibre/other")
                    .build(),
            ).execute()
            .close()

        val recorded = stub.server.takeRequest()
        assertEquals(StubWeatherServer.USER_AGENT, recorded.headers["User-Agent"])
        val names =
            recorded.headers
                .names()
                .map { it.lowercase() }
                .toSet()
        assertFalse(names.any { it in setOf("cookie", "authorization", "x-requested-with", "referer") })
        assertEquals(1, stub.server.requestCount)
    }

    @Test
    fun AC49_the_map_client_refuses_plain_http() {
        val client = MapConfig.httpClient(stub.httpClient)

        assertThrows(IOException::class.java) {
            client.newCall(Request.Builder().url("http://tiles.openfreemap.org/styles/liberty").build()).execute()
        }
        assertEquals(0, stub.server.requestCount)
    }

    @Test
    fun the_map_client_is_built_from_the_app_client_so_it_keeps_the_https_only_and_user_agent_checks() {
        val app = MetNorwayClient.createHttpClient(temporaryFolder.newFolder(), "DogWalkBuddy/test")

        val map = MapConfig.httpClient(app)

        assertEquals(app.interceptors.size, map.interceptors.size)
        assertEquals(app.networkInterceptors.size, map.networkInterceptors.size)
        assertFalse(map.followSslRedirects)
    }
}
