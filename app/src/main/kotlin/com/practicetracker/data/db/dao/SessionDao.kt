package com.practicetracker.data.db.dao

import androidx.room.*
import com.practicetracker.data.db.entity.PracticeSessionEntity
import com.practicetracker.data.db.entity.SessionEntryEntity
import com.practicetracker.data.db.entity.SkillCheckEntity
import com.practicetracker.data.db.relation.SessionWithEntries
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface SessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: PracticeSessionEntity)

    @Update
    suspend fun updateSession(session: PracticeSessionEntity)

    @Delete
    suspend fun deleteSession(session: PracticeSessionEntity)

    @Query("SELECT * FROM practice_sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<PracticeSessionEntity>>

    @Transaction
    @Query("SELECT * FROM practice_sessions ORDER BY startTime DESC")
    fun getAllSessionsWithEntries(): Flow<List<SessionWithEntries>>

    @Transaction
    @Query("SELECT * FROM practice_sessions WHERE id = :id")
    fun getSessionWithEntries(id: String): Flow<SessionWithEntries?>

    // In-progress session (endTime is null)
    @Query("SELECT * FROM practice_sessions WHERE endTime IS NULL LIMIT 1")
    suspend fun getInProgressSession(): PracticeSessionEntity?

    @Query("SELECT * FROM practice_sessions WHERE date = :date LIMIT 1")
    suspend fun getSessionForDate(date: LocalDate): PracticeSessionEntity?

    @Query("SELECT * FROM practice_sessions WHERE date BETWEEN :start AND :end ORDER BY date ASC")
    fun getSessionsInRange(start: LocalDate, end: LocalDate): Flow<List<PracticeSessionEntity>>

    // Aggregate queries for stats
    @Query("""
        SELECT COALESCE(SUM((strftime('%s', datetime(endTime/1000, 'unixepoch')) - strftime('%s', datetime(startTime/1000, 'unixepoch'))) / 60), 0)
        FROM practice_sessions
        WHERE date BETWEEN :start AND :end AND endTime IS NOT NULL
    """)
    fun getTotalMinutesInRange(start: LocalDate, end: LocalDate): Flow<Int>

    @Query("SELECT COUNT(*) FROM practice_sessions WHERE date BETWEEN :start AND :end AND endTime IS NOT NULL")
    fun getSessionCountInRange(start: LocalDate, end: LocalDate): Flow<Int>

    @Query("SELECT date FROM practice_sessions WHERE endTime IS NOT NULL ORDER BY date DESC")
    suspend fun getAllSessionDates(): List<LocalDate>

    // All-time aggregate stats for milestone evaluation
    @Query("SELECT COUNT(*) FROM practice_sessions WHERE endTime IS NOT NULL")
    suspend fun getTotalCompletedSessionCount(): Int

    @Query("""
        SELECT COALESCE(SUM(
            (strftime('%s', datetime(endTime/1000, 'unixepoch'))
           - strftime('%s', datetime(startTime/1000, 'unixepoch'))) / 60
        ), 0) FROM practice_sessions WHERE endTime IS NOT NULL
    """)
    suspend fun getTotalMinutesAllTime(): Int

    @Query("""
        SELECT COALESCE(MAX(pieceTotal), 0) FROM (
            SELECT COALESCE(SUM(
                CASE WHEN se.startTime IS NOT NULL AND se.endTime IS NOT NULL
                THEN (se.endTime - se.startTime) / 60000 ELSE 0 END
            ), 0) AS pieceTotal
            FROM session_entries se
            INNER JOIN practice_sessions ps ON se.sessionId = ps.id
            WHERE ps.endTime IS NOT NULL AND se.skipped = 0
            GROUP BY se.pieceId
        )
    """)
    suspend fun getMaxMinutesOnSinglePiece(): Int

    // Session entries
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessionEntry(entry: SessionEntryEntity)

    @Update
    suspend fun updateSessionEntry(entry: SessionEntryEntity)

    @Query("SELECT * FROM session_entries WHERE sessionId = :sessionId ORDER BY rowid ASC")
    fun getEntriesForSession(sessionId: String): Flow<List<SessionEntryEntity>>

    @Query("SELECT * FROM session_entries WHERE pieceId = :pieceId ORDER BY startTime DESC")
    fun getEntriesForPiece(pieceId: String): Flow<List<SessionEntryEntity>>

    // Skill checks
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSkillCheck(check: SkillCheckEntity)

    @Delete
    suspend fun deleteSkillCheck(check: SkillCheckEntity)

    @Query("SELECT * FROM skill_checks WHERE sessionEntryId = :entryId")
    fun getSkillChecksForEntry(entryId: String): Flow<List<SkillCheckEntity>>

    @Query("SELECT COUNT(*) FROM skill_checks WHERE skillId = :skillId")
    fun getTotalCheckCountForSkill(skillId: String): Flow<Int>

    // Drill-down: skill check frequency for a single piece (all time)
    @Query("""
        SELECT sc.skillId, s.label AS skillLabel, COUNT(*) AS checkCount
        FROM skill_checks sc
        INNER JOIN session_entries se ON sc.sessionEntryId = se.id
        INNER JOIN skills s ON sc.skillId = s.id
        WHERE se.pieceId = :pieceId
        GROUP BY sc.skillId
        ORDER BY checkCount DESC
    """)
    fun getSkillFrequencyForPiece(pieceId: String): Flow<List<SkillCheckFreqRow>>

    // Drill-down: session rows for a single piece (most recent first)
    @Query("""
        SELECT ps.id AS sessionId, ps.date, se.startTime, se.endTime, se.skipped
        FROM session_entries se
        INNER JOIN practice_sessions ps ON se.sessionId = ps.id
        WHERE se.pieceId = :pieceId
          AND ps.endTime IS NOT NULL
        ORDER BY ps.startTime DESC
    """)
    fun getSessionRowsForPiece(pieceId: String): Flow<List<PieceSessionRow>>

    // Drill-down: sessions where a given skill was checked (most recent first)
    @Query("""
        SELECT ps.id AS sessionId, ps.date,
            COALESCE(p.title, '[Deleted]') AS pieceName,
            se.startTime, se.endTime
        FROM skill_checks sc
        INNER JOIN session_entries se ON sc.sessionEntryId = se.id
        INNER JOIN practice_sessions ps ON se.sessionId = ps.id
        LEFT JOIN pieces p ON se.pieceId = p.id
        WHERE sc.skillId = :skillId
          AND ps.endTime IS NOT NULL
        ORDER BY ps.startTime DESC
    """)
    fun getSessionRowsForSkill(skillId: String): Flow<List<SkillSessionRow>>

    // Per-piece stats for the stats dashboard
    @Query("""
        SELECT p.id AS pieceId, p.title AS pieceName,
            COALESCE(SUM(
                CASE WHEN se.startTime IS NOT NULL AND se.endTime IS NOT NULL
                THEN (se.endTime - se.startTime) / 60000
                ELSE 0 END
            ), 0) AS totalMinutes,
            COUNT(DISTINCT s.id) AS sessionCount
        FROM session_entries se
        INNER JOIN practice_sessions s ON se.sessionId = s.id
        INNER JOIN pieces p ON se.pieceId = p.id
        WHERE s.date BETWEEN :start AND :end
          AND s.endTime IS NOT NULL
          AND se.skipped = 0
        GROUP BY se.pieceId
        ORDER BY totalMinutes DESC
    """)
    fun getPieceStatsInRange(start: LocalDate, end: LocalDate): Flow<List<PieceStatRow>>
}
