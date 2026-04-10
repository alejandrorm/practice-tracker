package com.practicetracker.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "pieces")
data class PieceEntity(
    @PrimaryKey val id: String,
    val title: String,
    val type: String,           // PieceType enum name
    val composer: String?,
    val book: String?,
    val pages: String?,
    val notes: String?,
    val suggestedMinutes: Int,
    val createdAt: Instant,
    val level: Int? = null,
    val levelAlias: String? = null
)
