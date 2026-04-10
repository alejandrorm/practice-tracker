package com.practicetracker.domain.model

import java.time.Instant

data class Piece(
    val id: String,
    val title: String,
    val type: PieceType,
    val composer: String?,
    val book: String?,
    val pages: String?,
    val notes: String?,
    val suggestedMinutes: Int,
    val skills: List<Skill>,
    val createdAt: Instant,
    val level: Int? = null,
    val levelAlias: String? = null
)
