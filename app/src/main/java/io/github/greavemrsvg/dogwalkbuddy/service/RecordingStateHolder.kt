package io.github.greavemrsvg.dogwalkbuddy.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
        mutableState.value = mutableState.value?.takeIf { it.walkId == walkId } ?: RecordingSession(walkId, startUtc)
    }

    fun updateDistance(distanceM: Double) {
        mutableState.value = mutableState.value?.copy(distanceM = distanceM)
    }

    fun clear() {
        mutableState.value = null
    }
}
