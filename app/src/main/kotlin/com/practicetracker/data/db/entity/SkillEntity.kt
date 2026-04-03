package com.practicetracker.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "skills")
data class SkillEntity(
    @PrimaryKey val id: String,
    val label: String,
    val scope: String,          // SkillScope enum name
    val createdAt: Instant
)
