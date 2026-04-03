package com.practicetracker.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import java.time.Instant

@Entity(
    tableName = "skill_checks",
    primaryKeys = ["sessionEntryId", "skillId"],
    foreignKeys = [
        ForeignKey(entity = SessionEntryEntity::class, parentColumns = ["id"], childColumns = ["sessionEntryId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = SkillEntity::class, parentColumns = ["id"], childColumns = ["skillId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("sessionEntryId"), Index("skillId")]
)
data class SkillCheckEntity(
    val sessionEntryId: String,
    val skillId: String,
    val checkedAt: Instant
)
