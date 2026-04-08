package com.practicetracker.data.repository

import com.practicetracker.data.db.dao.AchievementDao
import com.practicetracker.data.db.entity.AchievementEntity
import com.practicetracker.domain.model.Milestone
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

data class Achievement(val milestone: Milestone, val earnedAt: Instant)

@Singleton
class AchievementRepository @Inject constructor(private val achievementDao: AchievementDao) {

    fun getAllAchievements(): Flow<List<Achievement>> =
        achievementDao.getAllAchievements().map { list ->
            list.mapNotNull { entity ->
                val milestone = runCatching { Milestone.valueOf(entity.milestoneId) }.getOrNull()
                    ?: return@mapNotNull null
                Achievement(milestone, entity.earnedAt)
            }
        }

    suspend fun getEarnedMilestoneIds(): Set<String> =
        achievementDao.getEarnedMilestoneIds().toSet()

    suspend fun insertAchievement(milestone: Milestone, earnedAt: Instant) {
        achievementDao.insertAchievement(
            AchievementEntity(milestoneId = milestone.name, earnedAt = earnedAt)
        )
    }
}
