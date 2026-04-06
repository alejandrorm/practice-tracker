package com.practicetracker.ui.stats

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.practicetracker.data.db.dao.SkillSessionRow
import com.practicetracker.data.repository.PieceRepository
import com.practicetracker.data.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SkillDrillDownViewModel @Inject constructor(
    pieceRepository: PieceRepository,
    sessionRepository: SessionRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val skillId: String = checkNotNull(savedStateHandle["skillId"])

    private val _skillLabel = MutableStateFlow<String?>(null)
    val skillLabel: StateFlow<String?> = _skillLabel.asStateFlow()

    val sessionRows: StateFlow<List<SkillSessionRow>> =
        sessionRepository.getSessionRowsForSkill(skillId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            _skillLabel.value = pieceRepository.getSkillById(skillId)?.label
        }
    }
}
