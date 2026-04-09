package com.practicetracker.ui.practice

import android.app.NotificationManager
import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.practicetracker.data.repository.PieceRepository
import com.practicetracker.data.repository.PlanRepository
import com.practicetracker.data.repository.SessionRepository
import com.practicetracker.domain.model.Piece
import com.practicetracker.domain.model.SessionEntry
import com.practicetracker.domain.model.Skill
import com.practicetracker.domain.model.SkillCheck
import com.practicetracker.service.SessionNotificationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject

data class ActiveSessionUiState(
    val sessionId: String = "",
    val isLoading: Boolean = true,
    val isPaused: Boolean = false,
    val sessionElapsedSeconds: Long = 0L,
    val selectedPieceIndex: Int? = null,
    val entries: List<SessionEntryUiState> = emptyList(),
    val planName: String? = null
)

data class SessionEntryUiState(
    val entryId: String,
    val pieceId: String?,
    val pieceTitle: String,
    val pieceType: String,
    val suggestedMinutes: Int,
    val composer: String?,
    val book: String?,
    val pages: String?,
    val notes: String?,
    val skills: List<Skill>,
    val checkedSkillIds: Set<String>,
    val elapsedSeconds: Long,
    val isStarted: Boolean,
    val isDone: Boolean,
    val isSkipped: Boolean
)

