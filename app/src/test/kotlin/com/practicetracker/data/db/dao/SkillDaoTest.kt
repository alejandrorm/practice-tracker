package com.practicetracker.data.db.dao

import com.practicetracker.data.db.entity.SkillEntity
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class SkillDaoTest : BaseDaoTest() {

    private val dao get() = db.skillDao()

    private fun skill(label: String) = SkillEntity(
        id = label, label = label, scope = "GENERAL", createdAt = Instant.now()
    )

    @Test
    fun insertAndFindByLabel_caseSensitiveMatch() = runTest {
        dao.insertSkill(skill("Bow pressure"))
        val found = dao.findByLabelIgnoreCase("bow pressure")
        assertNotNull(found)
        assertEquals("Bow pressure", found!!.label)
    }

    @Test
    fun findByLabel_returnsNull_whenNotFound() = runTest {
        val found = dao.findByLabelIgnoreCase("nonexistent")
        assertNull(found)
    }

    @Test
    fun deleteSkill_removesFromDatabase() = runTest {
        val s = skill("Vibrato")
        dao.insertSkill(s)
        dao.deleteSkill(s)
        assertNull(dao.findByLabelIgnoreCase("Vibrato"))
    }

    @Test
    fun updateSkill_changesLabel() = runTest {
        val s = skill("OldLabel")
        dao.insertSkill(s)
        dao.updateSkill(s.copy(label = "NewLabel"))
        assertNull(dao.findByLabelIgnoreCase("OldLabel"))
        assertNotNull(dao.findByLabelIgnoreCase("NewLabel"))
    }
}
