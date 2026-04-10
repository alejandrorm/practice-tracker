package com.practicetracker.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.practicetracker.data.db.converter.Converters
import com.practicetracker.data.db.dao.AchievementDao
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
        SkillCheckEntity::class,
        AchievementEntity::class
    ],
    version = 3,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class PracticeTrackerDatabase : RoomDatabase() {
    abstract fun pieceDao(): PieceDao
    abstract fun skillDao(): SkillDao
    abstract fun planDao(): PlanDao
    abstract fun sessionDao(): SessionDao
    abstract fun achievementDao(): AchievementDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS achievements (
                        milestoneId TEXT NOT NULL PRIMARY KEY,
                        earnedAt    INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE pieces ADD COLUMN level INTEGER")
                db.execSQL("ALTER TABLE pieces ADD COLUMN levelAlias TEXT")
            }
        }
    }
}
