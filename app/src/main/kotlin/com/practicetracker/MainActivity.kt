package com.practicetracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.practicetracker.ui.AppScaffold
import com.practicetracker.ui.MainViewModel
import com.practicetracker.ui.onboarding.OnboardingScreen
import com.practicetracker.ui.theme.PracticeTrackerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PracticeTrackerTheme {
                val isProfileComplete by viewModel.isProfileComplete.collectAsStateWithLifecycle()

                if (isProfileComplete) {
                    AppScaffold(
                        onNavigateToOnboarding = {
                            // Profile was deleted — recreate so the StateFlow
                            // re-emits false and onboarding is shown again.
                            recreate()
                        }
                    )
                } else {
                    OnboardingScreen(onComplete = {})
                }
            }
        }
    }
}
