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

    fun pieceEditor(pieceId: String = "new") = "piece_editor/$pieceId"
    fun planEditor(planId: String = "new") = "plan_editor/$planId"
}
