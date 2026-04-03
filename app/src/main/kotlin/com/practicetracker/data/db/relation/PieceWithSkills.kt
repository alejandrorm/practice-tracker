package com.practicetracker.data.db.relation

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.practicetracker.data.db.entity.PieceEntity
import com.practicetracker.data.db.entity.PieceSkillEntity
import com.practicetracker.data.db.entity.SkillEntity

data class PieceWithSkills(
    @Embedded val piece: PieceEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = PieceSkillEntity::class,
            parentColumn = "pieceId",
            entityColumn = "skillId"
        )
    )
    val skills: List<SkillEntity>
)
