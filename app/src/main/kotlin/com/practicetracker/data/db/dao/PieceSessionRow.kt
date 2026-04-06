package com.practicetracker.data.db.dao

import java.time.Instant
import java.time.LocalDate

data class PieceSessionRow(
    val sessionId: String,
    val date: LocalDate,
    val startTime: Instant?,
    val endTime: Instant?,
    val skipped: Boolean
)
