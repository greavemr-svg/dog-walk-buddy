package io.github.greavemrsvg.dogwalkbuddy.service

import android.util.Log
import io.github.greavemrsvg.dogwalkbuddy.data.repo.RecoverInterruptedWalkUseCase
import io.github.greavemrsvg.dogwalkbuddy.data.repo.WalkRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * What the screens call to start and stop a walk (ADR 0005, start sequence). Starting and recovering share a lock,
 * so recovery at app start can never complete a walk that the user has just started.
 */
class WalkRecordingController(
    private val repository: WalkRepository,
    private val holder: RecordingStateHolder,
    private val launcher: WalkServiceLauncher,
    private val recovery: RecoverInterruptedWalkUseCase,
    // Only the class name is logged: no message, no coordinates.
    private val logError: (String) -> Unit = { name -> Log.println(Log.WARN, LOG_TAG, "Walk start failed: $name") },
) {
    private val lock = Mutex()

    /**
     * Inserts the `RECORDING` row, publishes the session and starts the service. Returns false when a walk is
     * already being recorded or the service could not be started (the new row is then removed).
     */
    suspend fun start(): Boolean =
        lock.withLock {
            if (holder.isRecording) return@withLock false
            val walk = repository.startWalk()
            holder.begin(walk.id, walk.startUtc)
            try {
                launcher.start(walk.id)
                true
            } catch (error: RuntimeException) {
                logError(error.javaClass.simpleName)
                holder.clear()
                repository.discard(walk.id)
                false
            }
        }

    /** Asks the service to save the walk and stop. The service ends the walk, so fixes and the end never race. */
    fun stop() {
        if (holder.isRecording) launcher.stop()
    }

    /** Saves any walk interrupted by a process kill (AC34). Returns how many were saved. */
    suspend fun recoverInterrupted(): Int = lock.withLock { recovery() }

    private companion object {
        const val LOG_TAG = "WalkRecording"
    }
}
