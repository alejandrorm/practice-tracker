package com.practicetracker.data.db.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.practicetracker.data.db.entity.PracticeSessionEntity
import com.practicetracker.data.db.entity.SessionEntryEntity

data class SessionWithEntries(
    @Embedded val session: PracticeSessionEntity,
    @Relation(parentColumn = "id", entityColumn = "sessionId")
    val entries: List<SessionEntryEntity>
)
