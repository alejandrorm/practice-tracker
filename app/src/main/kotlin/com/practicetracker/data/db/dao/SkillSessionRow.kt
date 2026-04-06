package com.practicetracker.data.db.dao

import java.time.Instant
import java.time.LocalDate

data class SkillSessionRow(
    val sessionId: String,
    val date: LocalDate,
    val pieceName: String,
    val startTime: Instant?,
    val endTime: Instant?
)
