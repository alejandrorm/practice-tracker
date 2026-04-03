package com.practicetracker.domain.model

import java.time.Instant
import java.time.LocalDate

data class PracticePlan(
    val id: String,
    val name: String,
    val entries: List<PlanEntry>,
    val schedule: Schedule,
    val clonedFromId: String?,
    val createdAt: Instant
)

data class PlanEntry(
    val id: String,
    val planId: String,
    val pieceId: String?,
    val order: Int,
    val overrideMinutes: Int?
)

sealed class Schedule {
    object Daily : Schedule()
    data class EveryOtherDay(val startDate: LocalDate) : Schedule()
    data class DaysOfWeek(val days: Set<java.time.DayOfWeek>) : Schedule()
    object Manual : Schedule()
}
