package com.practicetracker.data.db.dao

data class SkillCheckFreqRow(
    val skillId: String,
    val skillLabel: String,
    val checkCount: Int
)
