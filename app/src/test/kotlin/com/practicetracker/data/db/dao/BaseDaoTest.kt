package com.practicetracker.data.db.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.practicetracker.data.db.PracticeTrackerDatabase
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.robolectric.annotation.Config

@Config(sdk = [28])
abstract class BaseDaoTest {
    protected lateinit var db: PracticeTrackerDatabase

    @Before
    fun createDb() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            PracticeTrackerDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After
    fun closeDb() = db.close()
}
