package com.practicetracker.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.practicetracker.data.datastore.SettingsStore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Reschedules the practice reminder alarm after device reboot.
 * AlarmManager alarms are cleared when the device powers off.
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var settingsStore: SettingsStore

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val result = goAsync()
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            try {
                val settings = settingsStore.settings.first()
                ReminderScheduler.applySettings(context, settings)
            } finally {
                result.finish()
            }
        }
    }
}
