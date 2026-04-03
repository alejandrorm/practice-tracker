package com.practicetracker.di

import android.content.Context
import androidx.room.Room
import com.practicetracker.data.db.PracticeTrackerDatabase
import com.practicetracker.data.db.dao.PieceDao
import com.practicetracker.data.db.dao.PlanDao
import com.practicetracker.data.db.dao.SessionDao
import com.practicetracker.data.db.dao.SkillDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext context: Context): PracticeTrackerDatabase =
        Room.databaseBuilder(context, PracticeTrackerDatabase::class.java, "practice_tracker.db")
            .build()

    @Provides fun providePieceDao(db: PracticeTrackerDatabase): PieceDao = db.pieceDao()
    @Provides fun provideSkillDao(db: PracticeTrackerDatabase): SkillDao = db.skillDao()
    @Provides fun providePlanDao(db: PracticeTrackerDatabase): PlanDao = db.planDao()
    @Provides fun provideSessionDao(db: PracticeTrackerDatabase): SessionDao = db.sessionDao()
}
