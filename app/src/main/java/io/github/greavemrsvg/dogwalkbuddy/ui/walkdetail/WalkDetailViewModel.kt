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
import java.time.Clock
import java.time.Instant

sealed interface WalkDetailUiState {
    data object Loading : WalkDetailUiState

    data object NotFound : WalkDetailUiState

    data object LoadFailed : WalkDetailUiState

    /**
     * A saved walk. [mapAvailable] is false when the phone was offline as the detail opened or the map failed to
     * load, and the screen then shows "Map not available offline" in its place (AC39, AC47).
     */
    data class Loaded(
        val walk: WalkSummary,
        val route: List<LatLon>,
        val mapAvailable: Boolean,
    ) : WalkDetailUiState
}

/**
 * The delete flow of the detail (AC48): [confirming] while the sheet is open, [deleted] once the walk is gone (the
 * screen then returns to Walks), and [failed] when the delete did not happen.
 */
data class WalkDeletionState(
    val confirming: Boolean = false,
    val deleted: Boolean = false,
    val failed: Boolean = false,
)

/**
 * Loads one saved walk and its route for the detail screen, and deletes it on request (AC38, AC39, AC48).
 * [clock] is what the screen's date text uses for the current year (D8).
 */
class WalkDetailViewModel(
    private val walkId: Long,
    private val repository: WalkRepository,
    private val connectivity: ConnectivityStatus,
    val clock: Clock = Clock.systemDefaultZone(),
) : ViewModel() {
    private val mutableState = MutableStateFlow<WalkDetailUiState>(WalkDetailUiState.Loading)
    val state: StateFlow<WalkDetailUiState> = mutableState.asStateFlow()

    private val mutableDeletion = MutableStateFlow(WalkDeletionState())
    val deletion: StateFlow<WalkDeletionState> = mutableDeletion.asStateFlow()

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

    /** "Delete this walk" was tapped: ask first, nothing is deleted yet (AC48). */
    fun requestDelete() {
        mutableDeletion.value = WalkDeletionState(confirming = true)
    }

    /** Cancel, system Back or a scrim tap: the sheet closes and nothing changes (AC48). */
    fun cancelDelete() {
        mutableDeletion.update { it.copy(confirming = false) }
    }

    /** "Delete walk" was confirmed: the walk and its route are removed (AC48). */
    fun confirmDelete() {
        if (!mutableDeletion.value.confirming) return
        mutableDeletion.value = WalkDeletionState()
        viewModelScope.launch {
            try {
                repository.deleteWalk(walkId)
                mutableDeletion.update { it.copy(deleted = true) }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                // Nothing about the walk is logged.
                mutableDeletion.update { it.copy(failed = true) }
            }
        }
    }

    /** The delete failure message was dismissed. */
    fun dismissDeleteFailure() {
        mutableDeletion.update { it.copy(failed = false) }
    }

    companion object {
        private const val MILLIS_PER_SECOND = 1000L

        fun factory(
            container: AppContainer,
            walkId: Long,
        ) = viewModelFactory {
            initializer { WalkDetailViewModel(walkId, container.walkRepository, container.connectivity, container.clock) }
        }
    }
}
