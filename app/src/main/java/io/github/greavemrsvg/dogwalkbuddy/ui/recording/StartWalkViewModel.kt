package io.github.greavemrsvg.dogwalkbuddy.ui.recording

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.greavemrsvg.dogwalkbuddy.AppContainer
import io.github.greavemrsvg.dogwalkbuddy.service.RecordingStateHolder
import io.github.greavemrsvg.dogwalkbuddy.service.WalkRecordingController
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** What the Start walk control on Home shows. */
sealed interface StartWalkUiState {
    data object Idle : StartWalkUiState

    /** A walk is being recorded: the control says "Walk in progress" and returns to the recording screen. */
    data object InProgress : StartWalkUiState

    /** Precise location is not granted: the explanation and a grant control are shown, and nothing starts (AC33). */
    data object NeedsPreciseLocation : StartWalkUiState

    /** Precise location was refused and the system will not ask again: only Settings can grant it (AC33). */
    data object PreciseLocationBlocked : StartWalkUiState
}

/**
 * The Start walk control (ADR 0005, start sequence). It checks precise location first and starts nothing without
 * it (AC33). Permission dialogs are shown by the screen, which calls back here with the result.
 */
class StartWalkViewModel(
    private val controller: WalkRecordingController,
    holder: RecordingStateHolder,
    private val hasFinePermission: () -> Boolean,
) : ViewModel() {
    private enum class Prompt { NONE, NEEDED, BLOCKED }

    private val prompt = MutableStateFlow(Prompt.NONE)
    private val startedEvents = Channel<Unit>(Channel.BUFFERED)

    val state: StateFlow<StartWalkUiState> =
        combine(holder.state, prompt) { session, current ->
            when {
                session != null -> StartWalkUiState.InProgress
                current == Prompt.NEEDED -> StartWalkUiState.NeedsPreciseLocation
                current == Prompt.BLOCKED -> StartWalkUiState.PreciseLocationBlocked
                else -> StartWalkUiState.Idle
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, StartWalkUiState.Idle)

    /** Emits once each time a walk has been started, so the screen can open the recording screen. */
    val started: Flow<Unit> = startedEvents.receiveAsFlow()

    /** The Start tap. True when precise location is granted and the start may go ahead. */
    fun onStartTapped(): Boolean {
        if (!hasFinePermission()) {
            prompt.value = Prompt.NEEDED
            return false
        }
        prompt.value = Prompt.NONE
        return true
    }

    /** The result of the precise location request. True when it was granted and the start may go ahead. */
    fun onPreciseLocationResult(
        granted: Boolean,
        canAskAgain: Boolean,
    ): Boolean {
        prompt.value =
            when {
                granted -> Prompt.NONE
                canAskAgain -> Prompt.NEEDED
                else -> Prompt.BLOCKED
            }
        return granted
    }

    /** The screen started again: a permission granted in Settings clears the explanation. */
    fun onShown() {
        if (hasFinePermission()) prompt.value = Prompt.NONE
    }

    /** Inserts the walk and starts the service. Reports through [started] when the walk is recording. */
    fun start() {
        viewModelScope.launch {
            if (controller.start()) startedEvents.send(Unit)
        }
    }

    companion object {
        fun factory(container: AppContainer) =
            viewModelFactory {
                initializer {
                    StartWalkViewModel(
                        controller = container.walkRecordingController,
                        holder = container.recordingStateHolder,
                        hasFinePermission = container.hasFineLocationPermission,
                    )
                }
            }
    }
}
