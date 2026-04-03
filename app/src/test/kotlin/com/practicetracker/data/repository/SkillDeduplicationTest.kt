package com.practicetracker.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.practicetracker.data.db.PracticeTrackerDatabase
import com.practicetracker.domain.model.SkillScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class SkillDeduplicationTest {

    private lateinit var db: PracticeTrackerDatabase
    private lateinit var repo: PieceRepository

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            PracticeTrackerDatabase::class.java
        ).allowMainThreadQueries().build()
        repo = PieceRepository(db.pieceDao(), db.skillDao())
    }

    @After
    fun teardown() = db.close()

    @Test
    fun getOrCreateSkill_deduplicatesCaseInsensitive() = runTest {
        val s1 = repo.getOrCreateSkill("Bow Pressure", SkillScope.GENERAL)
        val s2 = repo.getOrCreateSkill("bow pressure", SkillScope.GENERAL)
        assertEquals(s1.id, s2.id)
    }

    @Test
    fun getOrCreateSkill_createsNewWhenNoMatch() = runTest {
        val s1 = repo.getOrCreateSkill("Intonation", SkillScope.GENERAL)
        val s2 = repo.getOrCreateSkill("Bow Pressure", SkillScope.GENERAL)
        assertNotEquals(s1.id, s2.id)
    }
}
