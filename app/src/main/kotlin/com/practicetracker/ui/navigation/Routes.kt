package com.practicetracker.ui.navigation

object Routes {
    // Bottom-nav tabs
    const val PRACTICE = "practice"
    const val STATS = "stats"
    const val ORGANIZE = "organize"

    // Full-screen destinations
    const val ONBOARDING = "onboarding"
    const val SETTINGS = "settings"

    // Organize sub-destinations
    const val PIECE_LIST    = "piece_list"
    const val PIECE_EDITOR  = "piece_editor/{pieceId}"
    const val PLAN_LIST     = "plan_list"
    const val PLAN_EDITOR   = "plan_editor/{planId}"
    const val SKILL_LIBRARY = "skill_library"

    // Practice sub-destinations
    const val ACTIVE_SESSION  = "practice/active/{sessionId}"
    const val SESSION_SUMMARY = "practice/summary/{sessionId}"

    // Stats sub-destinations
    const val HISTORY_LIST      = "stats/history"
    const val HISTORY_SESSION   = "stats/history/session/{sessionId}"
    const val PIECE_DRILL_DOWN  = "stats/piece/{pieceId}"
    const val SKILL_DRILL_DOWN  = "stats/skill/{skillId}"

    fun pieceEditor(pieceId: String = "new") = "piece_editor/$pieceId"
    fun planEditor(planId: String = "new") = "plan_editor/$planId"
    fun activeSession(sessionId: String)  = "practice/active/$sessionId"
    fun sessionSummary(sessionId: String) = "practice/summary/$sessionId"
    fun historySession(sessionId: String) = "stats/history/session/$sessionId"
    fun pieceDrillDown(pieceId: String)   = "stats/piece/$pieceId"
    fun skillDrillDown(skillId: String)   = "stats/skill/$skillId"
}
