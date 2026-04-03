package com.practicetracker.data.datastore

import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.userProfileDataStore: DataStore<Preferences> by preferencesDataStore("user_profile")

data class UserProfile(
    val displayName: String = "",
    val instrument: String = "",
    val skillLevel: String = "",
    val teacherName: String = "",
    val avatarUri: String = "",
    val isComplete: Boolean = false
)

@Singleton
class UserProfileStore @Inject constructor(@ApplicationContext private val context: Context) {

    private object Keys {
        val DISPLAY_NAME = stringPreferencesKey("display_name")
        val INSTRUMENT = stringPreferencesKey("instrument")
        val SKILL_LEVEL = stringPreferencesKey("skill_level")
        val TEACHER_NAME = stringPreferencesKey("teacher_name")
        val AVATAR_URI = stringPreferencesKey("avatar_uri")
        val IS_COMPLETE = booleanPreferencesKey("is_complete")
    }

    val profile: Flow<UserProfile> = context.userProfileDataStore.data.map { prefs ->
        UserProfile(
            displayName = prefs[Keys.DISPLAY_NAME] ?: "",
            instrument = prefs[Keys.INSTRUMENT] ?: "",
            skillLevel = prefs[Keys.SKILL_LEVEL] ?: "",
            teacherName = prefs[Keys.TEACHER_NAME] ?: "",
            avatarUri = prefs[Keys.AVATAR_URI] ?: "",
            isComplete = prefs[Keys.IS_COMPLETE] ?: false
        )
    }

    suspend fun saveProfile(profile: UserProfile) {
        context.userProfileDataStore.edit { prefs ->
            prefs[Keys.DISPLAY_NAME] = profile.displayName
            prefs[Keys.INSTRUMENT] = profile.instrument
            prefs[Keys.SKILL_LEVEL] = profile.skillLevel
            prefs[Keys.TEACHER_NAME] = profile.teacherName
            prefs[Keys.AVATAR_URI] = profile.avatarUri
            prefs[Keys.IS_COMPLETE] = profile.isComplete
        }
    }

    suspend fun clearAll() {
        context.userProfileDataStore.edit { it.clear() }
    }
}
