package io.github.greavemrsvg.dogwalkbuddy.ui.walkdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.greavemrsvg.dogwalkbuddy.AppContainer
import io.github.greavemrsvg.dogwalkbuddy.data.net.ConnectivityStatus
import io.github.greavemrsvg.dogwalkbuddy.data.repo.WalkRepository
import io.github.greavemrsvg.dogwalkbuddy.domain.location.LatLon
import io.github.greavemrsvg.dogwalkbuddy.domain.walk.WalkSummary
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant

sealed interface WalkDetailUiState {
    data object Loading : WalkDetailUiState

    data object NotFound : WalkDetailUiState

    data object LoadFailed : WalkDetailUiState

    /**
     * A saved walk. [mapAvailable] is false when the phone was offline as the detail opened or the map failed to
     * load, and the screen then shows "Map not available offline" in its place (AC39).
     */
    data class Loaded(
        val walk: WalkSummary,
        val route: List<LatLon>,
        val mapAvailable: Boolean,
    ) : WalkDetailUiState
}

/** Loads one saved walk and its route for the detail screen (AC38, AC39). */
class WalkDetailViewModel(
    private val walkId: Long,
    private val repository: WalkRepository,
    private val connectivity: ConnectivityStatus,
) : ViewModel() {
    private val mutableState = MutableStateFlow<WalkDetailUiState>(WalkDetailUiState.Loading)
    val state: StateFlow<WalkDetailUiState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        mutableState.value =
            try {
                val walk = repository.walk(walkId)
                if (walk == null) {
                    WalkDetailUiState.NotFound
                } else {
                    val route = repository.routePoints(walkId).map { LatLon(it.lat, it.lon) }
                    val end = walk.endUtcMs ?: walk.lastFixUtcMs ?: walk.startUtcMs
                    WalkDetailUiState.Loaded(
                        walk =
                            WalkSummary(
                                id = walk.id,
                                startUtc = Instant.ofEpochMilli(walk.startUtcMs),
                                durationSeconds = ((end - walk.startUtcMs) / MILLIS_PER_SECOND).coerceAtLeast(0),
                                distanceM = walk.distanceM,
                            ),
                        route = route,
                        // The phone is asked once, as the detail opens: offline, the map is never created (ADR 0003).
                        mapAvailable = connectivity.isOnline(),
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                // Nothing about the walk is logged.
                WalkDetailUiState.LoadFailed
            }
    }

    /** The map reported that it could not load its style or tiles (AC39). */
    fun onMapLoadFailed() {
        mutableState.update { current -> if (current is WalkDetailUiState.Loaded) current.copy(mapAvailable = false) else current }
    }

    companion object {
        private const val MILLIS_PER_SECOND = 1000L

        fun factory(
            container: AppContainer,
            walkId: Long,
        ) = viewModelFactory {
            initializer { WalkDetailViewModel(walkId, container.walkRepository, container.connectivity) }
        }
    }
}
