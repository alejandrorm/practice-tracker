package com.practicetracker.data.mapper

import com.practicetracker.data.db.entity.*
import com.practicetracker.data.db.relation.PieceWithSkills
import com.practicetracker.data.db.relation.PlanWithEntries
import com.practicetracker.data.db.relation.SessionWithEntries
import com.practicetracker.domain.model.*
import java.time.DayOfWeek
import java.time.LocalDate

fun PieceWithSkills.toDomain(orderedSkillIds: List<String> = emptyList()): Piece {
    val orderedSkills = if (orderedSkillIds.isNotEmpty()) {
        val skillMap = skills.associateBy { it.id }
        orderedSkillIds.mapNotNull { skillMap[it] }.map { it.toDomain() }
    } else {
        skills.map { it.toDomain() }
    }
    return Piece(
        id = piece.id,
        title = piece.title,
        type = PieceType.valueOf(piece.type),
        composer = piece.composer,
        book = piece.book,
        pages = piece.pages,
        notes = piece.notes,
        suggestedMinutes = piece.suggestedMinutes,
        skills = orderedSkills,
        createdAt = piece.createdAt,
        level = piece.level,
        levelAlias = piece.levelAlias
    )
}

fun PieceEntity.toDomain(skills: List<Skill> = emptyList()) = Piece(
    id = id,
    title = title,
    type = PieceType.valueOf(type),
    composer = composer,
    book = book,
    pages = pages,
    notes = notes,
    suggestedMinutes = suggestedMinutes,
    skills = skills,
    createdAt = createdAt,
    level = level,
    levelAlias = levelAlias
)

fun SkillEntity.toDomain() = Skill(
    id = id,
    label = label,
    scope = SkillScope.valueOf(scope),
    createdAt = createdAt
)

fun Skill.toEntity() = SkillEntity(
    id = id,
    label = label,
    scope = scope.name,
    createdAt = createdAt
)

fun Piece.toEntity() = PieceEntity(
    id = id,
    title = title,
    type = type.name,
    composer = composer,
    book = book,
    pages = pages,
    notes = notes,
    suggestedMinutes = suggestedMinutes,
    createdAt = createdAt,
    level = level,
    levelAlias = levelAlias
)

fun PlanWithEntries.toDomain(): PracticePlan {
    val scheduleType = ScheduleType.valueOf(plan.scheduleType)
    val schedule: Schedule = when (scheduleType) {
        ScheduleType.DAILY -> Schedule.Daily
        ScheduleType.EVERY_OTHER_DAY -> Schedule.EveryOtherDay(plan.scheduleStartDate ?: LocalDate.now())
        ScheduleType.DAYS_OF_WEEK -> {
            val days = mutableSetOf<DayOfWeek>()
            val mask = plan.scheduleDays
            if (mask and 0b0000001 != 0) days += DayOfWeek.MONDAY
            if (mask and 0b0000010 != 0) days += DayOfWeek.TUESDAY
            if (mask and 0b0000100 != 0) days += DayOfWeek.WEDNESDAY
            if (mask and 0b0001000 != 0) days += DayOfWeek.THURSDAY
            if (mask and 0b0010000 != 0) days += DayOfWeek.FRIDAY
            if (mask and 0b0100000 != 0) days += DayOfWeek.SATURDAY
            if (mask and 0b1000000 != 0) days += DayOfWeek.SUNDAY
            Schedule.DaysOfWeek(days)
        }
        ScheduleType.MANUAL -> Schedule.Manual
    }
    return PracticePlan(
        id = plan.id,
        name = plan.name,
        entries = entries.sortedBy { it.order }.map { it.toDomain() },
        schedule = schedule,
        clonedFromId = plan.clonedFromId,
        createdAt = plan.createdAt
    )
}

fun PlanEntryEntity.toDomain() = PlanEntry(
    id = id, planId = planId, pieceId = pieceId, order = order, overrideMinutes = overrideMinutes
)

fun PracticePlan.toEntity(): PracticePlanEntity {
    val (type, days, startDate) = when (val s = schedule) {
        is Schedule.Daily -> Triple(ScheduleType.DAILY, 0, null)
        is Schedule.EveryOtherDay -> Triple(ScheduleType.EVERY_OTHER_DAY, 0, s.startDate)
        is Schedule.DaysOfWeek -> {
            var mask = 0
            if (DayOfWeek.MONDAY in s.days) mask = mask or 0b0000001
            if (DayOfWeek.TUESDAY in s.days) mask = mask or 0b0000010
            if (DayOfWeek.WEDNESDAY in s.days) mask = mask or 0b0000100
            if (DayOfWeek.THURSDAY in s.days) mask = mask or 0b0001000
            if (DayOfWeek.FRIDAY in s.days) mask = mask or 0b0010000
            if (DayOfWeek.SATURDAY in s.days) mask = mask or 0b0100000
            if (DayOfWeek.SUNDAY in s.days) mask = mask or 0b1000000
            Triple(ScheduleType.DAYS_OF_WEEK, mask, null)
        }
        is Schedule.Manual -> Triple(ScheduleType.MANUAL, 0, null)
    }
    return PracticePlanEntity(
        id = id, name = name,
        scheduleType = type.name, scheduleDays = days, scheduleStartDate = startDate,
        clonedFromId = clonedFromId, createdAt = createdAt
    )
}

fun SessionWithEntries.toDomain(): PracticeSession = PracticeSession(
    id = session.id, planId = session.planId, date = session.date,
    startTime = session.startTime, endTime = session.endTime,
    entries = entries.map { it.toDomain() }
)

fun SessionEntryEntity.toDomain() = SessionEntry(
    id = id, sessionId = sessionId, pieceId = pieceId,
    startTime = startTime, endTime = endTime, skipped = skipped,
    skillChecks = emptyList() // populated separately when needed
)

fun SkillCheckEntity.toDomain() = SkillCheck(skillId = skillId, checkedAt = checkedAt)
