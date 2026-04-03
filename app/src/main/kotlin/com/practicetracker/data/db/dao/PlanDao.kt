package com.practicetracker.data.db.dao

import androidx.room.*
import com.practicetracker.data.db.entity.PlanEntryEntity
import com.practicetracker.data.db.entity.PracticePlanEntity
import com.practicetracker.data.db.relation.PlanWithEntries
import kotlinx.coroutines.flow.Flow

@Dao
interface PlanDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlan(plan: PracticePlanEntity)

    @Update
    suspend fun updatePlan(plan: PracticePlanEntity)

    @Delete
    suspend fun deletePlan(plan: PracticePlanEntity)

    @Query("SELECT * FROM practice_plans ORDER BY name ASC")
    fun getAllPlans(): Flow<List<PracticePlanEntity>>

    @Transaction
    @Query("SELECT * FROM practice_plans ORDER BY name ASC")
    fun getAllPlansWithEntries(): Flow<List<PlanWithEntries>>

    @Transaction
    @Query("SELECT * FROM practice_plans WHERE id = :id")
    fun getPlanWithEntries(id: String): Flow<PlanWithEntries?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlanEntry(entry: PlanEntryEntity)

    @Update
    suspend fun updatePlanEntry(entry: PlanEntryEntity)

    @Delete
    suspend fun deletePlanEntry(entry: PlanEntryEntity)

    @Query("DELETE FROM plan_entries WHERE planId = :planId")
    suspend fun deleteAllEntriesForPlan(planId: String)

    // Returns all plans whose scheduleType is not MANUAL, for conflict detection
    @Query("SELECT * FROM practice_plans WHERE scheduleType != 'MANUAL'")
    suspend fun getScheduledPlans(): List<PracticePlanEntity>
}
