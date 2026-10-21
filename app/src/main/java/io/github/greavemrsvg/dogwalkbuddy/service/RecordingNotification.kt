package io.github.greavemrsvg.dogwalkbuddy.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import io.github.greavemrsvg.dogwalkbuddy.MainActivity
import io.github.greavemrsvg.dogwalkbuddy.R

/**
 * The persistent "recording your walk" notification (AC28, ADR 0005). It is ongoing, opens the app when tapped and
 * has no stop action, because stopping from the notification is out of scope. It never shows a location.
 */
object RecordingNotification {
    const val CHANNEL_ID = "walk_recording"
    const val NOTIFICATION_ID = 1

    fun build(context: Context): Notification {
        ensureChannel(context)
        val launch = Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val openApp = PendingIntent.getActivity(context, 0, launch, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        return NotificationCompat
            .Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_walk)
            .setContentTitle(context.getString(R.string.recording_notification_title))
            .setContentText(context.getString(R.string.recording_notification_text))
            .setContentIntent(openApp)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        val name = context.getString(R.string.recording_notification_channel)
        manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_LOW))
    }
}
