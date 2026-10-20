package io.github.greavemrsvg.dogwalkbuddy.data.repo

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.greavemrsvg.dogwalkbuddy.data.db.AppDatabase
import io.github.greavemrsvg.dogwalkbuddy.data.db.CachedCheckEntity
import io.github.greavemrsvg.dogwalkbuddy.data.weather.MetNorwayClient
import io.github.greavemrsvg.dogwalkbuddy.domain.check.CheckData
import io.github.greavemrsvg.dogwalkbuddy.domain.location.LocationFix
import io.github.greavemrsvg.dogwalkbuddy.testutil.MutableClock
import io.github.greavemrsvg.dogwalkbuddy.testutil.StubWeatherServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import java.time.Duration
import java.time.Instant

/** Boundary and failure-mode cases for AC21 to AC24 at the repository level. */
@RunWith(AndroidJUnit4::class)
class CheckRepositoryBoundaryTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var stub: StubWeatherServer
    private lateinit var database: AppDatabase
    private lateinit var clock: MutableClock
    private lateinit var repository: CheckRepository

    private val spec = LocationFix(51.507351, -0.127758, accuracyM = 1000.0, timeUtc = START)

    @Before
    fun setUp() {
        stub = StubWeatherServer(temporaryFolder.newFolder())
        database =
            Room
                .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), AppDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        clock = MutableClock(START)
        repository =
            CheckRepository(MetNorwayClient(stub.baseUrl, stub.httpClient, Dispatchers.Unconfined), database.cachedCheckDao(), clock)
    }

    @After
    fun tearDown() {
        database.close()
        stub.close()
    }

    private suspend fun seedGoodCacheThenGoOffline() {
        stub.enqueueFixture()
        repository.check(spec)
        stub.close()
    }

    private suspend fun saveRow(text: String) {
        database
            .cachedCheckDao()
            .save(CachedCheckEntity(fetchedUtcMs = START.toEpochMilli(), lat = 51.51, lon = -0.13, hoursJson = text))
    }

    @Test
    fun AC21_offline_at_2h59m59s_still_shows_the_previous_result() =
        runTest {
            seedGoodCacheThenGoOffline()
            clock.advance(Duration.ofSeconds(HOURS_3_SECONDS - 1))

            assertTrue((repository.check(spec) as CheckData.Available).isCached)
        }

    @Test
    fun AC22_offline_at_3h_and_1s_gives_no_result() =
        runTest {
            seedGoodCacheThenGoOffline()
            clock.advance(Duration.ofSeconds(HOURS_3_SECONDS + 1))

            assertEquals(CheckData.Unavailable, repository.check(spec))
        }

    @Test
    fun AC22_no_location_at_exactly_3h_gives_no_result() =
        runTest {
            seedGoodCacheThenGoOffline()
            clock.advance(Duration.ofHours(3))

            assertEquals(CheckData.Unavailable, repository.check(null))
        }

    @Test
    fun AC22_a_cache_row_dated_in_the_future_is_not_shown() =
        runTest {
            seedGoodCacheThenGoOffline()
            clock.advance(Duration.ofMinutes(-5))

            assertEquals(CheckData.Unavailable, repository.check(spec))
        }

    @Test
    fun AC21_a_malformed_response_after_a_good_check_falls_back_to_the_cache_and_keeps_it() =
        runTest {
            stub.enqueueFixture().enqueueBody("{not json")
            repository.check(spec)
            clock.advance(Duration.ofMinutes(30))

            val result = repository.check(spec) as CheckData.Available

            assertTrue(result.isCached)
            assertEquals(START, result.fetchedAt)
            assertEquals(START.toEpochMilli(), database.cachedCheckDao().get()!!.fetchedUtcMs)
        }

    @Test
    fun AC21_a_later_good_fetch_replaces_the_cache_and_restarts_the_3h_window() =
        runTest {
            stub.enqueueFixture().enqueueFixture()
            repository.check(spec)
            clock.advance(Duration.ofHours(2))
            repository.check(spec)
            stub.close()
            clock.advance(Duration.ofMinutes(179))

            val result = repository.check(spec) as CheckData.Available

            assertTrue(result.isCached)
            assertEquals(START.plus(Duration.ofHours(2)), result.fetchedAt)
        }

    @Test
    fun AC22_cache_rows_with_the_wrong_shape_are_treated_as_no_cache() =
        runTest {
            val rows =
                listOf(
                    "",
                    "null",
                    "{}",
                    "[{}]",
                    "[1,2]",
                    "[{\"startUtcMs\":\"x\"}]",
                    "[{\"startUtcMs\":1,\"temperatureC\":\"hot\"}]",
                )
            for (text in rows) {
                saveRow(text)

                assertEquals("row <$text>", CheckData.Unavailable, repository.loadCached())
            }
        }

    @Test
    fun AC22_a_damaged_cache_row_with_a_failed_fetch_gives_no_result() =
        runTest {
            saveRow("garbage")
            stub.enqueueStatus(SERVER_ERROR)

            assertEquals(CheckData.Unavailable, repository.check(spec))
        }

    @Test
    fun AC21_missing_values_survive_the_cache_round_trip_as_missing_not_zero() =
        runTest {
            saveRow("[{\"startUtcMs\":1000}]")

            val hour = (repository.loadCached() as CheckData.Available).hours.single()

            assertNull(hour.temperatureC)
            assertNull(hour.precipitationMmPerHour)
            assertNull(hour.windMeanMph)
            assertNull(hour.cloudCoverPercent)
        }

    @Test
    fun AC23_the_check_sends_the_spec_location_rounded_and_nothing_else_identifying() =
        runTest {
            stub.enqueueFixture()

            repository.check(spec)

            val request = stub.server.takeRequest()
            assertEquals("https", request.url.scheme)
            assertEquals("lat=51.51&lon=-0.13", request.url.query)
            assertEquals(StubWeatherServer.USER_AGENT, request.headers["User-Agent"])
            assertNull(request.headers["Cookie"])
            assertNull(request.headers["Authorization"])
            assertEquals(0, request.body?.size ?: 0)
            assertEquals("GET", request.method)
        }

    @Test
    fun AC23_negative_latitude_and_positive_longitude_round_with_the_sign_kept() =
        runTest {
            stub.enqueueFixture()

            val southEast = LocationFix(-33.865, 151.209, null, START)
            val result = repository.check(southEast) as CheckData.Available

            val request = stub.server.takeRequest()
            assertEquals("lat=-33.87&lon=151.21", request.url.query)
            assertEquals(-33.87, result.latitude, 0.0)
            assertEquals(151.21, result.longitude, 0.0)
        }

    @Test
    fun AC24_a_failed_check_makes_exactly_one_request_and_does_not_retry() =
        runTest {
            stub.enqueueStatus(SERVER_ERROR).enqueueFixture()

            repository.check(spec)

            assertEquals(1, stub.server.requestCount)
        }

    @Test
    fun AC24_loading_the_cache_after_a_check_makes_no_further_request() =
        runTest {
            stub.enqueueFixture()
            repository.check(spec)

            repository.loadCached()
            val cached = repository.loadCached() as CheckData.Available

            assertEquals(1, stub.server.requestCount)
            assertEquals(51.51, cached.latitude, 0.0)
        }

    private companion object {
        const val SERVER_ERROR = 500
        const val HOURS_3_SECONDS = 3L * 3600
        val START: Instant = Instant.parse("2026-10-19T14:20:00Z")
    }
}
