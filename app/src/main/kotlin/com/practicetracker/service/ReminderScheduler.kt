package com.practicetracker.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.practicetracker.data.datastore.AppSettings
import java.time.ZonedDateTime

object ReminderScheduler {

    private const val REQUEST_CODE = 3001

    /**
     * Schedules or cancels the reminder alarm based on [settings].
     * Call this whenever reminder-related settings change and on boot.
     */
    fun applySettings(context: Context, settings: AppSettings) {
        if (!settings.remindersEnabled || settings.reminderDays == 0) {
            cancel(context)
        } else {
            schedule(context, settings)
        }
    }

    private fun schedule(context: Context, settings: AppSettings) {
        val triggerAt = nextTriggerMillis(settings) ?: return
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAt,
            buildPendingIntent(context)
        )
    }

    fun cancel(context: Context) {
        context.getSystemService(AlarmManager::class.java)
            .cancel(buildPendingIntent(context))
    }

    private fun buildPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java)
        return PendingIntent.getBroadcast(
            context, REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * Returns the epoch-millis timestamp of the next matching day/time, or null if
     * [settings.reminderDays] has no bits set.
     */
    fun nextTriggerMillis(settings: AppSettings): Long? {
        if (settings.reminderDays == 0) return null

        var candidate = ZonedDateTime.now()
            .withHour(settings.reminderHour)
            .withMinute(settings.reminderMinute)
            .withSecond(0)
            .withNano(0)

        // If the time has already passed today, start searching from tomorrow
        if (!candidate.isAfter(ZonedDateTime.now())) {
            candidate = candidate.plusDays(1)
        }

        // Advance up to 7 days to find the next enabled day-of-week
        repeat(7) {
            val bit = 1 shl (candidate.dayOfWeek.value - 1) // Mon = bit 0
            if (settings.reminderDays and bit != 0) return candidate.toInstant().toEpochMilli()
            candidate = candidate.plusDays(1)
        }
        return null
    }
}
