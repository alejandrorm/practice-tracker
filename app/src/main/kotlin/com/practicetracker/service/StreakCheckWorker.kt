package com.practicetracker.service

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.practicetracker.MainActivity
import com.practicetracker.data.datastore.SettingsStore
import com.practicetracker.data.repository.SessionRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import java.time.LocalDate

@EntryPoint
@InstallIn(SingletonComponent::class)
interface StreakCheckEntryPoint {
    fun settingsStore(): SettingsStore
    fun sessionRepository(): SessionRepository
}

class StreakCheckWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val entryPoint = EntryPointAccessors.fromApplication<StreakCheckEntryPoint>(appContext)
        val settingsStore = entryPoint.settingsStore()
        val sessionRepository = entryPoint.sessionRepository()

        val settings = settingsStore.settings.first()
        if (!settings.streakRiskNotificationEnabled) return Result.success()

        val streak = sessionRepository.calculateCurrentStreak()
        if (streak < 3) return Result.success()

        // Check if user has already practiced today
        val today = LocalDate.now()
        val sessionsToday = sessionRepository.getSessionsInRange(today, today).first()
        if (sessionsToday.isNotEmpty()) return Result.success()

        showStreakAlertNotification(appContext, streak)
        return Result.success()
    }

    private fun showStreakAlertNotification(context: Context, streak: Int) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, NotificationChannels.STREAK_ALERT)
            .setSmallIcon(android.R.drawable.ic_menu_recent_history)
            .setContentTitle("Don't break your streak!")
            .setContentText("You're on a $streak-day streak. Practice today to keep it going.")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    companion object {
        const val WORK_NAME = "streak_check"
        private const val NOTIFICATION_ID = 2002
    }
}
