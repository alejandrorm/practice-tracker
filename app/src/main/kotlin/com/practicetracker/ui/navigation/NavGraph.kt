package com.practicetracker.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.practicetracker.ui.organize.OrganizeScreen
import com.practicetracker.ui.practice.PracticeScreen
import com.practicetracker.ui.settings.SettingsScreen
import com.practicetracker.ui.stats.StatsScreen

@Composable
fun AppNavGraph(
    navController: NavHostController,
    startDestination: String = Routes.PRACTICE,
    onNavigateToOnboarding: () -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Routes.PRACTICE) { PracticeScreen() }
        composable(Routes.STATS) { StatsScreen() }
        composable(Routes.ORGANIZE) { OrganizeScreen() }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onDeletedNavigateToOnboarding = onNavigateToOnboarding
            )
        }
    }
}
