package com.practicetracker.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.practicetracker.data.db.converter.Converters
import com.practicetracker.data.db.dao.PieceDao
import com.practicetracker.data.db.dao.PlanDao
import com.practicetracker.data.db.dao.SessionDao
import com.practicetracker.data.db.dao.SkillDao
import com.practicetracker.data.db.entity.*

@Database(
    entities = [
        PieceEntity::class,
        SkillEntity::class,
        PieceSkillEntity::class,
        PracticePlanEntity::class,
        PlanEntryEntity::class,
        PracticeSessionEntity::class,
        SessionEntryEntity::class,
        SkillCheckEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class PracticeTrackerDatabase : RoomDatabase() {
    abstract fun pieceDao(): PieceDao
    abstract fun skillDao(): SkillDao
    abstract fun planDao(): PlanDao
    abstract fun sessionDao(): SessionDao
}
