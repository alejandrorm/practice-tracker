package com.practicetracker.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.practicetracker.ui.organize.OrganizeScreen
import com.practicetracker.ui.organize.piece.PieceEditorScreen
import com.practicetracker.ui.organize.piece.PieceListScreen
import com.practicetracker.ui.organize.plan.PlanEditorScreen
import com.practicetracker.ui.organize.plan.PlanListScreen
import com.practicetracker.ui.organize.skill.SkillLibraryScreen
import com.practicetracker.ui.practice.ActiveSessionScreen
import com.practicetracker.ui.practice.PracticeScreen
import com.practicetracker.ui.practice.SessionSummaryScreen
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
        composable(Routes.PRACTICE) {
            PracticeScreen(
                onNavigateToActiveSession = { sessionId ->
                    navController.navigate(Routes.activeSession(sessionId))
                },
                onNavigateToOrganize = {
                    navController.navigate(Routes.ORGANIZE) {
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
        composable(
            route = Routes.ACTIVE_SESSION,
            arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: return@composable
            ActiveSessionScreen(
                sessionId = sessionId,
                onSessionEnded = { id ->
                    navController.navigate(Routes.sessionSummary(id)) {
                        popUpTo(Routes.PRACTICE) { inclusive = false }
                    }
                }
            )
        }
        composable(
            route = Routes.SESSION_SUMMARY,
            arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: return@composable
            SessionSummaryScreen(
                sessionId = sessionId,
                onDone = {
                    navController.navigate(Routes.PRACTICE) {
                        popUpTo(Routes.PRACTICE) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.STATS) { StatsScreen() }
        composable(Routes.ORGANIZE) {
            OrganizeScreen(
                onNavigateToPlanList = { navController.navigate(Routes.PLAN_LIST) },
                onNavigateToPieceList = { navController.navigate(Routes.PIECE_LIST) },
                onNavigateToSkillLibrary = { navController.navigate(Routes.SKILL_LIBRARY) }
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onDeletedNavigateToOnboarding = onNavigateToOnboarding
            )
        }
        composable(Routes.PIECE_LIST) {
            PieceListScreen(
                onNavigateToPieceEditor = { pieceId -> navController.navigate(Routes.pieceEditor(pieceId)) },
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Routes.PIECE_EDITOR,
            arguments = listOf(navArgument("pieceId") { type = NavType.StringType })
        ) { backStackEntry ->
            val pieceId = backStackEntry.arguments?.getString("pieceId") ?: "new"
            PieceEditorScreen(
                pieceId = pieceId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Routes.PLAN_LIST) {
            PlanListScreen(
                onNavigateToPlanEditor = { planId -> navController.navigate(Routes.planEditor(planId)) },
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Routes.PLAN_EDITOR,
            arguments = listOf(navArgument("planId") { type = NavType.StringType })
        ) { backStackEntry ->
            val planId = backStackEntry.arguments?.getString("planId") ?: "new"
            PlanEditorScreen(
                planId = planId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPieceList = { navController.navigate(Routes.PIECE_LIST) }
            )
        }
        composable(Routes.SKILL_LIBRARY) {
            SkillLibraryScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}
