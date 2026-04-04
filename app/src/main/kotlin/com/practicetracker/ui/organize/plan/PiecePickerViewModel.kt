package com.practicetracker.ui.organize.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.practicetracker.data.repository.PieceRepository
import com.practicetracker.domain.model.Piece
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class PiecePickerViewModel @Inject constructor(
    pieceRepository: PieceRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val filteredPieces: StateFlow<List<Piece>> = combine(
        pieceRepository.getAllPiecesWithSkills(),
        _searchQuery
    ) { pieces, query ->
        if (query.isBlank()) pieces
        else pieces.filter { piece ->
            piece.title.contains(query, ignoreCase = true) ||
                    piece.composer?.contains(query, ignoreCase = true) == true
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setSearchQuery(q: String) { _searchQuery.value = q }
}
