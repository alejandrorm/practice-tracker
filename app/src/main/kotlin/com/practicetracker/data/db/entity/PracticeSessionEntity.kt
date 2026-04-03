package com.practicetracker.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate

@Entity(
    tableName = "practice_sessions",
    foreignKeys = [
        ForeignKey(entity = PracticePlanEntity::class, parentColumns = ["id"], childColumns = ["planId"], onDelete = ForeignKey.SET_NULL)
    ],
    indices = [Index("planId")]
)
data class PracticeSessionEntity(
    @PrimaryKey val id: String,
    val planId: String?,
    val date: LocalDate,
    val startTime: Instant,
    val endTime: Instant?
)
