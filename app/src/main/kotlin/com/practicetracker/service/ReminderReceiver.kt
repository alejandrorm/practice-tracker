package com.practicetracker.service

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.practicetracker.MainActivity
import com.practicetracker.data.datastore.SettingsStore
import com.practicetracker.data.repository.PlanRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@AndroidEntryPoint
class ReminderReceiver : BroadcastReceiver() {

    @Inject lateinit var settingsStore: SettingsStore
    @Inject lateinit var planRepository: PlanRepository

    override fun onReceive(context: Context, intent: Intent) {
        val result = goAsync()
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            try {
                val settings = settingsStore.settings.first()
                if (!settings.remindersEnabled) return@launch

                val planName = runCatching {
                    planRepository.getPlanForDate(LocalDate.now())?.name
                }.getOrNull()

                showNotification(context, planName)

                // Reschedule for the next occurrence
                ReminderScheduler.applySettings(context, settings)
            } finally {
                result.finish()
            }
        }
    }

    private fun showNotification(context: Context, planName: String?) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val body = if (planName != null) "Your $planName is ready." else "Ready for today's practice?"
        val notification = NotificationCompat.Builder(context, NotificationChannels.PRACTICE_REMINDER)
            .setSmallIcon(android.R.drawable.ic_menu_today)
            .setContentTitle("Time to practice! 🎵")
            .setContentText(body)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    companion object {
        const val NOTIFICATION_ID = 2001
    }
}
