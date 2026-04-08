package com.practicetracker.domain.engine

import com.practicetracker.domain.model.Milestone

/**
 * Input snapshot for evaluating which milestones currently qualify.
 * All values are all-time totals computed *after* the just-completed session.
 */
data class MilestoneInput(
    val totalSessionCount: Int,
    val currentStreak: Int,
    val longestStreak: Int,
    val totalMinutesAllTime: Int,
    val maxMinutesOnSinglePiece: Int,
    val sessionHadPieces: Boolean,
    val sessionHadNoSkips: Boolean
)

/**
 * Pure, stateless evaluator. Returns every milestone whose condition is met
 * given the current snapshot. Callers are responsible for filtering out
 * milestones that have already been stored in the database.
 */
object MilestoneEvaluator {

    fun evaluate(input: MilestoneInput): List<Milestone> = buildList {
        with(input) {
            if (totalSessionCount == 1)                          add(Milestone.FIRST_SESSION)
            if (sessionHadPieces && sessionHadNoSkips)           add(Milestone.PERFECT_SESSION)

            if (currentStreak >= 3)                              add(Milestone.STREAK_3)
            if (currentStreak >= 7)                              add(Milestone.STREAK_7)
            if (currentStreak >= 30)                             add(Milestone.STREAK_30)
            // Qualifies when the current streak IS the all-time record (≥ 2 days)
            if (currentStreak >= 2 && currentStreak >= longestStreak) add(Milestone.STREAK_RECORD)

            if (totalMinutesAllTime >= 60)                       add(Milestone.HOURS_1)
            if (totalMinutesAllTime >= 600)                      add(Milestone.HOURS_10)
            if (totalMinutesAllTime >= 3_000)                    add(Milestone.HOURS_50)
            if (totalMinutesAllTime >= 6_000)                    add(Milestone.HOURS_100)
            if (totalMinutesAllTime >= 30_000)                   add(Milestone.HOURS_500)

            if (maxMinutesOnSinglePiece >= 60)                   add(Milestone.PIECE_HOURS_1)
            if (maxMinutesOnSinglePiece >= 600)                  add(Milestone.PIECE_HOURS_10)
        }
    }
}
