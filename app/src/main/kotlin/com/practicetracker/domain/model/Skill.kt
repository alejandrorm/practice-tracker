package com.practicetracker.domain.model

import java.time.Instant

data class Skill(
    val id: String,
    val label: String,
    val scope: SkillScope,
    val createdAt: Instant
)
