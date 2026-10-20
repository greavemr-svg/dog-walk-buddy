package io.github.greavemrsvg.dogwalkbuddy.data.weather

import io.github.greavemrsvg.dogwalkbuddy.testutil.StubWeatherServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException
import java.util.concurrent.TimeUnit

class MetNorwayClientTest {
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

    private fun fetch(
        latitude: Double = 51.51,
        longitude: Double = -0.13,
        server: StubWeatherServer = stub,
    ) = runBlocking { MetNorwayClient(server.baseUrl, server.httpClient, Dispatchers.Unconfined).fetch(latitude, longitude) }

    @Test
    fun AC23_sends_only_the_rounded_location_over_https() {
        stub.enqueueFixture()

        fetch(latitude = 51.507351, longitude = -0.127758)

        val request = stub.server.takeRequest()
        assertEquals("https", request.url.scheme)
        assertEquals("/weatherapi/locationforecast/2.0/complete", request.url.encodedPath)
        assertEquals("lat=51.51&lon=-0.13", request.url.query)
        assertFalse(request.url.toString().contains("507351"))
        assertFalse(request.url.toString().contains("127758"))
    }

    @Test
    fun AC23_the_only_identifying_header_is_the_app_user_agent() {
        stub.enqueueFixture()

        fetch(51.507351, -0.127758)

        val headers = stub.server.takeRequest().headers
        assertEquals(StubWeatherServer.USER_AGENT, headers["User-Agent"])
        val names = headers.names().map { it.lowercase() }.toSet()
        assertTrue("unexpected identifying headers in $names", names.none { it in IDENTIFYING_HEADERS })
    }

    @Test
    fun AC23_returns_the_validated_hours() {
        stub.enqueueFixture()

        val hours = fetch()

        assertTrue(hours.size >= HOURS_NEEDED)
    }

    @Test
    fun AC22_an_error_status_is_a_failed_fetch() {
        for (code in listOf(500, 429, 404)) {
            stub.enqueueStatus(code)
            assertThrows(WeatherFetchException::class.java) { fetch() }
        }
    }

    @Test
    fun AC22_a_malformed_body_is_a_failed_fetch() {
        stub.enqueueBody("{not json")

        assertThrows(WeatherFetchException::class.java) { fetch() }
    }

    @Test
    fun AC22_a_body_over_2_MiB_is_a_failed_fetch() {
        val padding = "x".repeat((WeatherConfig.MAX_RESPONSE_BYTES + 1).toInt())
        stub.enqueueBody("""{"padding":"$padding"}""")

        assertThrows(WeatherFetchException::class.java) { fetch() }
    }

    @Test
    fun AC22_an_unreachable_server_is_a_failed_fetch() {
        stub.close()

        assertThrows(WeatherFetchException::class.java) { fetch() }
    }

    @Test
    fun AC22_a_slow_server_hits_the_call_timeout() {
        val slow = StubWeatherServer(temporaryFolder.newFolder()) { callTimeout(SHORT_TIMEOUT_MS, TimeUnit.MILLISECONDS) }
        try {
            slow.server.enqueue(
                MockResponse
                    .Builder()
                    .code(
                        200,
                    ).body(StubWeatherServer.fixtureText())
                    .headersDelay(SLOW_DELAY_MS, TimeUnit.MILLISECONDS)
                    .build(),
            )

            assertThrows(WeatherFetchException::class.java) {
                fetch(server = slow)
            }
        } finally {
            slow.close()
        }
    }

    @Test
    fun AC22_a_203_response_is_still_accepted() {
        stub.server.enqueue(
            MockResponse
                .Builder()
                .code(WeatherConfig.HTTP_NON_AUTHORITATIVE)
                .body(StubWeatherServer.fixtureText())
                .build(),
        )

        assertTrue(fetch().isNotEmpty())
    }

    @Test
    fun AC22_a_non_finite_location_is_refused_without_a_request() {
        assertThrows(WeatherFetchException::class.java) { fetch(Double.NaN, -0.13) }

        assertEquals(0, stub.server.requestCount)
    }

    @Test
    fun AC23_the_https_only_interceptor_refuses_plain_http() {
        val client = OkHttpClient.Builder().addInterceptor(HttpsOnlyInterceptor()).build()

        val error = assertThrows(IOException::class.java) { client.newCall(Request.Builder().url("http://localhost/").build()).execute() }

        assertEquals("Refusing non-HTTPS request", error.message)
    }

    @Test
    fun AC23_a_client_built_for_weather_has_a_10_second_call_timeout_and_a_cache() {
        val httpClient = MetNorwayClient.createHttpClient(temporaryFolder.newFolder(), StubWeatherServer.USER_AGENT)

        assertEquals(10_000, httpClient.callTimeoutMillis)
        assertTrue(httpClient.cache != null)
    }

    @Test
    fun AC23_the_production_user_agent_names_the_app_version_and_contact_url() {
        assertEquals(
            "DogWalkBuddy/0.1.0 (+https://github.com/greavemr-svg/dog-walk-buddy)",
            WeatherConfig.userAgent("0.1.0"),
        )
        assertTrue(WeatherConfig.productionBaseUrl.isHttps)
        assertNull(WeatherConfig.productionBaseUrl.query)
    }

    private companion object {
        const val HOURS_NEEDED = 12
        const val SHORT_TIMEOUT_MS = 300L
        const val SLOW_DELAY_MS = 3_000L
        val IDENTIFYING_HEADERS = setOf("authorization", "cookie", "x-device-id", "x-advertising-id", "x-user-id", "referer", "from")
    }
}
