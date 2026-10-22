package io.github.greavemrsvg.dogwalkbuddy.service

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

/** Starts and stops [WalkRecordingService]. Screen tests inject a fake. */
interface WalkServiceLauncher {
    fun start(walkId: Long)

    fun stop()
}

/** Starts the service from the visible activity, so it keeps "while in use" location access (ADR 0005). */
class AndroidWalkServiceLauncher(
    private val context: Context,
) : WalkServiceLauncher {
    override fun start(walkId: Long) {
        val intent = serviceIntent(WalkRecordingService.ACTION_START).putExtra(WalkRecordingService.EXTRA_WALK_ID, walkId)
        ContextCompat.startForegroundService(context, intent)
    }

    override fun stop() {
        context.startService(serviceIntent(WalkRecordingService.ACTION_STOP))
    }

    private fun serviceIntent(action: String) = Intent(context, WalkRecordingService::class.java).setAction(action)
}
