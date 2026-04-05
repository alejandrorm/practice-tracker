package com.practicetracker.ui.practice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.practicetracker.data.repository.PlanRepository
import com.practicetracker.data.repository.SessionRepository
import com.practicetracker.domain.model.PracticePlan
import com.practicetracker.domain.model.PracticeSession
import com.practicetracker.domain.model.SessionEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class SessionHomeViewModel @Inject constructor(
    private val planRepository: PlanRepository,
    private val sessionRepository: SessionRepository
) : ViewModel() {

    private val _todaysPlan = MutableStateFlow<PracticePlan?>(null)
    val todaysPlan: StateFlow<PracticePlan?> = _todaysPlan.asStateFlow()

    private val _inProgressSession = MutableStateFlow<PracticeSession?>(null)
    val inProgressSession: StateFlow<PracticeSession?> = _inProgressSession.asStateFlow()

    private val _startedSessionId = MutableSharedFlow<String>()
    val startedSessionId: SharedFlow<String> = _startedSessionId.asSharedFlow()

    init {
        viewModelScope.launch {
            _todaysPlan.value = planRepository.getPlanForDate(LocalDate.now())
            _inProgressSession.value = sessionRepository.getInProgressSession()
        }
    }

    fun startSession(plan: PracticePlan?) {
        viewModelScope.launch {
            val session = PracticeSession(
                id = UUID.randomUUID().toString(),
                planId = plan?.id,
                date = LocalDate.now(),
                startTime = Instant.now(),
                endTime = null,
                entries = emptyList()
            )
            sessionRepository.saveSession(session)
            plan?.entries?.forEachIndexed { index, planEntry ->
                planEntry.pieceId?.let { pieceId ->
                    val entry = SessionEntry(
                        id = UUID.randomUUID().toString(),
                        sessionId = session.id,
                        pieceId = pieceId,
                        startTime = null,
                        endTime = null,
                        skipped = false,
                        skillChecks = emptyList()
                    )
                    sessionRepository.saveSessionEntry(entry)
                }
            }
            _startedSessionId.emit(session.id)
        }
    }

    fun discardInProgressSession() {
        viewModelScope.launch {
            _inProgressSession.value?.let { session ->
                sessionRepository.deleteSession(session)
                _inProgressSession.value = null
            }
        }
    }
}
