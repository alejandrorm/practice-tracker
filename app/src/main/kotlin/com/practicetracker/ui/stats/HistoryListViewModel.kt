package com.practicetracker.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.practicetracker.data.repository.PlanRepository
import com.practicetracker.data.repository.SessionRepository
import com.practicetracker.domain.model.PracticeSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class HistoryRow(
    val sessionId: String,
    val session: PracticeSession,
    val date: LocalDate,
    val subtitle: String,
    val durationSeconds: Long,
    val pieceCount: Int
)

@HiltViewModel
class HistoryListViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    planRepository: PlanRepository
) : ViewModel() {

    // ID of session that is visually removed but not yet deleted from DB
    private val _pendingDeleteId = MutableStateFlow<String?>(null)
    val pendingDeleteId: StateFlow<String?> = _pendingDeleteId.asStateFlow()

    val history: StateFlow<List<HistoryRow>> = combine(
        sessionRepository.getAllSessions(),
        planRepository.getAllPlans(),
        _pendingDeleteId
    ) { sessions, plans, pendingId ->
        val planMap = plans.associateBy { it.id }
        sessions
            .filter { it.endTime != null && it.id != pendingId }
            .sortedByDescending { it.startTime }
            .map { session ->
                val planName = session.planId?.let { planMap[it]?.name }
                val durationSec = session.endTime?.let { end ->
                    (end.toEpochMilli() - session.startTime.toEpochMilli()) / 1000L
                } ?: 0L
                HistoryRow(
                    sessionId = session.id,
                    session = session,
                    date = session.date,
                    subtitle = planName ?: "${session.entries.size} piece${if (session.entries.size != 1) "s" else ""}",
                    durationSeconds = durationSec,
                    pieceCount = session.entries.size
                )
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Hides the session from the list immediately; DB delete is deferred until [confirmDelete]. */
    fun requestDelete(sessionId: String) {
        _pendingDeleteId.value = sessionId
    }

    /** Called when the snackbar undo action is tapped — restores the item. */
    fun cancelDelete() {
        _pendingDeleteId.value = null
    }

    /** Called when the snackbar is dismissed without undo — actually deletes from DB. */
    fun confirmDelete(session: PracticeSession) {
        _pendingDeleteId.value = null
        viewModelScope.launch { sessionRepository.deleteSession(session) }
    }
}
