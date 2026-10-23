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
import io.github.greavemrsvg.dogwalkbuddy.domain.check.Factor
import io.github.greavemrsvg.dogwalkbuddy.domain.check.FactorCell
import io.github.greavemrsvg.dogwalkbuddy.domain.check.FactorValue
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
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
import java.util.concurrent.RejectedExecutionException
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
        dogName: Flow<String?> = flowOf(null),
        isOnline: () -> Boolean = { true },
        connectivityChanges: Flow<Unit> = emptyFlow(),
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
        dogName = dogName,
        isOnline = isOnline,
        connectivityChanges = connectivityChanges,
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
    fun UX_AC17_the_rated_state_has_the_current_hours_factor_cells_rounded_with_the_reasons_cell_highlighted() {
        // 2 m/s is 4.47 mph. Cloud 100 % means the pavement rule does not apply.
        stub.enqueueBody(MetBodies.hourly(START.minusSeconds(START.epochSecond % 3600), temperatureC = -2.5))
        val viewModel = newViewModel()

        viewModel.onOpened()
        val state = rated(viewModel.awaitSettled())

        assertEquals(Reason.FREEZING, state.current.reason)
        assertEquals(
            listOf(
                FactorCell(Factor.AIR, FactorValue.Whole(-3), highlighted = true),
                FactorCell(Factor.RAIN, FactorValue.Whole(0), highlighted = false),
                FactorCell(Factor.WIND, FactorValue.Whole(4), highlighted = false),
                FactorCell(Factor.PAVEMENT, FactorValue.NotRated, highlighted = false),
            ),
            state.factors,
        )
        assertEquals(12, state.hours.size)
        assertEquals(state.current, state.hours.first())
        assertFalse(state.isCached)
        assertFalse(state.isOffline)
    }

    @Test
    fun UX_AC20_the_dog_name_and_sensitivity_reach_the_title_and_the_rated_state() {
        stub.enqueueFixture()
        sensitivity.value = HeatSensitivity.SENSITIVE
        val viewModel = newViewModel(dogName = flowOf("Biscuit"))

        viewModel.onOpened()
        val state = rated(viewModel.awaitSettled())

        assertEquals("Biscuit", state.dogName)
        assertEquals("Biscuit", viewModel.dogName.value)
        assertEquals(HeatSensitivity.SENSITIVE, state.sensitivity)
    }

    @Test
    fun UX_AC21_a_cached_result_is_offline_only_when_the_device_has_no_connection() {
        stub.enqueueFixture().enqueueStatus(SERVER_ERROR).enqueueStatus(SERVER_ERROR)
        newViewModel().apply {
            onOpened()
            awaitSettled()
        }
        clock.advance(Duration.ofMinutes(30))

        val offline = rated(newViewModel(isOnline = { false }).apply { onOpened() }.awaitSettled())
        assertTrue(offline.isCached)
        assertTrue(offline.isOffline)

        val online = rated(newViewModel(isOnline = { true }).apply { onOpened() }.awaitSettled())
        assertTrue(online.isCached)
        assertFalse(online.isOffline)
    }

    @Test
    fun UX_AC21_a_fresh_result_is_neither_cached_nor_offline_even_without_a_connection() {
        stub.enqueueFixture()
        val viewModel = newViewModel(isOnline = { false })

        viewModel.onOpened()
        val state = rated(viewModel.awaitSettled())

        assertFalse(state.isCached)
        assertFalse(state.isOffline)
    }

    @Test
    fun UX_AC21_no_location_fix_with_a_connection_shows_the_cached_result_without_the_offline_lead() {
        stub.enqueueFixture()
        newViewModel().apply {
            onOpened()
            awaitSettled()
        }
        clock.advance(Duration.ofMinutes(90))
        locationSource = ApproximateLocationSource { null }

        val online = rated(newViewModel(isOnline = { true }).apply { onOpened() }.awaitSettled())

        assertTrue(online.isCached)
        assertFalse(online.isOffline)
        assertEquals(90L, online.ageMinutes)
        assertEquals(1, stub.server.requestCount)
    }

    @Test
    fun UX_AC21_no_connection_at_2h59m_shows_the_cached_result_as_offline_with_its_age() {
        stub.enqueueFixture()
        newViewModel().apply {
            onOpened()
            awaitSettled()
        }
        clock.advance(Duration.ofMinutes(179))
        locationSource = ApproximateLocationSource { null }

        val offline = rated(newViewModel(isOnline = { false }).apply { onOpened() }.awaitSettled())

        assertTrue(offline.isCached)
        assertTrue(offline.isOffline)
        assertEquals(179L, offline.ageMinutes)
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
        val viewModel = newViewModel()

        // Anything the view model launched while being created only runs once the scheduler advances.
        settle()

        assertEquals(0, stub.server.requestCount)
        assertEquals(0, locationCalls)
        assertEquals(CheckUiState.Loading, viewModel.state.value)
    }

    @Test
    fun AC24_opening_while_a_check_is_already_running_does_not_start_another() {
        // Covers two back-to-back opens before anything is dispatched. The guard against
        // restarting a check that has already started is covered by the next test.
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
    fun AC24_opening_again_after_a_running_check_has_started_does_not_restart_it() {
        // The first check must actually be running (waiting for the location) before the second open, otherwise a
        // restart would cancel a job that never ran and the location would still be asked for only once.
        stub.enqueueFixture()
        val release = CompletableDeferred<LocationFix?>()
        locationSource = ApproximateLocationSource { release.await() }
        val viewModel = newViewModel()
        viewModel.onOpened()
        scheduler.runCurrent()
        assertEquals(1, locationCalls)

        viewModel.onOpened()
        scheduler.runCurrent()

        assertEquals(1, locationCalls)
        assertEquals(CheckUiState.Loading, viewModel.state.value)
        release.complete(fix)
        assertTrue(viewModel.awaitSettled() is CheckUiState.Rated)
        assertEquals(1, stub.server.requestCount)
        assertEquals(1, locationCalls)
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
        // The location lookup is held open so the check is genuinely still loading when the screen is shown again.
        val release = CompletableDeferred<LocationFix?>()
        locationSource = ApproximateLocationSource { release.await() }
        val viewModel = newViewModel()
        viewModel.onOpened()
        scheduler.runCurrent()
        assertEquals(1, locationCalls)
        assertEquals(CheckUiState.Loading, viewModel.state.value)

        viewModel.onShown()
        scheduler.runCurrent()
        assertEquals(CheckUiState.Loading, viewModel.state.value)
        release.complete(fix)
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

    @Test
    fun UX_AC21_the_offline_lead_follows_a_network_change_while_today_stays_open() {
        stub.enqueueFixture()
        newViewModel().apply {
            onOpened()
            awaitSettled()
        }
        clock.advance(Duration.ofMinutes(90))
        locationSource = ApproximateLocationSource { null }
        var online = true
        val changes = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
        val viewModel = newViewModel(isOnline = { online }, connectivityChanges = changes)
        viewModel.onOpened()
        assertFalse(rated(viewModel.awaitSettled()).isOffline)

        online = false
        changes.tryEmit(Unit)
        assertTrue(rated(viewModel.awaitSettled()).isOffline)

        online = true
        changes.tryEmit(Unit)
        assertFalse(rated(viewModel.awaitSettled()).isOffline)
        assertEquals(1, stub.server.requestCount)
    }

    @Test
    fun UX_AC21_a_failing_network_change_source_keeps_the_offline_lead_read_on_open_and_logs_the_error() {
        stub.enqueueFixture()
        newViewModel().apply {
            onOpened()
            awaitSettled()
        }
        clock.advance(Duration.ofMinutes(90))
        locationSource = ApproximateLocationSource { null }
        val logged = mutableListOf<String>()
        val failing = flow<Unit> { throw SecurityException("callback refused") }
        val viewModel = newViewModel(isOnline = { false }, logError = { logged += it }, connectivityChanges = failing)

        viewModel.onOpened()

        assertTrue(rated(viewModel.awaitSettled()).isOffline)
        assertEquals(listOf(SecurityException::class.java.name), logged)
    }

    @Test
    fun UX_AC21_R02_a_change_source_that_fails_later_keeps_the_last_reading_and_the_view_model_keeps_working() {
        stub.enqueueFixture()
        newViewModel().apply {
            onOpened()
            awaitSettled()
        }
        clock.advance(Duration.ofMinutes(90))
        locationSource = ApproximateLocationSource { null }
        var online = true
        var refuse = false
        val logged = mutableListOf<String>()
        val changes = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
        // Like the system refusing the callback partway, for example above the per-app limit (a RuntimeException).
        val failingLater = changes.map { if (refuse) throw IllegalStateException("too many requests") }
        val viewModel = newViewModel(isOnline = { online }, logError = { logged += it }, connectivityChanges = failingLater)
        viewModel.onOpened()
        assertFalse(rated(viewModel.awaitSettled()).isOffline)

        online = false
        changes.tryEmit(Unit)
        assertTrue("a change before the failure still updates the lead", rated(viewModel.awaitSettled()).isOffline)

        refuse = true
        online = true
        changes.tryEmit(Unit)
        assertTrue("the failure itself is not treated as a change", rated(viewModel.awaitSettled()).isOffline)
        assertEquals(listOf(IllegalStateException::class.java.name), logged)

        viewModel.onShown()
        assertFalse("the screen shown again still re-reads the connection", rated(viewModel.awaitSettled()).isOffline)

        stub.enqueueFixture()
        locationSource = ApproximateLocationSource { fix }
        viewModel.refresh()
        val fresh = rated(viewModel.awaitSettled())
        assertNull("a refresh after the failure still runs a fresh check", fresh.ageMinutes)
        assertFalse(fresh.isOffline)
        assertEquals(2, stub.server.requestCount)
        assertEquals("only the one failure is logged", 1, logged.size)
    }

    @Test
    fun UX_AC21_R02_a_change_source_failing_on_open_logs_only_the_class_name() {
        stub.enqueueFixture()
        val logged = mutableListOf<String>()
        val failing = flow<Unit> { throw SecurityException("callback refused for uid 10123 and secret detail") }
        val viewModel = newViewModel(logError = { logged += it }, connectivityChanges = failing)

        viewModel.onOpened()
        val state = rated(viewModel.awaitSettled())

        assertNull("the check on open is unaffected", state.ageMinutes)
        assertFalse(state.isOffline)
        assertEquals(listOf("java.lang.SecurityException"), logged)
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
    @Volatile
    private var stopped = false

    override fun execute(command: Runnable) {
        if (stopped) throw RejectedExecutionException("executor is shut down")
        command.run()
    }

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
