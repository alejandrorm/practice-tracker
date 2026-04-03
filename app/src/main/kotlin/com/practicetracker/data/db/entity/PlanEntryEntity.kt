package com.practicetracker.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "plan_entries",
    foreignKeys = [
        ForeignKey(entity = PracticePlanEntity::class, parentColumns = ["id"], childColumns = ["planId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = PieceEntity::class, parentColumns = ["id"], childColumns = ["pieceId"], onDelete = ForeignKey.SET_NULL)
    ],
    indices = [Index("planId"), Index("pieceId")]
)
data class PlanEntryEntity(
    @PrimaryKey val id: String,
    val planId: String,
    val pieceId: String?,       // nullable: piece may be deleted
    val order: Int,
    val overrideMinutes: Int?
)
