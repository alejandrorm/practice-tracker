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
import javax.inject.Inject

data class ActiveSessionUiState(
    val sessionId: String = "",
    val isLoading: Boolean = true,
    val isPaused: Boolean = false,
    val sessionElapsedSeconds: Long = 0L,
    val currentPieceIndex: Int = 0,
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

    private var pieceStartWallClock: Long = 0L
    private var piecePausedAccumulatedMs: Long = 0L

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
                checkedSkillsMap[entry.id] = mutableSetOf()
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

        if (entryIds.isNotEmpty()) {
            startCurrentPiece()
        }
    }

    private fun startCurrentPiece() {
        val index = _uiState.value.currentPieceIndex
        if (index >= entryIds.size) return
        val entryId = entryIds[index]
        val entry = entryMap[entryId] ?: return
        if (entry.startTime == null) {
            val started = entry.copy(startTime = Instant.now())
            entryMap[entryId] = started
            viewModelScope.launch { sessionRepository.saveSessionEntry(started) }
            pieceStartWallClock = System.currentTimeMillis()
            piecePausedAccumulatedMs = 0L
        }
        rebuildUiState()
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (isActive) {
                delay(1_000)
                if (!_uiState.value.isPaused) {
                    val now = System.currentTimeMillis()
                    val sessionElapsed = (now - sessionStartWallClock - sessionPausedAccumulatedMs) / 1000
                    val pieceElapsed = (now - pieceStartWallClock - piecePausedAccumulatedMs) / 1000

                    _uiState.update { state ->
                        val updatedEntries = state.entries.mapIndexed { i, entry ->
                            if (i == state.currentPieceIndex && entry.isStarted && !entry.isDone && !entry.isSkipped)
                                entry.copy(elapsedSeconds = pieceElapsed.coerceAtLeast(0))
                            else entry
                        }
                        state.copy(
                            sessionElapsedSeconds = sessionElapsed.coerceAtLeast(0),
                            entries = updatedEntries
                        )
                    }

                    // Update notification
                    val nm = context.getSystemService(NotificationManager::class.java)
                    nm.notify(
                        SessionNotificationHelper.NOTIFICATION_ID,
                        SessionNotificationHelper.buildNotification(context, sessionElapsed.coerceAtLeast(0))
                    )
                }
            }
        }
    }

    fun togglePause() {
        val now = System.currentTimeMillis()
        _uiState.update { state ->
            if (state.isPaused) {
                sessionPausedAccumulatedMs += now - sessionPauseStartMs
                piecePausedAccumulatedMs += now - sessionPauseStartMs
                state.copy(isPaused = false)
            } else {
                sessionPauseStartMs = now
                state.copy(isPaused = true)
            }
        }
    }

    fun checkSkill(skillId: String) {
        val index = _uiState.value.currentPieceIndex
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
        val index = _uiState.value.currentPieceIndex
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
        val index = _uiState.value.currentPieceIndex
        if (index >= entryIds.size) return
        val entryId = entryIds[index]
        val entry = entryMap[entryId] ?: return
        val completed = entry.copy(endTime = Instant.now())
        entryMap[entryId] = completed
        viewModelScope.launch { sessionRepository.updateSessionEntry(completed) }

        if (index + 1 < entryIds.size) {
            _uiState.update { it.copy(currentPieceIndex = index + 1) }
            pieceStartWallClock = System.currentTimeMillis()
            piecePausedAccumulatedMs = 0L
            startCurrentPiece()
        } else {
            endSession()
        }
    }

    fun skipPiece() {
        val index = _uiState.value.currentPieceIndex
        if (index >= entryIds.size) return
        val entryId = entryIds[index]
        val entry = entryMap[entryId] ?: return
        val skipped = entry.copy(skipped = true, endTime = Instant.now())
        entryMap[entryId] = skipped
        viewModelScope.launch { sessionRepository.updateSessionEntry(skipped) }

        if (index + 1 < entryIds.size) {
            _uiState.update { it.copy(currentPieceIndex = index + 1) }
            pieceStartWallClock = System.currentTimeMillis()
            piecePausedAccumulatedMs = 0L
            startCurrentPiece()
        } else {
            endSession()
        }
    }

    fun jumpToPiece(targetIndex: Int) {
        donePiece()
        if (targetIndex < entryIds.size) {
            _uiState.update { it.copy(currentPieceIndex = targetIndex) }
            pieceStartWallClock = System.currentTimeMillis()
            piecePausedAccumulatedMs = 0L
            startCurrentPiece()
        }
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
        val currentIndex = _uiState.value.currentPieceIndex
        val entryUiStates = entryIds.mapIndexed { index, entryId ->
            val entry = entryMap[entryId]
            val piece = entry?.pieceId?.let { pieceMap[it] }
            val checked = checkedSkillsMap[entryId] ?: emptySet<String>()
            val isCurrentPiece = index == currentIndex

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
                elapsedSeconds = if (isCurrentPiece) _uiState.value.entries.getOrNull(index)?.elapsedSeconds ?: 0L else 0L,
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
