package com.practicetracker.data.db.dao

data class PieceStatRow(
    val pieceId: String,
    val pieceName: String,
    val totalMinutes: Int,
    val sessionCount: Int
)
