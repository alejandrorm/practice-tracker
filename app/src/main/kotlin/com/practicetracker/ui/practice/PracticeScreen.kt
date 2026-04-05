package com.practicetracker.ui.practice

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun PracticeScreen(
    onNavigateToActiveSession: (String) -> Unit,
    onNavigateToOrganize: () -> Unit
) {
    SessionHomeScreen(
        onNavigateToActiveSession = onNavigateToActiveSession,
        onNavigateToOrganize = onNavigateToOrganize
    )
}
