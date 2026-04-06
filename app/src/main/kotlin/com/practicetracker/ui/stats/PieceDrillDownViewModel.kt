package com.practicetracker.ui.stats

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.practicetracker.data.db.dao.PieceSessionRow
import com.practicetracker.data.db.dao.SkillCheckFreqRow
import com.practicetracker.data.repository.PieceRepository
import com.practicetracker.data.repository.SessionRepository
import com.practicetracker.domain.model.Piece
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class PieceTotals(val totalMinutes: Int, val sessionCount: Int)

@HiltViewModel
class PieceDrillDownViewModel @Inject constructor(
    pieceRepository: PieceRepository,
    sessionRepository: SessionRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val pieceId: String = checkNotNull(savedStateHandle["pieceId"])

    val piece: StateFlow<Piece?> = pieceRepository.getPieceWithSkills(pieceId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val skillFrequency: StateFlow<List<SkillCheckFreqRow>> =
        sessionRepository.getSkillFrequencyForPiece(pieceId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val sessionRows: StateFlow<List<PieceSessionRow>> =
        sessionRepository.getSessionRowsForPiece(pieceId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val totals: StateFlow<PieceTotals> = sessionRows.map { rows ->
        val totalMinutes = rows.sumOf { row ->
            if (row.startTime != null && row.endTime != null)
                (row.endTime.toEpochMilli() - row.startTime.toEpochMilli()) / 60_000L
            else 0L
        }.toInt()
        val sessionCount = rows.map { it.sessionId }.distinct().size
        PieceTotals(totalMinutes, sessionCount)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PieceTotals(0, 0))
}
