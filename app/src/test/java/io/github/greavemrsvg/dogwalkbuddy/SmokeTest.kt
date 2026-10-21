package io.github.greavemrsvg.dogwalkbuddy

import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.greavemrsvg.dogwalkbuddy.data.db.AppDatabase
import io.github.greavemrsvg.dogwalkbuddy.testutil.FakeWalkServiceLauncher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.emptyFlow
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

@RunWith(AndroidJUnit4::class)
class SmokeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun home_screen_shows_the_app_title() {
        composeRule.onNodeWithText("Dog Walk Buddy").assertExists()
    }

    @Test
    fun application_builds_a_container_with_the_system_zone() {
        val app = ApplicationProvider.getApplicationContext<DogWalkBuddyApp>()

        assertEquals(ZoneId.systemDefault(), app.container.zoneId)
    }

    @Test
    fun container_uses_the_injected_clock_and_zone() {
        val fixed = Clock.fixed(Instant.parse("2026-10-19T12:00:00Z"), ZoneOffset.UTC)
        val zone = ZoneId.of("Europe/London")

        val context = ApplicationProvider.getApplicationContext<DogWalkBuddyApp>()
        val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        val baseUrl = "https://localhost/".toHttpUrl()

        val container =
            AppContainer(
                clock = fixed,
                zoneId = zone,
                weatherBaseUrl = baseUrl,
                weatherHttpClient = OkHttpClient(),
                database = database,
                approximateLocationSource = { null },
                hasCoarseLocationPermission = { false },
                is24HourFormat = { true },
                filesDir = context.filesDir,
                walkLocationSource = { emptyFlow() },
                hasFineLocationPermission = { false },
                walkServiceLauncher = FakeWalkServiceLauncher(),
                applicationScope = CoroutineScope(SupervisorJob()),
            )

        assertEquals(Instant.parse("2026-10-19T12:00:00Z"), container.clock.instant())
        assertEquals(zone, container.zoneId)
        assertEquals(baseUrl, container.weatherBaseUrl)
        database.close()
    }

    @Test
    fun AC24_the_default_container_is_wired_for_the_production_weather_host_and_makes_no_request_on_creation() {
        val app = ApplicationProvider.getApplicationContext<DogWalkBuddyApp>()

        val container = app.container

        assertEquals("api.met.no", container.weatherBaseUrl.host)
        assertTrue(container.weatherBaseUrl.isHttps)
        assertEquals(0, container.weatherHttpClient.dispatcher.runningCallsCount())
        assertNotNull(container.checkRepository)
        assertNotNull(container.approximateLocationSource)
    }
}
