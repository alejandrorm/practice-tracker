package com.practicetracker.ui.practice

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.practicetracker.data.datastore.UserProfile
import com.practicetracker.data.datastore.UserProfileStore
import com.practicetracker.data.repository.Achievement
import com.practicetracker.data.repository.AchievementRepository
import com.practicetracker.data.repository.PieceRepository
import com.practicetracker.data.repository.SessionRepository
import com.practicetracker.domain.engine.MilestoneEvaluator
import com.practicetracker.domain.engine.MilestoneInput
import com.practicetracker.domain.model.Milestone
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class SessionSummaryViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val pieceRepository: PieceRepository,
    private val achievementRepository: AchievementRepository,
    private val userProfileStore: UserProfileStore,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val sessionId: String = checkNotNull(savedStateHandle["sessionId"])

    data class SummaryUiState(
        val isLoading: Boolean = true,
        val totalDurationSeconds: Long = 0,
        val entryRows: List<EntryRow> = emptyList(),
        val allPiecesCompleted: Boolean = false
    )

    data class EntryRow(
        val pieceTitle: String,
        val durationSeconds: Long,
        val isSkipped: Boolean,
        val checkedSkillLabels: List<String>
    )

    private val _uiState = MutableStateFlow(SummaryUiState())
    val uiState: StateFlow<SummaryUiState> = _uiState.asStateFlow()

    /** Milestones earned for the first time in this session. Empty until evaluation completes. */
    private val _newlyEarned = MutableStateFlow<List<Milestone>>(emptyList())
    val newlyEarned: StateFlow<List<Milestone>> = _newlyEarned.asStateFlow()

    /** Earned achievements with timestamps, for the share card. */
    private val _earnedAchievements = MutableStateFlow<Map<Milestone, Achievement>>(emptyMap())
    val earnedAchievements: StateFlow<Map<Milestone, Achievement>> = _earnedAchievements.asStateFlow()

    val userProfile: StateFlow<UserProfile> = userProfileStore.profile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserProfile())

    init {
        viewModelScope.launch { loadSummary() }
    }

    private suspend fun loadSummary() {
        sessionRepository.getSession(sessionId).firstOrNull()?.let { session ->
            val totalSeconds = session.endTime?.let { end ->
                (end.toEpochMilli() - session.startTime.toEpochMilli()) / 1000
            } ?: 0L

            val rows = session.entries.map { entry ->
                val piece = entry.pieceId?.let {
                    pieceRepository.getPieceWithSkills(it).firstOrNull()
                }
                val durationSec = if (entry.startTime != null && entry.endTime != null)
                    (entry.endTime.toEpochMilli() - entry.startTime.toEpochMilli()) / 1000
                else 0L
                val checkedLabels = entry.skillChecks.mapNotNull { check ->
                    piece?.skills?.find { it.id == check.skillId }?.label
                }
                EntryRow(
                    pieceTitle = piece?.title ?: "[Deleted piece]",
                    durationSeconds = durationSec,
                    isSkipped = entry.skipped,
                    checkedSkillLabels = checkedLabels
                )
            }

            val allCompleted = session.entries.isNotEmpty() &&
                session.entries.none { it.skipped } &&
                session.entries.all { it.endTime != null }

            _uiState.value = SummaryUiState(
                isLoading = false,
                totalDurationSeconds = totalSeconds,
                entryRows = rows,
                allPiecesCompleted = allCompleted
            )

            // Evaluate milestones only when this is a freshly ended session (endTime just set)
            if (session.endTime != null) {
                evaluateMilestones(
                    sessionHadPieces = session.entries.isNotEmpty(),
                    sessionHadNoSkips = allCompleted
                )
            }
        }
    }

    private suspend fun evaluateMilestones(sessionHadPieces: Boolean, sessionHadNoSkips: Boolean) {
        val totalCount   = sessionRepository.getTotalCompletedSessionCount()
        val streak       = sessionRepository.calculateCurrentStreak()
        val longest      = sessionRepository.calculateLongestStreak()
        val totalMinutes = sessionRepository.getTotalMinutesAllTime()
        val maxPiece     = sessionRepository.getMaxMinutesOnSinglePiece()

        val input = MilestoneInput(
            totalSessionCount      = totalCount,
            currentStreak          = streak,
            longestStreak          = longest,
            totalMinutesAllTime    = totalMinutes,
            maxMinutesOnSinglePiece = maxPiece,
            sessionHadPieces       = sessionHadPieces,
            sessionHadNoSkips      = sessionHadNoSkips
        )

        val qualifying   = MilestoneEvaluator.evaluate(input)
        val alreadyEarned = achievementRepository.getEarnedMilestoneIds()
        val newly        = qualifying.filter { it.name !in alreadyEarned }

        if (newly.isNotEmpty()) {
            val earnedAt = Instant.now()
            newly.forEach { achievementRepository.insertAchievement(it, earnedAt) }
            _newlyEarned.value = newly
            _earnedAchievements.value = newly.associateWith { Achievement(it, earnedAt) }
        }
    }
}
