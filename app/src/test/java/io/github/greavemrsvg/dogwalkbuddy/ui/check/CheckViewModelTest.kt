package io.github.greavemrsvg.dogwalkbuddy.ui.check

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.greavemrsvg.dogwalkbuddy.data.db.AppDatabase
import io.github.greavemrsvg.dogwalkbuddy.data.db.CachedCheckDao
import io.github.greavemrsvg.dogwalkbuddy.data.db.CachedCheckEntity
import io.github.greavemrsvg.dogwalkbuddy.data.location.ApproximateLocationSource
import io.github.greavemrsvg.dogwalkbuddy.data.repo.CheckRepository
import io.github.greavemrsvg.dogwalkbuddy.data.weather.MetNorwayClient
import io.github.greavemrsvg.dogwalkbuddy.domain.location.LocationFix
import io.github.greavemrsvg.dogwalkbuddy.domain.rating.HeatSensitivity
import io.github.greavemrsvg.dogwalkbuddy.domain.rating.Rating
import io.github.greavemrsvg.dogwalkbuddy.domain.rating.Reason
import io.github.greavemrsvg.dogwalkbuddy.domain.window.BestWindow
import io.github.greavemrsvg.dogwalkbuddy.testutil.MetBodies
import io.github.greavemrsvg.dogwalkbuddy.testutil.MutableClock
import io.github.greavemrsvg.dogwalkbuddy.testutil.StubWeatherServer
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import okhttp3.Dispatcher
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import java.time.Duration
import java.time.Instant
import java.util.concurrent.AbstractExecutorService
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class CheckViewModelTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var stub: StubWeatherServer
    private lateinit var database: AppDatabase
    private lateinit var clock: MutableClock
    private lateinit var repository: CheckRepository

    private val fix = LocationFix(51.507351, -0.127758, accuracyM = 1000.0, timeUtc = START)

    // All coroutines (viewModelScope on Main, the weather client) run on this one virtual-time scheduler and only
    // advance when the test says so, so no assertion depends on thread timing or wall-clock time.
    private val scheduler = TestCoroutineScheduler()
    private val testDispatcher = StandardTestDispatcher(scheduler)
    private var permissionGranted = true
    private var locationCalls = 0
    private var locationSource = ApproximateLocationSource { fix }
    private val sensitivity = MutableStateFlow(HeatSensitivity.NORMAL)
    private val hasProfile = MutableStateFlow(true)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        // OkHttp and Room normally finish on their own threads, which the test scheduler cannot see. Running their work
        // on the calling thread makes every step complete inside advanceUntilIdle().
        stub = StubWeatherServer(temporaryFolder.newFolder()) { dispatcher(Dispatcher(DirectExecutorService())) }
        database =
            Room
                .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), AppDatabase::class.java)
                .allowMainThreadQueries()
                .setQueryExecutor { it.run() }
                .setTransactionExecutor { it.run() }
                .build()
        clock = MutableClock(START)
        repository = repositoryOver(database.cachedCheckDao())
    }

    @After
    fun tearDown() {
        database.close()
        stub.close()
        Dispatchers.resetMain()
    }

    private fun repositoryOver(dao: CachedCheckDao) =
        CheckRepository(MetNorwayClient(stub.baseUrl, stub.httpClient, testDispatcher), dao, clock)

    private fun newViewModel(
        repo: CheckRepository = repository,
        locationTimeout: Duration = CheckViewModel.LOCATION_TIMEOUT,
        logError: (String) -> Unit = {},
    ) = CheckViewModel(
        repository = repo,
        locationSource =
            ApproximateLocationSource {
                locationCalls++
                locationSource.current()
            },
        sensitivity = sensitivity,
        hasProfile = hasProfile,
        hasCoarsePermission = { permissionGranted },
        clock = clock,
        locationTimeout = locationTimeout,
        logError = logError,
    )

    // Runs everything that is scheduled, including virtual-time delays such as the location timeout.
    private fun settle() = scheduler.advanceUntilIdle()

    private fun CheckViewModel.awaitSettled(): CheckUiState {
        settle()
        return state.value.also { assertFalse("the check is still loading", it is CheckUiState.Loading) }
    }

    private fun rated(state: CheckUiState) = state as CheckUiState.Rated

    private fun CheckViewModel.awaitRated(accept: (CheckUiState.Rated) -> Boolean): CheckUiState.Rated {
        settle()
        val current = rated(state.value)
        assertTrue("the rated state was not the expected one: $current", accept(current))
        return current
    }

    private fun enqueueHours(temperatureC: Double) =
        stub.enqueueBody(MetBodies.hourly(START.minusSeconds(START.epochSecond % 3600), temperatureC))

    @Test
    fun AC20_a_successful_check_shows_a_rating_with_no_age_label_and_the_best_window() {
        stub.enqueueFixture()
        val viewModel = newViewModel()

        viewModel.onOpened()
        val state = rated(viewModel.awaitSettled())

        assertEquals(Rating.GO, state.current.rating)
        assertEquals(Reason.GOOD, state.current.reason)
        assertNull(state.ageMinutes)
        assertTrue(state.window is BestWindow.Window)
    }

    @Test
    fun AC25_without_location_permission_the_screen_explains_and_no_request_is_made() {
        permissionGranted = false
        val viewModel = newViewModel()

        viewModel.onOpened()
        settle()

        assertEquals(CheckUiState.NeedsLocationPermission, viewModel.state.value)
        assertEquals(0, stub.server.requestCount)
        assertEquals(0, locationCalls)
    }

    @Test
    fun AC25_after_the_permission_is_granted_the_check_runs() {
        permissionGranted = false
        stub.enqueueFixture()
        val viewModel = newViewModel()
        viewModel.onOpened()
        settle()

        permissionGranted = true
        viewModel.refresh()

        assertTrue(viewModel.awaitSettled() is CheckUiState.Rated)
        assertEquals(1, stub.server.requestCount)
    }

    @Test
    fun AC25_refresh_without_permission_still_makes_no_request() {
        permissionGranted = false
        val viewModel = newViewModel()

        viewModel.refresh()
        settle()

        assertEquals(CheckUiState.NeedsLocationPermission, viewModel.state.value)
        assertEquals(0, stub.server.requestCount)
    }

    @Test
    fun AC21_offline_after_2h59m_the_previous_result_is_shown_with_its_age() {
        stub.enqueueFixture().enqueueStatus(SERVER_ERROR)
        newViewModel().apply {
            onOpened()
            awaitSettled()
        }
        clock.advance(Duration.ofMinutes(179))
        val viewModel = newViewModel()

        viewModel.onOpened()
        val state = rated(viewModel.awaitSettled())

        assertEquals(179L, state.ageMinutes)
    }

    @Test
    fun AC22_at_3h_with_a_failing_request_there_is_no_rating() {
        stub.enqueueFixture().enqueueStatus(SERVER_ERROR)
        newViewModel().apply {
            onOpened()
            awaitSettled()
        }
        clock.advance(Duration.ofHours(3))
        val viewModel = newViewModel()

        viewModel.onOpened()

        assertEquals(CheckUiState.CantCheck, viewModel.awaitSettled())
    }

    @Test
    fun AC22_with_no_previous_check_and_a_failing_request_there_is_no_rating() {
        stub.enqueueStatus(SERVER_ERROR)
        val viewModel = newViewModel()

        viewModel.onOpened()

        assertEquals(CheckUiState.CantCheck, viewModel.awaitSettled())
    }

    @Test
    fun AC22_a_malformed_response_is_cant_check_never_a_rating() {
        stub.enqueueBody("{not json")
        val viewModel = newViewModel()

        viewModel.onOpened()

        assertEquals(CheckUiState.CantCheck, viewModel.awaitSettled())
    }

    @Test
    fun AC15_all_three_core_values_missing_for_the_current_hour_is_cant_check() {
        stub.enqueueBody(
            MetBodies.hourly(
                START.minusSeconds(START.epochSecond % 3600),
                temperatureC = null,
                windMetresPerSecond = null,
                precipitationMmPerHour = null,
            ),
        )
        val viewModel = newViewModel()

        viewModel.onOpened()

        assertEquals(CheckUiState.CantCheck, viewModel.awaitSettled())
    }

    @Test
    fun AC15_retry_runs_a_new_check_that_can_succeed() {
        stub.enqueueStatus(SERVER_ERROR).enqueueFixture()
        val viewModel = newViewModel()
        viewModel.onOpened()
        assertEquals(CheckUiState.CantCheck, viewModel.awaitSettled())

        viewModel.refresh()

        assertTrue(viewModel.awaitSettled() is CheckUiState.Rated)
        assertEquals(2, stub.server.requestCount)
    }

    @Test
    fun AC44_a_sensitivity_change_re_rates_go_to_caution_without_a_new_request() {
        enqueueHours(17.0)
        val viewModel = newViewModel()
        viewModel.onOpened()
        assertEquals(Rating.GO, rated(viewModel.awaitSettled()).current.rating)

        sensitivity.value = HeatSensitivity.SENSITIVE
        val state = viewModel.awaitRated { it.current.rating == Rating.CAUTION }

        assertEquals(Rating.CAUTION, state.current.rating)
        assertEquals(Reason.WARM, state.current.reason)
        assertEquals(1, stub.server.requestCount)
    }

    @Test
    fun AC44_a_sensitivity_change_re_rates_caution_to_avoid_without_a_new_request() {
        enqueueHours(22.0)
        val viewModel = newViewModel()
        viewModel.onOpened()
        assertEquals(Rating.CAUTION, rated(viewModel.awaitSettled()).current.rating)

        sensitivity.value = HeatSensitivity.SENSITIVE
        val state = viewModel.awaitRated { it.current.rating == Rating.AVOID }

        assertEquals(Rating.AVOID, state.current.rating)
        assertEquals(Reason.TOO_HOT, state.current.reason)
        assertEquals(1, stub.server.requestCount)
    }

    @Test
    fun AC44_returning_to_the_screen_after_a_profile_change_shows_the_re_rated_result_with_no_request() {
        enqueueHours(17.0)
        val viewModel = newViewModel()
        viewModel.onOpened()
        viewModel.awaitSettled()

        sensitivity.value = HeatSensitivity.SENSITIVE
        viewModel.onOpened()

        assertEquals(Rating.CAUTION, viewModel.awaitRated { it.current.rating == Rating.CAUTION }.current.rating)
        assertEquals(1, stub.server.requestCount)
        assertEquals(1, locationCalls)
    }

    @Test
    fun AC45_with_no_profile_the_state_says_normal_sensitivity_is_used() {
        hasProfile.value = false
        stub.enqueueFixture()
        val viewModel = newViewModel()

        viewModel.onOpened()

        assertTrue(rated(viewModel.awaitSettled()).usingDefaultSensitivity)
    }

    @Test
    fun AC45_with_a_profile_the_note_is_not_needed() {
        stub.enqueueFixture()
        val viewModel = newViewModel()

        viewModel.onOpened()

        assertFalse(rated(viewModel.awaitSettled()).usingDefaultSensitivity)
    }

    @Test
    fun AC24_opening_again_with_a_result_under_3h_old_makes_no_second_request() {
        stub.enqueueFixture()
        val viewModel = newViewModel()
        viewModel.onOpened()
        viewModel.awaitSettled()

        clock.advance(Duration.ofMinutes(30))
        viewModel.onOpened()
        settle()

        assertEquals(1, stub.server.requestCount)
        assertEquals(30L, rated(viewModel.state.value).ageMinutes)
    }

    @Test
    fun AC24_opening_with_a_result_that_is_3h_old_checks_again() {
        stub.enqueueFixture().enqueueFixture()
        val viewModel = newViewModel()
        viewModel.onOpened()
        viewModel.awaitSettled()

        clock.advance(Duration.ofHours(3))
        viewModel.onOpened()
        viewModel.awaitSettled()

        assertEquals(2, stub.server.requestCount)
    }

    @Test
    fun AC24_refresh_always_makes_a_new_request() {
        stub.enqueueFixture().enqueueFixture()
        val viewModel = newViewModel()
        viewModel.onOpened()
        viewModel.awaitSettled()

        viewModel.refresh()
        viewModel.awaitSettled()

        assertEquals(2, stub.server.requestCount)
    }

    @Test
    fun AC24_creating_the_view_model_makes_no_request() {
        newViewModel()

        assertEquals(0, stub.server.requestCount)
        assertEquals(0, locationCalls)
    }

    @Test
    fun AC24_opening_while_a_check_is_already_running_does_not_start_another() {
        val release = CompletableDeferred<LocationFix?>()
        locationSource = ApproximateLocationSource { release.await() }
        val viewModel = newViewModel()

        viewModel.onOpened()
        viewModel.onOpened()
        scheduler.runCurrent()

        assertEquals(1, locationCalls)
        assertEquals(CheckUiState.Loading, viewModel.state.value)
        release.cancel()
    }

    @Test
    fun AC22_a_location_that_never_arrives_times_out_into_the_cache_or_cant_check() {
        locationSource = ApproximateLocationSource { awaitCancellation() }
        val viewModel = newViewModel(locationTimeout = Duration.ofMillis(LOCATION_TIMEOUT_MS))

        viewModel.onOpened()

        assertEquals(CheckUiState.CantCheck, viewModel.awaitSettled())
        assertEquals(0, stub.server.requestCount)
    }

    @Test
    fun AC21_no_location_falls_back_to_a_cached_result_under_3h_old() {
        stub.enqueueFixture()
        newViewModel().apply {
            onOpened()
            awaitSettled()
        }
        clock.advance(Duration.ofMinutes(90))
        locationSource = ApproximateLocationSource { null }
        val viewModel = newViewModel()

        viewModel.onOpened()

        assertEquals(90L, rated(viewModel.awaitSettled()).ageMinutes)
        assertEquals(1, stub.server.requestCount)
    }

    @Test
    fun AC22_a_location_source_that_throws_is_treated_as_no_location() {
        locationSource = ApproximateLocationSource { throw SecurityException("permission revoked") }
        val viewModel = newViewModel()

        viewModel.onOpened()

        assertEquals(CheckUiState.CantCheck, viewModel.awaitSettled())
        assertEquals(0, stub.server.requestCount)
    }

    @Test
    fun AC22_an_unexpected_storage_failure_is_cant_check_not_a_crash() {
        val dao = database.cachedCheckDao()
        val failingDao =
            object : CachedCheckDao by dao {
                override suspend fun get(): CachedCheckEntity? = throw IllegalStateException("disk failure")
            }
        locationSource = ApproximateLocationSource { null }
        val viewModel = newViewModel(repo = repositoryOver(failingDao))

        viewModel.onOpened()

        assertEquals(CheckUiState.CantCheck, viewModel.awaitSettled())
    }

    @Test
    fun AC21_a_result_that_is_a_few_minutes_old_in_this_session_is_labelled_with_its_age() {
        stub.enqueueFixture()
        val viewModel = newViewModel()
        viewModel.onOpened()
        viewModel.awaitSettled()

        assertNull(rated(viewModel.state.value).ageMinutes)

        // The state is recomputed when an input changes, here the sensitivity, using the clock at that moment.
        clock.advance(Duration.ofSeconds(60))
        sensitivity.value = HeatSensitivity.SENSITIVE

        assertEquals(1L, viewModel.awaitRated { it.ageMinutes == 1L }.ageMinutes)
    }

    @Test
    fun AC21_returning_to_the_screen_30_min_after_a_check_shows_the_label_with_no_request() {
        stub.enqueueFixture()
        val viewModel = newViewModel()
        viewModel.onOpened()
        viewModel.awaitSettled()

        clock.advance(Duration.ofMinutes(30))
        viewModel.onShown()

        assertEquals(30L, viewModel.awaitRated { it.ageMinutes == 30L }.ageMinutes)
        assertEquals(1, stub.server.requestCount)
        assertEquals(1, locationCalls)
    }

    @Test
    fun AC22_returning_to_the_screen_3h_after_a_check_is_cant_check_and_makes_no_request() {
        stub.enqueueFixture()
        val viewModel = newViewModel()
        viewModel.onOpened()
        viewModel.awaitSettled()

        clock.advance(Duration.ofHours(3))
        viewModel.onShown()

        settle()

        assertEquals(CheckUiState.CantCheck, viewModel.state.value)
        assertEquals(1, stub.server.requestCount)
        assertEquals(1, locationCalls)
    }

    @Test
    fun AC22_a_result_just_under_3h_old_is_still_shown_with_its_age() {
        stub.enqueueFixture()
        val viewModel = newViewModel()
        viewModel.onOpened()
        viewModel.awaitSettled()

        clock.advance(Duration.ofMinutes(179))
        viewModel.onShown()

        assertEquals(179L, viewModel.awaitRated { it.ageMinutes == 179L }.ageMinutes)
    }

    @Test
    fun AC22_a_result_1_second_short_of_3h_old_is_still_shown_and_makes_no_request() {
        stub.enqueueFixture()
        val viewModel = newViewModel()
        viewModel.onOpened()
        viewModel.awaitSettled()

        clock.advance(Duration.ofHours(3).minusSeconds(1))
        viewModel.onShown()

        assertEquals(179L, viewModel.awaitRated { it.ageMinutes == 179L }.ageMinutes)
        assertEquals(1, stub.server.requestCount)
        assertEquals(1, locationCalls)
    }

    @Test
    fun AC22_a_result_shown_after_3h_can_be_checked_again_with_a_refresh() {
        stub.enqueueFixture()
        val viewModel = newViewModel()
        viewModel.onOpened()
        viewModel.awaitSettled()
        clock.advance(Duration.ofHours(3))
        viewModel.onShown()
        settle()
        assertEquals(CheckUiState.CantCheck, viewModel.state.value)
        stub.enqueueBody(MetBodies.hourly(START.plus(Duration.ofHours(3)).let { it.minusSeconds(it.epochSecond % 3600) }, 12.0))

        viewModel.refresh()

        assertTrue(viewModel.awaitSettled() is CheckUiState.Rated)
        assertEquals(2, stub.server.requestCount)
    }

    @Test
    fun AC24_returning_to_the_screen_while_a_check_is_loading_does_not_start_a_second_request() {
        stub.enqueueFixture()
        val viewModel = newViewModel()
        viewModel.onOpened()

        viewModel.onShown()
        viewModel.awaitSettled()

        assertEquals(1, stub.server.requestCount)
        assertEquals(1, locationCalls)
    }

    @Test
    fun AC25_returning_from_settings_after_a_first_refusal_with_the_permission_granted_runs_the_check() {
        permissionGranted = false
        stub.enqueueFixture()
        val viewModel = newViewModel()
        viewModel.onOpened()
        settle()
        viewModel.onPermissionResult(granted = false, canAskAgain = true)

        permissionGranted = true
        viewModel.onShown()

        assertTrue(viewModel.awaitSettled() is CheckUiState.Rated)
        assertEquals(1, stub.server.requestCount)
    }

    @Test
    fun AC25_a_permission_revoked_while_a_result_is_shown_keeps_the_result_until_refresh_then_asks_again() {
        stub.enqueueFixture()
        val viewModel = newViewModel()
        viewModel.onOpened()
        viewModel.awaitSettled()

        permissionGranted = false
        viewModel.onShown()
        assertTrue(viewModel.awaitSettled() is CheckUiState.Rated)
        viewModel.refresh()
        settle()

        assertEquals(CheckUiState.NeedsLocationPermission, viewModel.state.value)
        assertEquals(1, stub.server.requestCount)
    }

    @Test
    fun AC22_a_location_failure_with_a_message_holding_coordinates_never_logs_the_message() {
        val logged = mutableListOf<String>()
        locationSource = ApproximateLocationSource { throw SecurityException("lat=51.507351 lon=-0.127758") }
        val viewModel = newViewModel(logError = { logged += it })

        viewModel.onOpened()
        viewModel.awaitSettled()

        assertEquals(listOf(SecurityException::class.java.name), logged)
        assertTrue(logged.none { it.contains("51.5") || it.contains("lat=") })
    }

    @Test
    fun AC25_a_permanent_refusal_shows_the_open_settings_state_and_makes_no_request() {
        permissionGranted = false
        val viewModel = newViewModel()
        viewModel.onOpened()
        settle()

        viewModel.onPermissionResult(granted = false, canAskAgain = false)
        settle()

        assertEquals(CheckUiState.LocationPermissionBlocked, viewModel.state.value)
        assertEquals(0, stub.server.requestCount)
        assertEquals(0, locationCalls)
    }

    @Test
    fun AC25_a_refusal_that_can_be_asked_again_keeps_the_grant_prompt() {
        permissionGranted = false
        val viewModel = newViewModel()
        viewModel.onOpened()
        settle()
        viewModel.onPermissionResult(granted = false, canAskAgain = false)

        viewModel.onPermissionResult(granted = false, canAskAgain = true)
        settle()

        assertEquals(CheckUiState.NeedsLocationPermission, viewModel.state.value)
    }

    @Test
    fun AC25_a_grant_from_the_dialog_runs_the_check() {
        permissionGranted = false
        stub.enqueueFixture()
        val viewModel = newViewModel()
        viewModel.onOpened()
        settle()

        permissionGranted = true
        viewModel.onPermissionResult(granted = true, canAskAgain = true)

        assertTrue(viewModel.awaitSettled() is CheckUiState.Rated)
    }

    @Test
    fun AC25_returning_from_settings_with_the_permission_granted_runs_the_check() {
        permissionGranted = false
        stub.enqueueFixture()
        val viewModel = newViewModel()
        viewModel.onOpened()
        settle()
        viewModel.onPermissionResult(granted = false, canAskAgain = false)

        permissionGranted = true
        viewModel.onShown()

        assertTrue(viewModel.awaitSettled() is CheckUiState.Rated)
        assertEquals(1, stub.server.requestCount)
    }

    @Test
    fun AC25_returning_from_settings_still_without_the_permission_stays_blocked_with_no_request() {
        permissionGranted = false
        val viewModel = newViewModel()
        viewModel.onOpened()
        settle()
        viewModel.onPermissionResult(granted = false, canAskAgain = false)

        viewModel.onShown()
        settle()

        assertEquals(CheckUiState.LocationPermissionBlocked, viewModel.state.value)
        assertEquals(0, stub.server.requestCount)
    }

    @Test
    fun AC21_a_stored_result_under_a_minute_old_reads_0_min_ago() {
        stub.enqueueFixture().enqueueStatus(SERVER_ERROR)
        newViewModel().apply {
            onOpened()
            awaitSettled()
        }
        val viewModel = newViewModel()

        viewModel.onOpened()

        assertEquals(0L, rated(viewModel.awaitSettled()).ageMinutes)
    }

    @Test
    fun AC22_an_unexpected_failure_logs_only_the_error_class_name() {
        val logged = mutableListOf<String>()
        locationSource = ApproximateLocationSource { throw IllegalStateException("secret 51.5, -0.12") }
        val viewModel = newViewModel(logError = { logged += it })

        viewModel.onOpened()
        viewModel.awaitSettled()

        assertTrue(logged.isNotEmpty())
        assertTrue(logged.all { it == IllegalStateException::class.java.name })
    }

    @Test
    fun AC22_a_storage_failure_logs_only_the_error_class_name() {
        val logged = mutableListOf<String>()
        val failingDao =
            object : CachedCheckDao by database.cachedCheckDao() {
                override suspend fun get(): CachedCheckEntity? = throw IllegalStateException("disk failure")
            }
        locationSource = ApproximateLocationSource { null }
        val viewModel = newViewModel(repositoryOver(failingDao), logError = { logged += it })

        viewModel.onOpened()
        viewModel.awaitSettled()

        assertEquals(listOf(IllegalStateException::class.java.name), logged)
    }

    private companion object {
        const val SERVER_ERROR = 500
        const val LOCATION_TIMEOUT_MS = 50L

        // Inside the first hourly period of the fixture, which starts 14:00 UTC on 19 October 2026.
        val START: Instant = Instant.parse("2026-10-19T14:20:00Z")
    }
}

/** Runs every task at once on the calling thread, so OkHttp finishes a call before enqueue returns. */
private class DirectExecutorService : AbstractExecutorService() {
    private var stopped = false

    override fun execute(command: Runnable) = command.run()

    override fun shutdown() {
        stopped = true
    }

    override fun shutdownNow(): MutableList<Runnable> {
        stopped = true
        return mutableListOf()
    }

    override fun isShutdown() = stopped

    override fun isTerminated() = stopped

    override fun awaitTermination(
        timeout: Long,
        unit: TimeUnit,
    ) = stopped
}
