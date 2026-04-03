package com.practicetracker.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate

@Entity(tableName = "practice_plans")
data class PracticePlanEntity(
    @PrimaryKey val id: String,
    val name: String,
    val scheduleType: String,   // ScheduleType enum name
    val scheduleDays: Int,      // bitmask for DAYS_OF_WEEK (bit 0 = Mon … bit 6 = Sun)
    val scheduleStartDate: LocalDate?,  // anchor date for EVERY_OTHER_DAY
    val clonedFromId: String?,
    val createdAt: Instant
)
