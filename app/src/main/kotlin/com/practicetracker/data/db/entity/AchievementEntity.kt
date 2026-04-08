package com.practicetracker.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "achievements")
data class AchievementEntity(
    /** The Milestone enum name — acts as a natural deduplicated primary key. */
    @PrimaryKey val milestoneId: String,
    val earnedAt: Instant
)
