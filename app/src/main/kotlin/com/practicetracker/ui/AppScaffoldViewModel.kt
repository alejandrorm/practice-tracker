package com.practicetracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.practicetracker.data.datastore.UserProfile
import com.practicetracker.data.datastore.UserProfileStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class AppScaffoldViewModel @Inject constructor(
    userProfileStore: UserProfileStore
) : ViewModel() {

    val profile: StateFlow<UserProfile> = userProfileStore.profile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserProfile())
}
