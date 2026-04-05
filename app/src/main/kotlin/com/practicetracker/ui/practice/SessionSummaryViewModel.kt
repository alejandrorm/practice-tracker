package com.practicetracker.ui.practice

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.practicetracker.data.repository.PieceRepository
import com.practicetracker.data.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SessionSummaryViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val pieceRepository: PieceRepository,
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
        }
    }
}
