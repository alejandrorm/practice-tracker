package com.practicetracker.data.db.dao

import com.practicetracker.data.db.entity.PracticeSessionEntity
import com.practicetracker.data.db.entity.SessionEntryEntity
import com.practicetracker.data.db.entity.SkillCheckEntity
import com.practicetracker.data.db.entity.SkillEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class SessionDaoTest : BaseDaoTest() {

    private val sessionDao get() = db.sessionDao()
    private val skillDao get() = db.skillDao()

    private fun session(id: String = "s1", endTime: Instant? = Instant.now()) = PracticeSessionEntity(
        id = id, planId = null, date = LocalDate.now(), startTime = Instant.now(), endTime = endTime
    )

    private fun entry(id: String = "e1", sessionId: String = "s1") = SessionEntryEntity(
        id = id, sessionId = sessionId, pieceId = null,
        startTime = Instant.now(), endTime = Instant.now(), skipped = false
    )

    @Test
    fun getInProgressSession_returnsSessionWithNullEndTime() = runTest {
        sessionDao.insertSession(session("s1", endTime = null))
        sessionDao.insertSession(session("s2", endTime = Instant.now()))
        val inProgress = sessionDao.getInProgressSession()
        assertNotNull(inProgress)
        assertEquals("s1", inProgress!!.id)
    }

    @Test
    fun getInProgressSession_returnsNull_whenNoneInProgress() = runTest {
        sessionDao.insertSession(session("s1", endTime = Instant.now()))
        assertNull(sessionDao.getInProgressSession())
    }

    @Test
    fun deleteSession_cascadesEntries() = runTest {
        val s = session()
        sessionDao.insertSession(s)
        sessionDao.insertSessionEntry(entry())
        sessionDao.deleteSession(s)
        val entries = sessionDao.getEntriesForSession("s1").first()
        assertTrue(entries.isEmpty())
    }

    @Test
    fun skillCheck_cascadesOnEntryDelete() = runTest {
        val skill = SkillEntity("sk1", "Intonation", "GENERAL", Instant.now())
        skillDao.insertSkill(skill)
        val s = session()
        sessionDao.insertSession(s)
        val e = entry()
        sessionDao.insertSessionEntry(e)
        sessionDao.insertSkillCheck(SkillCheckEntity("e1", "sk1", Instant.now()))

        sessionDao.deleteSession(s) // cascades to entries and skill_checks
        val checks = sessionDao.getSkillChecksForEntry("e1").first()
        assertTrue(checks.isEmpty())
    }

    @Test
    fun totalCheckCountForSkill_incrementsOnInsert() = runTest {
        val skill = SkillEntity("sk1", "Rhythm", "GENERAL", Instant.now())
        skillDao.insertSkill(skill)
        val s = session()
        sessionDao.insertSession(s)
        sessionDao.insertSessionEntry(entry("e1"))
        sessionDao.insertSessionEntry(entry("e2"))
        sessionDao.insertSkillCheck(SkillCheckEntity("e1", "sk1", Instant.now()))
        sessionDao.insertSkillCheck(SkillCheckEntity("e2", "sk1", Instant.now()))

        val count = sessionDao.getTotalCheckCountForSkill("sk1").first()
        assertEquals(2, count)
    }
}
