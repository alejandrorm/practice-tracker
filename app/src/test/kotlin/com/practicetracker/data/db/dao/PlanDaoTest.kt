package com.practicetracker.data.db.dao

import com.practicetracker.data.db.entity.PieceEntity
import com.practicetracker.data.db.entity.PlanEntryEntity
import com.practicetracker.data.db.entity.PracticePlanEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class PlanDaoTest : BaseDaoTest() {

    private val planDao get() = db.planDao()
    private val pieceDao get() = db.pieceDao()

    private fun plan(id: String = "plan1") = PracticePlanEntity(
        id = id, name = "My Plan", scheduleType = "DAILY", scheduleDays = 0,
        scheduleStartDate = null, clonedFromId = null, createdAt = Instant.now()
    )

    private fun piece(id: String = "p1") = PieceEntity(
        id = id, title = "Piece", type = "SONG", composer = null,
        book = null, pages = null, notes = null, suggestedMinutes = 5, createdAt = Instant.now()
    )

    @Test
    fun insertPlanWithEntries_retrievedWithEntries() = runTest {
        val pl = plan()
        val pc = piece()
        pieceDao.insertPiece(pc)
        planDao.insertPlan(pl)
        planDao.insertPlanEntry(PlanEntryEntity("e1", pl.id, pc.id, 0, null))

        val result = planDao.getPlanWithEntries(pl.id).first()
        assertNotNull(result)
        assertEquals(1, result!!.entries.size)
        assertEquals(pc.id, result.entries[0].pieceId)
    }

    @Test
    fun deletePlan_cascadesEntries() = runTest {
        val pl = plan()
        val pc = piece()
        pieceDao.insertPiece(pc)
        planDao.insertPlan(pl)
        planDao.insertPlanEntry(PlanEntryEntity("e1", pl.id, pc.id, 0, null))
        planDao.deletePlan(pl)

        val result = planDao.getAllPlans().first()
        assertTrue(result.isEmpty())
    }

    @Test
    fun deleteAllEntriesForPlan_leavesOtherPlansIntact() = runTest {
        val pl1 = plan("plan1")
        val pl2 = plan("plan2")
        val pc = piece()
        pieceDao.insertPiece(pc)
        planDao.insertPlan(pl1)
        planDao.insertPlan(pl2)
        planDao.insertPlanEntry(PlanEntryEntity("e1", pl1.id, pc.id, 0, null))
        planDao.insertPlanEntry(PlanEntryEntity("e2", pl2.id, pc.id, 0, null))

        planDao.deleteAllEntriesForPlan(pl1.id)

        val pl1Result = planDao.getPlanWithEntries(pl1.id).first()
        assertTrue(pl1Result!!.entries.isEmpty())
        val pl2Result = planDao.getPlanWithEntries(pl2.id).first()
        assertEquals(1, pl2Result!!.entries.size)
    }
}
