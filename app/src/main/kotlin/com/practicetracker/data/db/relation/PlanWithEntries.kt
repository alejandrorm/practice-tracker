package com.practicetracker.data.db.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.practicetracker.data.db.entity.PlanEntryEntity
import com.practicetracker.data.db.entity.PracticePlanEntity

data class PlanWithEntries(
    @Embedded val plan: PracticePlanEntity,
    @Relation(parentColumn = "id", entityColumn = "planId")
    val entries: List<PlanEntryEntity>
)
