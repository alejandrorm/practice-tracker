package com.practicetracker.domain.model

import java.time.Instant
import java.time.LocalDate

data class PracticeSession(
    val id: String,
    val planId: String?,
    val date: LocalDate,
    val startTime: Instant,
    val endTime: Instant?,
    val entries: List<SessionEntry>
)

data class SessionEntry(
    val id: String,
    val sessionId: String,
    val pieceId: String?,
    val startTime: Instant?,
    val endTime: Instant?,
    val skipped: Boolean,
    val skillChecks: List<SkillCheck>
)

data class SkillCheck(
    val skillId: String,
    val checkedAt: Instant
)
