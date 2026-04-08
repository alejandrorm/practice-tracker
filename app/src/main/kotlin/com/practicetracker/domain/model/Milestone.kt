package com.practicetracker.domain.model

enum class Milestone(val displayName: String, val unlockHint: String) {
    FIRST_SESSION(
        displayName = "First Session",
        unlockHint = "Complete your very first practice session"
    ),
    STREAK_3(
        displayName = "3-Day Streak",
        unlockHint = "Practice 3 days in a row"
    ),
    STREAK_7(
        displayName = "7-Day Streak",
        unlockHint = "Practice 7 days in a row"
    ),
    STREAK_30(
        displayName = "30-Day Streak",
        unlockHint = "Practice 30 days in a row"
    ),
    STREAK_RECORD(
        displayName = "Personal Best Streak",
        unlockHint = "Set a new personal streak record"
    ),
    HOURS_1(
        displayName = "1 Hour Total",
        unlockHint = "Accumulate 1 hour of practice time"
    ),
    HOURS_10(
        displayName = "10 Hours Total",
        unlockHint = "Accumulate 10 hours of practice time"
    ),
    HOURS_50(
        displayName = "50 Hours Total",
        unlockHint = "Accumulate 50 hours of practice time"
    ),
    HOURS_100(
        displayName = "100 Hours Total",
        unlockHint = "Accumulate 100 hours of practice time"
    ),
    HOURS_500(
        displayName = "500 Hours Total",
        unlockHint = "Accumulate 500 hours of practice time"
    ),
    PIECE_HOURS_1(
        displayName = "1 Hour on a Piece",
        unlockHint = "Practice a single piece for 1 cumulative hour"
    ),
    PIECE_HOURS_10(
        displayName = "10 Hours on a Piece",
        unlockHint = "Practice a single piece for 10 cumulative hours"
    ),
    PERFECT_SESSION(
        displayName = "Perfect Session",
        unlockHint = "Complete a session without skipping any piece"
    )
}
