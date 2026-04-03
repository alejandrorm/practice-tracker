package com.practicetracker.data.db.dao

import com.practicetracker.data.db.entity.PieceEntity
import com.practicetracker.data.db.entity.PieceSkillEntity
import com.practicetracker.data.db.entity.SkillEntity
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
class PieceDaoTest : BaseDaoTest() {

    private val pieceDao get() = db.pieceDao()
    private val skillDao get() = db.skillDao()

    private fun piece(id: String = "p1", title: String = "Test Piece") = PieceEntity(
        id = id, title = title, type = "SONG", composer = null,
        book = null, pages = null, notes = null, suggestedMinutes = 10, createdAt = Instant.now()
    )

    private fun skill(id: String, label: String) = SkillEntity(
        id = id, label = label, scope = "GENERAL", createdAt = Instant.now()
    )

    @Test
    fun insertPiece_appearsInGetAll() = runTest {
        pieceDao.insertPiece(piece())
        val all = pieceDao.getAllPieces().first()
        assertEquals(1, all.size)
        assertEquals("Test Piece", all[0].title)
    }

    @Test
    fun deletePiece_removesFromDatabase() = runTest {
        val p = piece()
        pieceDao.insertPiece(p)
        pieceDao.deletePiece(p)
        val all = pieceDao.getAllPieces().first()
        assertTrue(all.isEmpty())
    }

    @Test
    fun pieceWithSkills_returnsLinkedSkills() = runTest {
        val p = piece()
        val s1 = skill("s1", "Intonation")
        val s2 = skill("s2", "Bow pressure")
        pieceDao.insertPiece(p)
        skillDao.insertSkill(s1)
        skillDao.insertSkill(s2)
        pieceDao.insertPieceSkill(PieceSkillEntity(p.id, s1.id, 0))
        pieceDao.insertPieceSkill(PieceSkillEntity(p.id, s2.id, 1))

        val pws = pieceDao.getPieceWithSkills(p.id).first()
        assertNotNull(pws)
        assertEquals(2, pws!!.skills.size)
    }

    @Test
    fun deletingSkill_cascadesRemovesPieceSkillLink() = runTest {
        val p = piece()
        val s = skill("s1", "Vibrato")
        pieceDao.insertPiece(p)
        skillDao.insertSkill(s)
        pieceDao.insertPieceSkill(PieceSkillEntity(p.id, s.id, 0))
        skillDao.deleteSkill(s)

        val pws = pieceDao.getPieceWithSkills(p.id).first()
        assertTrue(pws!!.skills.isEmpty())
    }

    @Test
    fun removePieceSkill_doesNotDeleteSkill() = runTest {
        val p = piece()
        val s = skill("s1", "Rhythm")
        pieceDao.insertPiece(p)
        skillDao.insertSkill(s)
        pieceDao.insertPieceSkill(PieceSkillEntity(p.id, s.id, 0))
        pieceDao.removePieceSkill(p.id, s.id)

        val pws = pieceDao.getPieceWithSkills(p.id).first()
        assertTrue(pws!!.skills.isEmpty())
        // Skill still in library
        assertNotNull(skillDao.findByLabelIgnoreCase("Rhythm"))
    }
}
