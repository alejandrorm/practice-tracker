package com.practicetracker.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.practicetracker.data.datastore.AppSettings
import com.practicetracker.data.datastore.SettingsStore
import com.practicetracker.data.datastore.UserProfile
import com.practicetracker.data.datastore.UserProfileStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userProfileStore: UserProfileStore,
    private val settingsStore: SettingsStore
) : ViewModel() {

    val profile: StateFlow<UserProfile> = userProfileStore.profile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserProfile())

    val settings: StateFlow<AppSettings> = settingsStore.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    private val _deleteEvent = MutableSharedFlow<Unit>()
    val deleteEvent: SharedFlow<Unit> = _deleteEvent.asSharedFlow()

    fun updateProfile(
        name: String,
        instrument: String,
        skillLevel: String,
        teacherName: String,
        avatarUri: String
    ) {
        viewModelScope.launch {
            userProfileStore.saveProfile(
                UserProfile(
                    displayName = name,
                    instrument = instrument,
                    skillLevel = skillLevel,
                    teacherName = teacherName,
                    avatarUri = avatarUri,
                    isComplete = true
                )
            )
        }
    }

    fun updateSettings(updatedSettings: AppSettings) {
        viewModelScope.launch {
            settingsStore.updateSettings(updatedSettings)
        }
    }

    fun deleteAllData() {
        viewModelScope.launch {
            userProfileStore.clearAll()
            _deleteEvent.emit(Unit)
        }
    }
}