@HiltViewModel
class ActiveSessionViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val planRepository: PlanRepository,
    private val pieceRepository: PieceRepository,
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val sessionId: String = checkNotNull(savedStateHandle["sessionId"])

    private val _uiState = MutableStateFlow(ActiveSessionUiState(sessionId = sessionId))
    val uiState: StateFlow<ActiveSessionUiState> = _uiState.asStateFlow()

    private val _sessionEnded = MutableSharedFlow<String>()
    val sessionEnded: SharedFlow<String> = _sessionEnded.asSharedFlow()

    private var sessionStartWallClock: Long = 0L
    private var sessionPausedAccumulatedMs: Long = 0L
    private var sessionPauseStartMs: Long = 0L

    // Per-piece timing for the currently open piece
    private var activePieceOpenedAtMs: Long = 0L
    private var activePiecePausedMs: Long = 0L
    // Accumulated elapsed ms per entry from previous opens
    private val pieceAccumMs = mutableMapOf<String, Long>()

    private var tickerJob: Job? = null

    private val entryIds = mutableListOf<String>()
    private val entryMap = mutableMapOf<String, SessionEntry>()
    private val pieceMap = mutableMapOf<String, Piece>()
    private val checkedSkillsMap = mutableMapOf<String, MutableSet<String>>()

    init {
        viewModelScope.launch { loadSession() }
    }

    private suspend fun loadSession() {
        val session = sessionRepository.getInProgressSession()
            ?: run {
                _sessionEnded.emit(sessionId)
                return
            }

        val planName = session.planId?.let {
            planRepository.getPlanWithEntries(it).firstOrNull()?.name
        }

        sessionRepository.getSession(sessionId).firstOrNull()?.let { fullSession ->
            fullSession.entries.forEach { entry ->
                entryIds += entry.id
                entryMap[entry.id] = entry
                checkedSkillsMap[entry.id] = entry.skillChecks.map { it.skillId }.toMutableSet()
                // Pre-populate elapsed for already-finished pieces
                if (entry.startTime != null && entry.endTime != null) {
                    pieceAccumMs[entry.id] = ChronoUnit.MILLIS
                        .between(entry.startTime, entry.endTime)
                        .coerceAtLeast(0L)
                }
                entry.pieceId?.let { pid ->
                    pieceRepository.getPieceWithSkills(pid).firstOrNull()?.let { piece ->
                        pieceMap[pid] = piece
                    }
                }
            }
        }

        sessionStartWallClock = System.currentTimeMillis() -
            (Instant.now().toEpochMilli() - session.startTime.toEpochMilli())

        rebuildUiState(planName)
        startTicker()
    }

    fun selectPiece(index: Int) {
        if (index >= entryIds.size) return
        // Flush any currently open piece
        flushActivePieceTime()

        val entryId = entryIds[index]
        activePieceOpenedAtMs = System.currentTimeMillis()
        activePiecePausedMs = 0L

        // Mark startTime in DB if this is the first time opening
        val entry = entryMap[entryId] ?: return
        if (entry.startTime == null) {
            val started = entry.copy(startTime = Instant.now())
            entryMap[entryId] = started
            viewModelScope.launch { sessionRepository.saveSessionEntry(started) }
        }

        _uiState.update { it.copy(selectedPieceIndex = index) }
        rebuildUiState()
    }

    fun returnToOverview() {
        flushActivePieceTime()
        _uiState.update { it.copy(selectedPieceIndex = null) }
        rebuildUiState()
    }

    private fun flushActivePieceTime() {
        val index = _uiState.value.selectedPieceIndex ?: return
        val entryId = entryIds.getOrNull(index) ?: return
        if (activePieceOpenedAtMs == 0L) return
        val now = System.currentTimeMillis()
        val elapsed = ((now - activePieceOpenedAtMs) - activePiecePausedMs).coerceAtLeast(0L)
        pieceAccumMs[entryId] = (pieceAccumMs[entryId] ?: 0L) + elapsed
        activePieceOpenedAtMs = 0L
        activePiecePausedMs = 0L
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (isActive) {
                delay(1_000)
                if (!_uiState.value.isPaused) {
                    val now = System.currentTimeMillis()
                    val sessionElapsed = ((now - sessionStartWallClock - sessionPausedAccumulatedMs) / 1000)
                        .coerceAtLeast(0)
                    _uiState.update { it.copy(sessionElapsedSeconds = sessionElapsed) }
                    if (_uiState.value.selectedPieceIndex != null) {
                        rebuildUiState()
                    }
                    val nm = context.getSystemService(NotificationManager::class.java)
                    nm.notify(
                        SessionNotificationHelper.NOTIFICATION_ID,
                        SessionNotificationHelper.buildNotification(context, sessionElapsed)
                    )
                }
            }
        }
    }

    fun togglePause() {
        val now = System.currentTimeMillis()
        _uiState.update { state ->
            if (state.isPaused) {
                val pausedDuration = now - sessionPauseStartMs
                sessionPausedAccumulatedMs += pausedDuration
                if (state.selectedPieceIndex != null) {
                    activePiecePausedMs += pausedDuration
                }
                state.copy(isPaused = false)
            } else {
                sessionPauseStartMs = now
                state.copy(isPaused = true)
            }
        }
    }

    fun checkSkill(skillId: String) {
        val index = _uiState.value.selectedPieceIndex ?: return
        if (index >= entryIds.size) return
        val entryId = entryIds[index]
        checkedSkillsMap.getOrPut(entryId) { mutableSetOf() }.add(skillId)
        viewModelScope.launch {
            sessionRepository.recordSkillCheck(
                SkillCheck(skillId = skillId, checkedAt = Instant.now()),
                entryId
            )
        }
        rebuildUiState()
    }

    fun uncheckSkill(skillId: String) {
        val index = _uiState.value.selectedPieceIndex ?: return
        if (index >= entryIds.size) return
        val entryId = entryIds[index]
        checkedSkillsMap[entryId]?.remove(skillId)
        viewModelScope.launch {
            sessionRepository.removeSkillCheck(
                SkillCheck(skillId = skillId, checkedAt = Instant.now()),
                entryId
            )
        }
        rebuildUiState()
    }

    fun donePiece() {
        val index = _uiState.value.selectedPieceIndex ?: return
        val entryId = entryIds.getOrNull(index) ?: return
        val entry = entryMap[entryId] ?: return
        flushActivePieceTime()
        val completed = entry.copy(endTime = Instant.now())
        entryMap[entryId] = completed
        viewModelScope.launch { sessionRepository.updateSessionEntry(completed) }
        _uiState.update { it.copy(selectedPieceIndex = null) }
        rebuildUiState()
    }

    fun skipPiece() {
        val index = _uiState.value.selectedPieceIndex ?: return
        val entryId = entryIds.getOrNull(index) ?: return
        val entry = entryMap[entryId] ?: return
        flushActivePieceTime()
        val skipped = entry.copy(skipped = true, endTime = Instant.now())
        entryMap[entryId] = skipped
        viewModelScope.launch { sessionRepository.updateSessionEntry(skipped) }
        _uiState.update { it.copy(selectedPieceIndex = null) }
        rebuildUiState()
    }

    fun endSession() {
        tickerJob?.cancel()
        context.getSystemService(NotificationManager::class.java)
            .cancel(SessionNotificationHelper.NOTIFICATION_ID)
        viewModelScope.launch {
            val session = sessionRepository.getInProgressSession()
            session?.let {
                val ended = it.copy(endTime = Instant.now())
                sessionRepository.updateSession(ended)
            }
            _sessionEnded.emit(sessionId)
        }
    }

    private fun rebuildUiState(planName: String? = _uiState.value.planName) {
        val selectedIndex = _uiState.value.selectedPieceIndex
        val now = System.currentTimeMillis()

        val entryUiStates = entryIds.mapIndexed { index, entryId ->
            val entry = entryMap[entryId]
            val piece = entry?.pieceId?.let { pieceMap[it] }
            val checked = checkedSkillsMap[entryId] ?: emptySet<String>()
            val isSelected = index == selectedIndex

            val elapsedSeconds = if (isSelected && activePieceOpenedAtMs > 0L) {
                val sinceOpen = ((now - activePieceOpenedAtMs) - activePiecePausedMs).coerceAtLeast(0L)
                ((pieceAccumMs[entryId] ?: 0L) + sinceOpen) / 1000
            } else {
                (pieceAccumMs[entryId] ?: 0L) / 1000
            }

            SessionEntryUiState(
                entryId = entryId,
                pieceId = entry?.pieceId,
                pieceTitle = piece?.title ?: "[Deleted piece]",
                pieceType = piece?.type?.name ?: "",
                suggestedMinutes = piece?.suggestedMinutes ?: 0,
                composer = piece?.composer,
                book = piece?.book,
                pages = piece?.pages,
                notes = piece?.notes,
                skills = piece?.skills ?: emptyList(),
                checkedSkillIds = checked,
                elapsedSeconds = elapsedSeconds,
                isStarted = entry?.startTime != null,
                isDone = entry?.endTime != null && entry.skipped.not(),
                isSkipped = entry?.skipped == true
            )
        }
        _uiState.update { state ->
            state.copy(
                isLoading = false,
                entries = entryUiStates,
                planName = planName
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        tickerJob?.cancel()
        context.getSystemService(NotificationManager::class.java)
            .cancel(SessionNotificationHelper.NOTIFICATION_ID)
    }
}
