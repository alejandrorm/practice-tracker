package com.practicetracker

import android.app.Application
import com.practicetracker.service.SessionNotificationHelper
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class PracticeTrackerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        SessionNotificationHelper.createChannel(this)
    }
}
