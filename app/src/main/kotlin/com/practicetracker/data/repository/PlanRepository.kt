package com.practicetracker.data.repository

import com.practicetracker.data.db.dao.PlanDao
import com.practicetracker.data.db.entity.PlanEntryEntity
import com.practicetracker.data.mapper.toDomain
import com.practicetracker.data.mapper.toEntity
import com.practicetracker.domain.model.PracticePlan
import com.practicetracker.domain.model.Schedule
import com.practicetracker.domain.model.ScheduleType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlanRepository @Inject constructor(private val planDao: PlanDao) {

    fun getAllPlans(): Flow<List<PracticePlan>> =
        planDao.getAllPlansWithEntries().map { list -> list.map { it.toDomain() } }

    fun getPlanWithEntries(id: String): Flow<PracticePlan?> =
        planDao.getPlanWithEntries(id).map { it?.toDomain() }

    suspend fun savePlan(plan: PracticePlan) {
        planDao.insertPlan(plan.toEntity())
        planDao.deleteAllEntriesForPlan(plan.id)
        plan.entries.forEach { entry ->
            planDao.insertPlanEntry(
                PlanEntryEntity(entry.id, entry.planId, entry.pieceId, entry.order, entry.overrideMinutes)
            )
        }
    }

    suspend fun deletePlan(plan: PracticePlan) {
        planDao.deletePlan(plan.toEntity())
    }

    /** Returns the plan scheduled for [date], if any. */
    suspend fun getPlanForDate(date: LocalDate): PracticePlan? {
        val scheduled = planDao.getScheduledPlans()
        return scheduled.firstOrNull { entity ->
            val type = ScheduleType.valueOf(entity.scheduleType)
            when (type) {
                ScheduleType.DAILY -> true
                ScheduleType.EVERY_OTHER_DAY -> {
                    val start = entity.scheduleStartDate ?: return@firstOrNull false
                    val diff = java.time.temporal.ChronoUnit.DAYS.between(start, date)
                    diff >= 0 && diff % 2 == 0L
                }
                ScheduleType.DAYS_OF_WEEK -> {
                    val dow = date.dayOfWeek
                    val bit = when (dow) {
                        java.time.DayOfWeek.MONDAY -> 0b0000001
                        java.time.DayOfWeek.TUESDAY -> 0b0000010
                        java.time.DayOfWeek.WEDNESDAY -> 0b0000100
                        java.time.DayOfWeek.THURSDAY -> 0b0001000
                        java.time.DayOfWeek.FRIDAY -> 0b0010000
                        java.time.DayOfWeek.SATURDAY -> 0b0100000
                        java.time.DayOfWeek.SUNDAY -> 0b1000000
                        else -> 0
                    }
                    entity.scheduleDays and bit != 0
                }
                ScheduleType.MANUAL -> false
            }
        }?.let { entity ->
            planDao.getPlanWithEntries(entity.id).map { it?.toDomain() }.let { flow ->
                // Collect one value synchronously — safe inside a suspend fun
                var result: PracticePlan? = null
                flow.collect { result = it; return@collect }
                result
            }
        }
    }

    /** Detects if [plan]'s schedule overlaps with any existing scheduled plan (excluding itself). */
    suspend fun findConflictingPlan(plan: PracticePlan): PracticePlan? {
        val allScheduled = planDao.getScheduledPlans()
            .filter { it.id != plan.id }
        // Simplified conflict: two non-MANUAL plans on the same day type conflict if both cover today
        val today = LocalDate.now()
        val conflicting = allScheduled.firstOrNull { entity ->
            val type = ScheduleType.valueOf(entity.scheduleType)
            when (type) {
                ScheduleType.DAILY -> true // always conflicts
                ScheduleType.EVERY_OTHER_DAY -> {
                    val start = entity.scheduleStartDate ?: return@firstOrNull false
                    val diff = java.time.temporal.ChronoUnit.DAYS.between(start, today)
                    diff >= 0 && diff % 2 == 0L
                }
                ScheduleType.DAYS_OF_WEEK -> {
                    val bit = 1 shl (today.dayOfWeek.value - 1)
                    entity.scheduleDays and bit != 0
                }
                ScheduleType.MANUAL -> false
            }
        }
        return conflicting?.let { entity ->
            var result: PracticePlan? = null
            planDao.getPlanWithEntries(entity.id).map { it?.toDomain() }.collect { result = it; return@collect }
            result
        }
    }
}
