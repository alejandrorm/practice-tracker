package com.practicetracker.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

object NotificationChannels {

    const val SESSION_IN_PROGRESS = "session_in_progress"
    const val PRACTICE_REMINDER   = "practice_reminder"
    const val STREAK_ALERT        = "streak_alert"
    const val ACHIEVEMENT         = "achievement"

    fun createAll(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannels(
            listOf(
                NotificationChannel(
                    SESSION_IN_PROGRESS,
                    "Session in progress",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Shown while a practice session is active"
                },
                NotificationChannel(
                    PRACTICE_REMINDER,
                    "Practice reminders",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Reminds you to practice at your scheduled time"
                },
                NotificationChannel(
                    STREAK_ALERT,
                    "Streak alerts",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Warns you when your practice streak is at risk"
                },
                NotificationChannel(
                    ACHIEVEMENT,
                    "Achievements",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Notifies you when you earn a new achievement"
                }
            )
        )
    }
}
