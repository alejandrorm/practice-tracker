package com.practicetracker.data.repository

import com.practicetracker.data.db.dao.PieceStatRow
import com.practicetracker.data.db.dao.SessionDao
import com.practicetracker.data.db.entity.SkillCheckEntity
import com.practicetracker.data.mapper.toDomain
import com.practicetracker.domain.model.PracticeSession
import com.practicetracker.domain.model.SessionEntry
import com.practicetracker.domain.model.SkillCheck
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionRepository @Inject constructor(private val sessionDao: SessionDao) {

    fun getAllSessions(): Flow<List<PracticeSession>> =
        sessionDao.getAllSessionsWithEntries().map { list -> list.map { it.toDomain() } }

    fun getSession(id: String): Flow<PracticeSession?> =
        sessionDao.getSessionWithEntries(id).map { it?.toDomain() }

    fun getSessionsInRange(start: LocalDate, end: LocalDate): Flow<List<PracticeSession>> =
        sessionDao.getSessionsInRange(start, end).map { list ->
            list.map { entity ->
                PracticeSession(entity.id, entity.planId, entity.date, entity.startTime, entity.endTime, emptyList())
            }
        }

    suspend fun getInProgressSession(): PracticeSession? {
        val entity = sessionDao.getInProgressSession() ?: return null
        return PracticeSession(entity.id, entity.planId, entity.date, entity.startTime, entity.endTime, emptyList())
    }

    suspend fun saveSession(session: PracticeSession) {
        sessionDao.insertSession(
            com.practicetracker.data.db.entity.PracticeSessionEntity(
                session.id, session.planId, session.date, session.startTime, session.endTime
            )
        )
    }

    suspend fun updateSession(session: PracticeSession) {
        sessionDao.updateSession(
            com.practicetracker.data.db.entity.PracticeSessionEntity(
                session.id, session.planId, session.date, session.startTime, session.endTime
            )
        )
    }

    suspend fun deleteSession(session: PracticeSession) {
        sessionDao.deleteSession(
            com.practicetracker.data.db.entity.PracticeSessionEntity(
                session.id, session.planId, session.date, session.startTime, session.endTime
            )
        )
    }

    suspend fun saveSessionEntry(entry: SessionEntry) {
        sessionDao.insertSessionEntry(
            com.practicetracker.data.db.entity.SessionEntryEntity(
                entry.id, entry.sessionId, entry.pieceId,
                entry.startTime, entry.endTime, entry.skipped
            )
        )
    }

    suspend fun updateSessionEntry(entry: SessionEntry) {
        sessionDao.updateSessionEntry(
            com.practicetracker.data.db.entity.SessionEntryEntity(
                entry.id, entry.sessionId, entry.pieceId,
                entry.startTime, entry.endTime, entry.skipped
            )
        )
    }

    suspend fun recordSkillCheck(check: SkillCheck, sessionEntryId: String) {
        sessionDao.insertSkillCheck(
            SkillCheckEntity(sessionEntryId, check.skillId, check.checkedAt)
        )
    }

    suspend fun removeSkillCheck(check: SkillCheck, sessionEntryId: String) {
        sessionDao.deleteSkillCheck(
            SkillCheckEntity(sessionEntryId, check.skillId, check.checkedAt)
        )
    }

    fun getTotalMinutesInRange(start: LocalDate, end: LocalDate): Flow<Int> =
        sessionDao.getTotalMinutesInRange(start, end)

    fun getPieceStatsInRange(start: LocalDate, end: LocalDate): Flow<List<PieceStatRow>> =
        sessionDao.getPieceStatsInRange(start, end)

    fun getSessionCountInRange(start: LocalDate, end: LocalDate): Flow<Int> =
        sessionDao.getSessionCountInRange(start, end)

    suspend fun calculateCurrentStreak(): Int {
        val dates = sessionDao.getAllSessionDates().toSet()
        var streak = 0
        var day = LocalDate.now()
        while (day in dates) {
            streak++
            day = day.minusDays(1)
        }
        return streak
    }

    suspend fun calculateLongestStreak(): Int {
        val dates = sessionDao.getAllSessionDates().sortedDescending()
        if (dates.isEmpty()) return 0
        var longest = 1
        var current = 1
        for (i in 1 until dates.size) {
            current = if (dates[i - 1].minusDays(1) == dates[i]) current + 1 else 1
            if (current > longest) longest = current
        }
        return longest
    }
}
