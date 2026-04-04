package com.practicetracker.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.practicetracker.data.datastore.UserProfile
import com.practicetracker.data.datastore.UserProfileStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val userProfileStore: UserProfileStore
) : ViewModel() {

    val profile: StateFlow<UserProfile> = userProfileStore.profile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserProfile())

    private val _currentPage = MutableStateFlow(0)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()

    val instrumentSuggestions = listOf(
        "Violin", "Piano", "Cello", "Guitar", "Flute",
        "Viola", "Clarinet", "Trumpet", "Drums", "Voice", "Bass", "Saxophone"
    )

    fun nextPage() {
        if (_currentPage.value < 2) _currentPage.value++
    }

    fun prevPage() {
        if (_currentPage.value > 0) _currentPage.value--
    }

    fun saveProfile(
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
}
