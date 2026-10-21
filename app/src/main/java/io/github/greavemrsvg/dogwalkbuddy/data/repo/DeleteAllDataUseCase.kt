package io.github.greavemrsvg.dogwalkbuddy.data.repo

import io.github.greavemrsvg.dogwalkbuddy.data.db.AppDatabase
import io.github.greavemrsvg.dogwalkbuddy.map.MapCacheClearer
import io.github.greavemrsvg.dogwalkbuddy.service.RecordingStateHolder
import io.github.greavemrsvg.dogwalkbuddy.service.WalkRecordingController
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Duration

/** Deleting could not be completed. Some data may be left, so the user can try again. */
class DeleteAllDataException(
    cause: Throwable,
) : Exception("Not all data could be deleted", cause)

/**
 * "Delete all my data" (AC47, ADR 0004). It stops any active recording first, then removes every table (walks,
 * route points, the profile and the cached check), the photo copies, the weather HTTP cache and the map tile cache.
 * Every step runs even if an earlier one fails, so as much as possible is deleted, and the first failure is then
 * reported. All steps can safely be run again.
 */
class DeleteAllDataUseCase(
    private val recordingController: WalkRecordingController,
    private val recordingState: RecordingStateHolder,
    private val database: AppDatabase,
    private val photoStore: PhotoStore,
    private val evictHttpCache: () -> Unit,
    private val mapCache: MapCacheClearer,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val stopTimeout: Duration = STOP_TIMEOUT,
) {
    private val mutableDeletions = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** Emits after the database has been emptied, so screens holding data in memory can forget it. */
    val deletions: Flow<Unit> = mutableDeletions.asSharedFlow()

    suspend operator fun invoke() {
        // A walk still being recorded would otherwise write its next fix into a database that was just emptied.
        stopRecording()
        val failures = mutableListOf<Throwable>()
        attempt(failures) {
            withContext(ioDispatcher) { database.clearAllTables() }
            mutableDeletions.tryEmit(Unit)
        }
        attempt(failures) { photoStore.deleteAll() }
        attempt(failures) { withContext(ioDispatcher) { evictHttpCache() } }
        attempt(failures) { mapCache.clear() }
        val first = failures.firstOrNull() ?: return
        failures.drop(1).forEach { first.addSuppressed(it) }
        throw DeleteAllDataException(first)
    }

    private suspend fun stopRecording() {
        if (!recordingState.isRecording) return
        recordingController.stop()
        val stopped =
            withTimeoutOrNull(stopTimeout.toMillis()) {
                recordingState.state.first { it == null }
                true
            }
        // If the walk did not stop, deleting now would leave a recording running after the data is gone.
        if (stopped == null) throw DeleteAllDataException(IllegalStateException("Recording did not stop"))
    }

    private suspend fun attempt(
        failures: MutableList<Throwable>,
        step: suspend () -> Unit,
    ) {
        try {
            step()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            failures += error
        }
    }

    private companion object {
        val STOP_TIMEOUT: Duration = Duration.ofSeconds(5)
    }
}
