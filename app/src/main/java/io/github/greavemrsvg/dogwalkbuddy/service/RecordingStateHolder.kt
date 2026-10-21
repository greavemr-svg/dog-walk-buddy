package io.github.greavemrsvg.dogwalkbuddy.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.Instant

/** The walk being recorded right now. Elapsed time is `clock.now - startUtc`, never a sum of fixes (AC29). */
data class RecordingSession(
    val walkId: Long,
    val startUtc: Instant,
    val distanceM: Double = 0.0,
)

/**
 * Process-local state of the recording (ADR 0005). The controller and the service publish it and the screens
 * observe it. It is empty in a fresh process, which is what marks a `RECORDING` row as interrupted (AC34).
 */
class RecordingStateHolder {
    private val mutableState = MutableStateFlow<RecordingSession?>(null)

    val state: StateFlow<RecordingSession?> = mutableState.asStateFlow()

    val isRecording: Boolean get() = mutableState.value != null

    /** Publishes the session. A session already published for the same walk is kept, with its distance. */
    fun begin(
        walkId: Long,
        startUtc: Instant,
    ) {
        mutableState.update { current -> current?.takeIf { it.walkId == walkId } ?: RecordingSession(walkId, startUtc) }
    }

    fun updateDistance(distanceM: Double) {
        mutableState.update { it?.copy(distanceM = distanceM) }
    }

    fun clear() {
        mutableState.value = null
    }

    /** Clears the session only if it is for [walkId], so a late end of an old service cannot drop a newer walk. */
    fun clearIf(walkId: Long) {
        mutableState.update { current -> current?.takeUnless { it.walkId == walkId } }
    }
}
