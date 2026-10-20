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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
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
    private var permissionGranted = true
    private var locationCalls = 0
    private var locationSource = ApproximateLocationSource { fix }
    private val sensitivity = MutableStateFlow(HeatSensitivity.NORMAL)
    private val hasProfile = MutableStateFlow(true)

    @Before
    fun setUp() {
        // Real (not virtual) time on the main dispatcher, so the location timeout and OkHttp threads behave as in the app.
        Dispatchers.setMain(Dispatchers.Unconfined)
        stub = StubWeatherServer(temporaryFolder.newFolder())
        database =
            Room
                .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), AppDatabase::class.java)
                .allowMainThreadQueries()
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
        CheckRepository(MetNorwayClient(stub.baseUrl, stub.httpClient, Dispatchers.Unconfined), dao, clock)

    private fun newViewModel(
        repo: CheckRepository = repository,
        locationTimeout: Duration = CheckViewModel.LOCATION_TIMEOUT,
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
    )

    private fun CheckViewModel.awaitSettled(): CheckUiState =
        runBlocking { withTimeout(AWAIT_MS) { state.first { it !is CheckUiState.Loading } } }

    private fun rated(state: CheckUiState) = state as CheckUiState.Rated

    // The state is recomputed on the flow's own dispatch, so wait for the change instead of reading it at once.
    private fun CheckViewModel.awaitRated(accept: (CheckUiState.Rated) -> Boolean): CheckUiState.Rated =
        runBlocking { withTimeout(AWAIT_MS) { state.first { it is CheckUiState.Rated && accept(it) } } } as CheckUiState.Rated

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

    private companion object {
        const val SERVER_ERROR = 500
        const val AWAIT_MS = 10_000L
        const val LOCATION_TIMEOUT_MS = 50L

        // Inside the first hourly period of the fixture, which starts 14:00 UTC on 19 October 2026.
        val START: Instant = Instant.parse("2026-10-19T14:20:00Z")
    }
}
