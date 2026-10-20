package io.github.greavemrsvg.dogwalkbuddy.ui.check

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.greavemrsvg.dogwalkbuddy.AppContainer
import io.github.greavemrsvg.dogwalkbuddy.data.location.ApproximateLocationSource
import io.github.greavemrsvg.dogwalkbuddy.data.repo.CheckRepository
import io.github.greavemrsvg.dogwalkbuddy.domain.check.CachePolicy
import io.github.greavemrsvg.dogwalkbuddy.domain.check.CheckData
import io.github.greavemrsvg.dogwalkbuddy.domain.check.CheckOutcome
import io.github.greavemrsvg.dogwalkbuddy.domain.check.CheckRater
import io.github.greavemrsvg.dogwalkbuddy.domain.location.LocationFix
import io.github.greavemrsvg.dogwalkbuddy.domain.rating.HeatSensitivity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Clock
import java.time.Duration

/**
 * The single source of truth for the check card (design, "The check flow"). A check runs only when the screen
 * is opened with nothing usable in memory ([onOpened]) or when the user taps refresh or retry ([refresh]).
 * There are no timers here (AC24). The screen calls [onShown] when it starts again, which makes no request. The heat sensitivity is combined with the loaded
 * hours, so changing the dog profile re-rates the data in memory with no network request (AC44).
 */
class CheckViewModel(
    private val repository: CheckRepository,
    private val locationSource: ApproximateLocationSource,
    sensitivity: Flow<HeatSensitivity>,
    hasProfile: Flow<Boolean>,
    private val hasCoarsePermission: () -> Boolean,
    private val clock: Clock,
    private val locationTimeout: Duration = LOCATION_TIMEOUT,
    // Only the class name of an unexpected error is logged: no message, no coordinates.
    private val logError: (String) -> Unit = { name -> Log.println(Log.WARN, LOG_TAG, "Check step failed: $name") },
) : ViewModel() {
    private sealed interface Source {
        data object Idle : Source

        data object Loading : Source

        data object NeedsPermission : Source

        /** Permission was refused and the system will not ask again, so only Settings can grant it. */
        data object PermissionBlocked : Source

        data class Loaded(
            val data: CheckData,
        ) : Source
    }

    private val source = MutableStateFlow<Source>(Source.Idle)

    // Bumped when the screen is shown again, so the state is recomputed with the current time (the age label).
    private val shown = MutableStateFlow(0)
    private var refreshJob: Job? = null

    val state: StateFlow<CheckUiState> =
        combine(source, sensitivity, hasProfile, shown) { current, heat, profileExists, _ -> toUiState(current, heat, profileExists) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, CheckUiState.Loading)

    /**
     * The screen was opened. A check runs unless a check is already running or a result under 3 h old is in
     * memory, in which case that result is simply shown again (and re-rated, see [state]).
     */
    fun onOpened() {
        if (refreshJob?.isActive == true) return
        if (hasUsableResultInMemory()) {
            shown.update { it + 1 }
            return
        }
        refresh()
    }

    /**
     * The screen started again (for example after the app was in the background). This makes no request: it only
     * re-evaluates what is shown against the current time (so the age label is current and a result that has
     * become 3 h old is no longer shown), and it picks up a permission granted in Settings (AC21, AC22, AC25).
     */
    fun onShown() {
        val waitingForPermission = source.value == Source.NeedsPermission || source.value == Source.PermissionBlocked
        if (waitingForPermission && hasCoarsePermission() && refreshJob?.isActive != true) {
            refresh()
        } else {
            shown.update { it + 1 }
        }
    }

    /** The system permission dialog closed. [canAskAgain] is false after a permanent refusal (AC25). */
    fun onPermissionResult(
        granted: Boolean,
        canAskAgain: Boolean,
    ) {
        if (granted) {
            refresh()
        } else if (!canAskAgain) {
            refreshJob?.cancel()
            source.value = Source.PermissionBlocked
        } else {
            source.value = Source.NeedsPermission
        }
    }

    /** Refresh or retry was tapped, or location permission was just granted. */
    fun refresh() {
        refreshJob?.cancel()
        refreshJob =
            viewModelScope.launch {
                if (!hasCoarsePermission()) {
                    source.value = Source.NeedsPermission
                    return@launch
                }
                source.value = Source.Loading
                source.value = Source.Loaded(loadData(readLocation()))
            }
    }

    private suspend fun readLocation(): LocationFix? =
        try {
            withTimeoutOrNull(locationTimeout.toMillis()) { locationSource.current() }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            // No location is a normal outcome: the repository falls back to a recent stored check.
            logError(error.javaClass.name)
            null
        }

    private suspend fun loadData(fix: LocationFix?): CheckData =
        try {
            repository.check(fix)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            // Anything unexpected (for example a database failure) must end as "Can't check right now", not a crash.
            logError(error.javaClass.name)
            CheckData.Unavailable
        }

    private fun hasUsableResultInMemory(): Boolean {
        val data = (source.value as? Source.Loaded)?.data as? CheckData.Available ?: return false
        val now = clock.instant()
        // Sensitivity does not change whether the current hour has data, so Normal is enough for this test.
        return CachePolicy.isUsable(data.fetchedAt, now) && CheckRater.rate(data, HeatSensitivity.NORMAL, now) is CheckOutcome.Rated
    }

    private fun toUiState(
        current: Source,
        sensitivity: HeatSensitivity,
        hasProfile: Boolean,
    ): CheckUiState =
        when (current) {
            Source.Idle, Source.Loading -> CheckUiState.Loading
            Source.NeedsPermission -> CheckUiState.NeedsLocationPermission
            Source.PermissionBlocked -> CheckUiState.LocationPermissionBlocked
            is Source.Loaded -> rated(current.data, sensitivity, hasProfile)
        }

    private fun rated(
        data: CheckData,
        sensitivity: HeatSensitivity,
        hasProfile: Boolean,
    ): CheckUiState {
        if (data !is CheckData.Available) return CheckUiState.CantCheck
        val now = clock.instant()
        // Under the offline rule (AC22) nothing 3 h old or older is ever shown, however long the screen stayed open.
        if (!CachePolicy.isUsable(data.fetchedAt, now)) return CheckUiState.CantCheck
        val outcome = CheckRater.rate(data, sensitivity, now) as? CheckOutcome.Rated ?: return CheckUiState.CantCheck
        val ageMinutes = CachePolicy.age(outcome.fetchedAt, now).toMinutes().coerceAtLeast(0)
        return CheckUiState.Rated(
            current = outcome.current,
            window = outcome.window,
            ageMinutes = ageMinutes.takeIf { outcome.isCached || it >= MIN_AGE_TO_LABEL_MINUTES },
            usingDefaultSensitivity = !hasProfile,
        )
    }

    companion object {
        /** The location read gives up after this long (design, step 3). */
        val LOCATION_TIMEOUT: Duration = Duration.ofSeconds(10)

        private const val LOG_TAG = "CheckViewModel"

        /** A result from this session is labelled with its age once it is at least this old. */
        const val MIN_AGE_TO_LABEL_MINUTES = 1L

        fun factory(container: AppContainer) =
            viewModelFactory {
                initializer {
                    CheckViewModel(
                        repository = container.checkRepository,
                        locationSource = container.approximateLocationSource,
                        sensitivity = container.profileRepository.observeSensitivity(),
                        hasProfile = container.profileRepository.observeProfile().map { it != null },
                        hasCoarsePermission = container.hasCoarseLocationPermission,
                        clock = container.clock,
                    )
                }
            }
    }
}
