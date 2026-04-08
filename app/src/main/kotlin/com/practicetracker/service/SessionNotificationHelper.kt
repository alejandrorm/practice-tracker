package com.practicetracker.service

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.practicetracker.MainActivity

object SessionNotificationHelper {
    const val NOTIFICATION_ID = 1001

    fun buildNotification(context: Context, elapsedSeconds: Long): Notification {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val elapsed = "%02d:%02d".format(elapsedSeconds / 60, elapsedSeconds % 60)
        return NotificationCompat.Builder(context, NotificationChannels.SESSION_IN_PROGRESS)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("Practice session in progress")
            .setContentText("Elapsed: $elapsed")
            .setContentIntent(pendingIntent)
            .addAction(0, "Return to session", pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }
}
