package com.practicetracker.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "piece_skills",
    primaryKeys = ["pieceId", "skillId"],
    foreignKeys = [
        ForeignKey(entity = PieceEntity::class, parentColumns = ["id"], childColumns = ["pieceId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = SkillEntity::class, parentColumns = ["id"], childColumns = ["skillId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("pieceId"), Index("skillId")]
)
data class PieceSkillEntity(
    val pieceId: String,
    val skillId: String,
    val order: Int
)
