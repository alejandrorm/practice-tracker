package com.practicetracker

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.practicetracker.service.NotificationChannels
import com.practicetracker.service.StreakCheckWorker
import dagger.hilt.android.HiltAndroidApp
import java.time.LocalTime
import java.util.concurrent.TimeUnit

@HiltAndroidApp
class PracticeTrackerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationChannels.createAll(this)
        scheduleStreakCheck()
    }

    private fun scheduleStreakCheck() {
        // Target 8:00 PM local time; compute initial delay from now
        val now = LocalTime.now()
        val target = LocalTime.of(20, 0)
        val initialDelayMinutes = if (now.isBefore(target)) {
            now.until(target, java.time.temporal.ChronoUnit.MINUTES)
        } else {
            // Already past 8 PM — schedule for tomorrow
            now.until(target, java.time.temporal.ChronoUnit.MINUTES) + 24 * 60
        }

        val request = PeriodicWorkRequestBuilder<StreakCheckWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(initialDelayMinutes, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            StreakCheckWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}
