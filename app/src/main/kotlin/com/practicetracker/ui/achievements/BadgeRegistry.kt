package com.practicetracker.ui.achievements

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.HourglassFull
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.practicetracker.domain.model.Milestone

data class BadgeInfo(
    val milestone: Milestone,
    val icon: ImageVector,
    val color: Color,
    val emoji: String   // Used in share card text rendering
)

object BadgeRegistry {

    val all: List<BadgeInfo> = listOf(
        BadgeInfo(Milestone.FIRST_SESSION,   Icons.Filled.Star,               Color(0xFFFFD700), "⭐"),
        BadgeInfo(Milestone.STREAK_3,        Icons.Filled.LocalFireDepartment, Color(0xFFFF6B35), "🔥"),
        BadgeInfo(Milestone.STREAK_7,        Icons.Filled.Whatshot,           Color(0xFFFF4500), "🔥"),
        BadgeInfo(Milestone.STREAK_30,       Icons.Filled.EmojiEvents,        Color(0xFFFFAA00), "🏆"),
        BadgeInfo(Milestone.STREAK_RECORD,   Icons.Filled.TrendingUp,         Color(0xFF00C853), "📈"),
        BadgeInfo(Milestone.HOURS_1,         Icons.Filled.Timer,              Color(0xFF42A5F5), "⏱"),
        BadgeInfo(Milestone.HOURS_10,        Icons.Filled.HourglassFull,      Color(0xFF7E57C2), "⌛"),
        BadgeInfo(Milestone.HOURS_50,        Icons.Filled.Diamond,            Color(0xFF26C6DA), "💎"),
        BadgeInfo(Milestone.HOURS_100,       Icons.Filled.Diamond,            Color(0xFF7B1FA2), "💎"),
        BadgeInfo(Milestone.HOURS_500,       Icons.Filled.WorkspacePremium,   Color(0xFFE91E63), "👑"),
        BadgeInfo(Milestone.PIECE_HOURS_1,   Icons.Filled.MusicNote,          Color(0xFF66BB6A), "🎵"),
        BadgeInfo(Milestone.PIECE_HOURS_10,  Icons.Filled.LibraryMusic,       Color(0xFF2E7D32), "🎼"),
        BadgeInfo(Milestone.PERFECT_SESSION, Icons.Filled.CheckCircle,        Color(0xFF4CAF50), "✅")
    )

    private val byMilestone: Map<Milestone, BadgeInfo> = all.associateBy { it.milestone }

    fun get(milestone: Milestone): BadgeInfo = requireNotNull(byMilestone[milestone]) {
        "No BadgeInfo registered for $milestone"
    }
}
