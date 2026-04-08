package com.practicetracker.ui.achievements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.practicetracker.data.repository.Achievement
import com.practicetracker.data.repository.AchievementRepository
import com.practicetracker.domain.model.Milestone
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class BadgeUiItem(
    val info: BadgeInfo,
    val earned: Boolean,
    val achievement: Achievement? // non-null when earned
)

@HiltViewModel
class AchievementsViewModel @Inject constructor(
    achievementRepository: AchievementRepository
) : ViewModel() {

    val badges: StateFlow<List<BadgeUiItem>> =
        achievementRepository.getAllAchievements().map { earned ->
            val earnedMap = earned.associateBy { it.milestone }
            BadgeRegistry.all.map { info ->
                BadgeUiItem(
                    info = info,
                    earned = info.milestone in earnedMap,
                    achievement = earnedMap[info.milestone]
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialBadgeList())

    private fun initialBadgeList() = BadgeRegistry.all.map {
        BadgeUiItem(info = it, earned = false, achievement = null)
    }

    val earnedCount: StateFlow<Int> = badges
        .map { list -> list.count { it.earned } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
}
