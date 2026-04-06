package com.practicetracker.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.practicetracker.data.db.dao.PieceStatRow
import com.practicetracker.data.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import javax.inject.Inject

enum class StatsWindow(val days: Long, val label: String) {
    WEEK(7, "7 days"),
    MONTH(30, "30 days"),
    YEAR(365, "1 year")
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class StatsViewModel @Inject constructor(
    private val sessionRepository: SessionRepository
) : ViewModel() {

    private val _window = MutableStateFlow(StatsWindow.WEEK)
    val window: StateFlow<StatsWindow> = _window.asStateFlow()

    val totalMinutes: StateFlow<Int> = _window.flatMapLatest { w ->
        val end = LocalDate.now()
        val start = end.minusDays(w.days - 1)
        sessionRepository.getTotalMinutesInRange(start, end)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val sessionCount: StateFlow<Int> = _window.flatMapLatest { w ->
        val end = LocalDate.now()
        val start = end.minusDays(w.days - 1)
        sessionRepository.getSessionCountInRange(start, end)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val pieceStats: StateFlow<List<PieceStatRow>> = _window.flatMapLatest { w ->
        val end = LocalDate.now()
        val start = end.minusDays(w.days - 1)
        sessionRepository.getPieceStatsInRange(start, end)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Streaks are derived from all sessions regardless of the selected window
    val currentStreak: StateFlow<Int> = sessionRepository.getAllSessions()
        .map { sessions ->
            val dates = sessions.filter { it.endTime != null }.map { it.date }.toSet()
            var streak = 0
            var day = LocalDate.now()
            while (day in dates) { streak++; day = day.minusDays(1) }
            streak
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val longestStreak: StateFlow<Int> = sessionRepository.getAllSessions()
        .map { sessions ->
            val dates = sessions
                .filter { it.endTime != null }
                .map { it.date }
                .distinct()
                .sortedDescending()
            if (dates.isEmpty()) return@map 0
            var longest = 1; var current = 1
            for (i in 1 until dates.size) {
                current = if (dates[i - 1].minusDays(1) == dates[i]) current + 1 else 1
                if (current > longest) longest = current
            }
            longest
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    // Set of dates where a completed session exists, for the activity grid
    val practicedDates: StateFlow<Set<LocalDate>> = sessionRepository.getAllSessions()
        .map { sessions ->
            sessions.filter { it.endTime != null }.map { it.date }.toSet()
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    fun selectWindow(w: StatsWindow) { _window.value = w }
}
