package com.practicetracker.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore("settings")

data class AppSettings(
    val remindersEnabled: Boolean = false,
    val reminderScheduleInferred: Boolean = false,
    val reminderDays: Int = 0,          // bitmask Mon–Sun (same as plan schedule)
    val reminderHour: Int = 18,
    val reminderMinute: Int = 0,
    val streakRiskNotificationEnabled: Boolean = true,
    val defaultSuggestedMinutes: Int = 5,
    val theme: String = "SYSTEM"        // LIGHT / DARK / SYSTEM
)

@Singleton
class SettingsStore @Inject constructor(@ApplicationContext private val context: Context) {

    private object Keys {
        val REMINDERS_ENABLED = booleanPreferencesKey("reminders_enabled")
        val REMINDER_INFERRED = booleanPreferencesKey("reminder_inferred")
        val REMINDER_DAYS = intPreferencesKey("reminder_days")
        val REMINDER_HOUR = intPreferencesKey("reminder_hour")
        val REMINDER_MINUTE = intPreferencesKey("reminder_minute")
        val STREAK_RISK_ENABLED = booleanPreferencesKey("streak_risk_enabled")
        val DEFAULT_MINUTES = intPreferencesKey("default_minutes")
        val THEME = stringPreferencesKey("theme")
    }

    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->
        AppSettings(
            remindersEnabled = prefs[Keys.REMINDERS_ENABLED] ?: false,
            reminderScheduleInferred = prefs[Keys.REMINDER_INFERRED] ?: false,
            reminderDays = prefs[Keys.REMINDER_DAYS] ?: 0,
            reminderHour = prefs[Keys.REMINDER_HOUR] ?: 18,
            reminderMinute = prefs[Keys.REMINDER_MINUTE] ?: 0,
            streakRiskNotificationEnabled = prefs[Keys.STREAK_RISK_ENABLED] ?: true,
            defaultSuggestedMinutes = prefs[Keys.DEFAULT_MINUTES] ?: 5,
            theme = prefs[Keys.THEME] ?: "SYSTEM"
        )
    }

    suspend fun updateSettings(settings: AppSettings) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.REMINDERS_ENABLED] = settings.remindersEnabled
            prefs[Keys.REMINDER_INFERRED] = settings.reminderScheduleInferred
            prefs[Keys.REMINDER_DAYS] = settings.reminderDays
            prefs[Keys.REMINDER_HOUR] = settings.reminderHour
            prefs[Keys.REMINDER_MINUTE] = settings.reminderMinute
            prefs[Keys.STREAK_RISK_ENABLED] = settings.streakRiskNotificationEnabled
            prefs[Keys.DEFAULT_MINUTES] = settings.defaultSuggestedMinutes
            prefs[Keys.THEME] = settings.theme
        }
    }
}
