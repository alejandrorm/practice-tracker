package com.practicetracker.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(
    tableName = "session_entries",
    foreignKeys = [
        ForeignKey(entity = PracticeSessionEntity::class, parentColumns = ["id"], childColumns = ["sessionId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = PieceEntity::class, parentColumns = ["id"], childColumns = ["pieceId"], onDelete = ForeignKey.SET_NULL)
    ],
    indices = [Index("sessionId"), Index("pieceId")]
)
data class SessionEntryEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val pieceId: String?,
    val startTime: Instant?,
    val endTime: Instant?,
    val skipped: Boolean
)
