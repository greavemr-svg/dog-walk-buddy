package io.github.greavemrsvg.dogwalkbuddy.service

import android.content.Intent
import android.content.pm.ServiceInfo
import android.util.Log
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import io.github.greavemrsvg.dogwalkbuddy.DogWalkBuddyApp
import io.github.greavemrsvg.dogwalkbuddy.data.db.WalkStatus
import io.github.greavemrsvg.dogwalkbuddy.domain.walk.FixResult
import io.github.greavemrsvg.dogwalkbuddy.domain.walk.RouteDistanceCalculator
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.time.Instant

/**
 * Records a walk in a foreground service of type `location` (ADR 0005). It is started from the visible activity and
 * posts the ongoing notification at once. It stores every accepted fix as it arrives (ADR 0004), so a killed
 * process loses at most the fix in flight (AC34). It returns `START_NOT_STICKY`: a system restart could not legally
 * restart foreground location, so recovery happens on the next launch instead. It makes no network call, and it
 * never logs a location.
 */
class WalkRecordingService : LifecycleService() {
    private var recording: Job? = null

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_START -> begin(intent.getLongExtra(EXTRA_WALK_ID, NO_WALK), startId)
            ACTION_STOP -> stopRecording(startId)
            else -> stopSelf(startId)
        }
        return START_NOT_STICKY
    }

    private fun container() = (application as DogWalkBuddyApp).container

    private fun begin(
        walkId: Long,
        startId: Int,
    ) {
        // Calling startForeground first keeps the system's promise to show the notification within seconds.
        if (!enterForeground()) {
            lifecycleScope.launch { discard(walkId, startId) }
            return
        }
        if (recording?.isActive == true) return
        recording = lifecycleScope.launch { record(walkId, startId) }
    }

    private fun enterForeground(): Boolean =
        try {
            ServiceCompat.startForeground(
                this,
                RecordingNotification.NOTIFICATION_ID,
                RecordingNotification.build(this),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION,
            )
            true
        } catch (error: SecurityException) {
            logFailure(error)
            false
        } catch (error: IllegalStateException) {
            // The system refused a foreground start (for example the app was not visible).
            logFailure(error)
            false
        }

    private suspend fun discard(
        walkId: Long,
        startId: Int,
    ) {
        val container = container()
        container.recordingStateHolder.clear()
        if (walkId != NO_WALK) container.walkRepository.discard(walkId)
        stopSelf(startId)
    }

    private suspend fun record(
        walkId: Long,
        startId: Int,
    ) {
        val container = container()
        val repository = container.walkRepository
        val holder = container.recordingStateHolder
        val walk = repository.walk(walkId)
        if (walk == null || walk.status != WalkStatus.RECORDING) {
            holder.clear()
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf(startId)
            return
        }
        holder.begin(walkId, Instant.ofEpochMilli(walk.startUtcMs))
        val calculator = RouteDistanceCalculator()
        var seq = repository.nextSeq(walkId)
        container.walkLocationSource.fixes().collect { fix ->
            val result = calculator.add(fix)
            if (result is FixResult.Accepted && repository.recordFix(walkId, seq, fix, result.totalM)) {
                seq++
                holder.updateDistance(result.totalM)
            }
        }
    }

    private fun stopRecording(startId: Int) {
        val job = recording
        lifecycleScope.launch {
            // Wait for a fix that is being stored, so the end never races a write.
            job?.cancel()
            job?.join()
            val container = container()
            val walkId =
                container.recordingStateHolder.state.value
                    ?.walkId
            if (walkId != null) container.walkRepository.finishWalk(walkId)
            container.recordingStateHolder.clear()
            ServiceCompat.stopForeground(this@WalkRecordingService, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf(startId)
        }
    }

    override fun onDestroy() {
        // A service that ends without Stop leaves its row as RECORDING, which recovery saves on the next launch.
        container().recordingStateHolder.clear()
        super.onDestroy()
    }

    private fun logFailure(error: Exception) {
        // Only the class name is logged: no message, no coordinates.
        Log.println(Log.WARN, LOG_TAG, "Foreground start refused: ${error.javaClass.simpleName}")
    }

    companion object {
        const val ACTION_START = "io.github.greavemrsvg.dogwalkbuddy.action.START_WALK"
        const val ACTION_STOP = "io.github.greavemrsvg.dogwalkbuddy.action.STOP_WALK"
        const val EXTRA_WALK_ID = "io.github.greavemrsvg.dogwalkbuddy.extra.WALK_ID"
        private const val NO_WALK = -1L
        private const val LOG_TAG = "WalkRecording"
    }
}
