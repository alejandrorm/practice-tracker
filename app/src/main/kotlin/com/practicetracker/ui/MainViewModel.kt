package com.practicetracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.practicetracker.data.datastore.UserProfileStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    userProfileStore: UserProfileStore
) : ViewModel() {

    val isProfileComplete: StateFlow<Boolean> = userProfileStore.profile
        .map { it.isComplete }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
}
