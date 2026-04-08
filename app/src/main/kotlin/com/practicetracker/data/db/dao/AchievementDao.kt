package com.practicetracker.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.practicetracker.data.db.entity.AchievementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AchievementDao {

    /** Inserts a newly earned milestone. IGNORE prevents re-insertion if already earned. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAchievement(achievement: AchievementEntity)

    @Query("SELECT * FROM achievements ORDER BY earnedAt ASC")
    fun getAllAchievements(): Flow<List<AchievementEntity>>

    @Query("SELECT milestoneId FROM achievements")
    suspend fun getEarnedMilestoneIds(): List<String>
}
