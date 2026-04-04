package com.practicetracker.ui.organize.piece

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.practicetracker.data.repository.PieceRepository
import com.practicetracker.domain.model.Piece
import com.practicetracker.domain.model.PieceType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PieceListViewModel @Inject constructor(
    private val pieceRepository: PieceRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _typeFilter = MutableStateFlow<PieceType?>(null)
    val typeFilter: StateFlow<PieceType?> = _typeFilter.asStateFlow()

    val pieces: StateFlow<List<Piece>> = combine(
        pieceRepository.getAllPiecesWithSkills(),
        _searchQuery,
        _typeFilter
    ) { pieces, query, type ->
        pieces
            .filter { piece ->
                (query.isBlank() || piece.title.contains(query, ignoreCase = true) ||
                        piece.composer?.contains(query, ignoreCase = true) == true ||
                        piece.book?.contains(query, ignoreCase = true) == true)
                        &&
                        (type == null || piece.type == type)
            }
            .sortedBy { it.title }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setSearchQuery(query: String) { _searchQuery.value = query }
    fun setTypeFilter(type: PieceType?) { _typeFilter.value = type }

    fun deletePiece(piece: Piece) {
        viewModelScope.launch { pieceRepository.deletePiece(piece) }
    }
}
