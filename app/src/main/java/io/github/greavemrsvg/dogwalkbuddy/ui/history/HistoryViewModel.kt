package io.github.greavemrsvg.dogwalkbuddy.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.greavemrsvg.dogwalkbuddy.AppContainer
import io.github.greavemrsvg.dogwalkbuddy.data.repo.WalkRepository
import io.github.greavemrsvg.dogwalkbuddy.domain.walk.DayDot
import io.github.greavemrsvg.dogwalkbuddy.domain.walk.WalkSummary
import io.github.greavemrsvg.dogwalkbuddy.domain.walk.WeekGroup
import io.github.greavemrsvg.dogwalkbuddy.domain.walk.WeeklyTotals
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.ZoneId

data class HistoryUiState(
    val isLoaded: Boolean,
    /** The current Monday-to-Sunday week. Always present, with zero totals when it has no walks (AC37). */
    val thisWeek: WeekGroup,
    /** The weeks that have walks, newest first. */
    val weeks: List<WeekGroup>,
    /** The walk the user asked to delete and has not yet confirmed or cancelled (AC40). */
    val pendingDelete: WalkSummary? = null,
    val deleteFailed: Boolean = false,
    val loadFailed: Boolean = false,
    /** The seven days of the current week, Monday to Sunday, for the this-week card's dots (AC41). */
    val thisWeekDays: List<DayDot> = emptyList(),
) {
    val isEmpty: Boolean get() = weeks.isEmpty()
}

/**
 * Holds the history: weekly groups from the saved walks, the current week's day dots, and the delete confirmation
 * (AC35, AC36, AC37, AC40, AC41). [clock] is also what the screen's date text uses for the current year (D7).
 */
class HistoryViewModel(
    private val repository: WalkRepository,
    val clock: Clock,
    private val zoneId: ZoneId,
) : ViewModel() {
    private val pendingDelete = MutableStateFlow<WalkSummary?>(null)
    private val deleteFailed = MutableStateFlow(false)
    private val refreshes = MutableStateFlow(0)

    val state: StateFlow<HistoryUiState> =
        combine(repository.observeSummaries(), pendingDelete, deleteFailed, refreshes) { walks, pending, failed, _ ->
            HistoryUiState(
                isLoaded = true,
                thisWeek = WeeklyTotals.thisWeek(walks, zoneId, clock.instant()),
                weeks = WeeklyTotals.group(walks, zoneId),
                thisWeekDays = WeeklyTotals.dayDots(walks, zoneId, clock.instant()),
                pendingDelete = pending,
                deleteFailed = failed,
            )
        }.catch { error ->
            if (error is CancellationException) throw error
            // Nothing about the walks is logged. The user is told and can leave and come back.
            emit(loadFailedState())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyState(isLoaded = false))

    fun requestDelete(walk: WalkSummary) {
        deleteFailed.value = false
        pendingDelete.value = walk
    }

    /** Cancel leaves everything as it was (AC40). */
    fun cancelDelete() {
        pendingDelete.value = null
    }

    fun confirmDelete() {
        val walk = pendingDelete.value ?: return
        pendingDelete.value = null
        viewModelScope.launch {
            try {
                repository.deleteWalk(walk.id)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                deleteFailed.update { true }
            }
        }
    }

    /** Recomputes "This week" against the current time, for when the screen is shown again (AC37). */
    fun refresh() {
        refreshes.update { it + 1 }
    }

    fun dismissDeleteFailure() {
        deleteFailed.value = false
    }

    private fun emptyState(isLoaded: Boolean) =
        HistoryUiState(
            isLoaded = isLoaded,
            thisWeek = WeeklyTotals.thisWeek(emptyList(), zoneId, clock.instant()),
            weeks = emptyList(),
            thisWeekDays = WeeklyTotals.dayDots(emptyList(), zoneId, clock.instant()),
        )

    private fun loadFailedState() = emptyState(isLoaded = true).copy(loadFailed = true)

    companion object {
        private const val STOP_TIMEOUT_MS = 5_000L

        fun factory(container: AppContainer) =
            viewModelFactory {
                initializer { HistoryViewModel(container.walkRepository, container.clock, container.zoneId) }
            }
    }
}
