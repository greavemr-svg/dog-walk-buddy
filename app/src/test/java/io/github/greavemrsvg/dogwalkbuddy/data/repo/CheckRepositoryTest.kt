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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import java.time.Duration
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class CheckRepositoryTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var stub: StubWeatherServer
    private lateinit var database: AppDatabase
    private lateinit var clock: MutableClock
    private lateinit var repository: CheckRepository

    private val fix = LocationFix(51.507351, -0.127758, accuracyM = 1000.0, timeUtc = START)

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

    @Test
    fun AC21_a_fresh_check_is_saved_with_rounded_coordinates_and_the_fetch_time() =
        runTest {
            stub.enqueueFixture()

            val result = repository.check(fix) as CheckData.Available

            assertFalse(result.isCached)
            assertEquals(START, result.fetchedAt)
            assertEquals(51.51, result.latitude, 0.0)
            assertEquals(-0.13, result.longitude, 0.0)
            val saved = database.cachedCheckDao().get()
            assertNotNull(saved)
            assertEquals(START.toEpochMilli(), saved!!.fetchedUtcMs)
            assertEquals(51.51, saved.lat, 0.0)
            assertEquals(-0.13, saved.lon, 0.0)
        }

    @Test
    fun AC21_offline_after_2h59m_shows_the_previous_result_with_its_fetch_time() =
        runTest {
            stub.enqueueFixture()
            val fresh = repository.check(fix) as CheckData.Available
            stub.close()
            clock.advance(Duration.ofMinutes(179))

            val result = repository.check(fix) as CheckData.Available

            assertTrue(result.isCached)
            assertEquals(START, result.fetchedAt)
            assertEquals(fresh.hours, result.hours)
            assertEquals(Duration.ofMinutes(179), Duration.between(result.fetchedAt, clock.instant()))
        }

    @Test
    fun AC21_a_server_error_after_2h59m_also_falls_back_to_the_cache() =
        runTest {
            stub.enqueueFixture().enqueueStatus(SERVER_ERROR)
            repository.check(fix)
            clock.advance(Duration.ofMinutes(179))

            val result = repository.check(fix)

            assertTrue((result as CheckData.Available).isCached)
        }

    @Test
    fun AC21_no_location_falls_back_to_a_cached_check_under_3h() =
        runTest {
            stub.enqueueFixture()
            repository.check(fix)
            clock.advance(Duration.ofMinutes(179))

            val result = repository.check(null)

            assertTrue((result as CheckData.Available).isCached)
        }

    @Test
    fun AC22_offline_at_exactly_3h_gives_no_result() =
        runTest {
            stub.enqueueFixture()
            repository.check(fix)
            stub.close()
            clock.advance(Duration.ofHours(3))

            assertEquals(CheckData.Unavailable, repository.check(fix))
        }

    @Test
    fun AC22_with_no_previous_check_a_failed_request_gives_no_result() =
        runTest {
            stub.enqueueStatus(SERVER_ERROR)

            assertEquals(CheckData.Unavailable, repository.check(fix))
        }

    @Test
    fun AC22_a_malformed_response_with_no_cache_gives_no_result_never_a_rating() =
        runTest {
            stub.enqueueBody("{not json")

            assertEquals(CheckData.Unavailable, repository.check(fix))
        }

    @Test
    fun AC22_no_location_and_no_cache_gives_no_result() =
        runTest {
            assertEquals(CheckData.Unavailable, repository.check(null))
        }

    @Test
    fun AC22_a_damaged_cache_row_is_treated_as_no_cache() =
        runTest {
            database
                .cachedCheckDao()
                .save(CachedCheckEntity(fetchedUtcMs = START.toEpochMilli(), lat = 51.51, lon = -0.13, hoursJson = "garbage"))

            assertEquals(CheckData.Unavailable, repository.loadCached())
        }

    @Test
    fun AC21_a_failed_fetch_does_not_overwrite_the_last_good_cache() =
        runTest {
            stub.enqueueFixture().enqueueStatus(SERVER_ERROR)
            repository.check(fix)
            clock.advance(Duration.ofMinutes(10))

            repository.check(fix)

            assertEquals(START.toEpochMilli(), database.cachedCheckDao().get()!!.fetchedUtcMs)
        }

    @Test
    fun AC21_an_empty_timeseries_shows_the_cached_result_and_leaves_the_cache_unchanged() =
        runTest {
            stub.enqueueFixture().enqueueBody(EMPTY_TIMESERIES)
            val fresh = repository.check(fix) as CheckData.Available
            val savedBefore = database.cachedCheckDao().get()
            clock.advance(Duration.ofMinutes(10))

            val result = repository.check(fix) as CheckData.Available

            assertTrue(result.isCached)
            assertEquals(START, result.fetchedAt)
            assertEquals(fresh.hours, result.hours)
            assertEquals(savedBefore, database.cachedCheckDao().get())
        }

    @Test
    fun AC22_an_empty_timeseries_with_no_cache_gives_no_result_and_saves_nothing() =
        runTest {
            stub.enqueueBody(EMPTY_TIMESERIES)

            assertEquals(CheckData.Unavailable, repository.check(fix))
            assertNull(database.cachedCheckDao().get())
        }

    @Test
    fun AC24_creating_the_repository_and_reading_the_cache_make_no_request() =
        runTest {
            repository.loadCached()
            repository.check(null)

            assertEquals(0, stub.server.requestCount)
        }

    @Test
    fun AC24_one_check_makes_exactly_one_request() =
        runTest {
            stub.enqueueFixture()

            repository.check(fix)

            assertEquals(1, stub.server.requestCount)
        }

    private companion object {
        const val SERVER_ERROR = 500
        const val EMPTY_TIMESERIES = """{"properties":{"timeseries":[]}}"""
        val START: Instant = Instant.parse("2026-10-19T14:20:00Z")
    }
}
