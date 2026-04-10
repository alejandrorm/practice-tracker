package com.practicetracker.ui.organize.piece

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.practicetracker.data.datastore.SettingsStore
import com.practicetracker.data.datastore.UserProfileStore
import com.practicetracker.data.repository.PieceRepository
import com.practicetracker.data.repository.RepertoireRepository
import com.practicetracker.domain.engine.SuggestionEngine
import com.practicetracker.domain.model.Piece
import com.practicetracker.domain.model.PieceType
import com.practicetracker.domain.model.Skill
import com.practicetracker.domain.model.SkillScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.firstOrNull as flowFirstOrNull

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class PieceEditorViewModel @Inject constructor(
    private val pieceRepository: PieceRepository,
    private val settingsStore: SettingsStore,
    private val userProfileStore: UserProfileStore,
    private val repertoireRepository: RepertoireRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val pieceId: String = savedStateHandle["pieceId"] ?: "new"
    val isNewPiece = pieceId == "new"

    val title = MutableStateFlow("")
    val type = MutableStateFlow(PieceType.SONG)
    val composer = MutableStateFlow("")
    val book = MutableStateFlow("")
    val pages = MutableStateFlow("")
    val notes = MutableStateFlow("")
    val suggestedMinutes = MutableStateFlow(5)
    val attachedSkills = MutableStateFlow<List<Skill>>(emptyList())
    val level = MutableStateFlow<Int?>(null)
    val levelAlias = MutableStateFlow<String?>(null)

    private val _selectedRepertoireEntry = MutableStateFlow<RepertoireRepository.RepertoireEntry?>(null)

    private val _skillSearchQuery = MutableStateFlow("")
    val skillSearchQuery: StateFlow<String> = _skillSearchQuery.asStateFlow()

    private val userSkillLevel: StateFlow<String> = userProfileStore.profile
        .map { it.skillLevel }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    /** Repertoire title suggestions (shown while typing the title). */
    val repertoireSuggestions: StateFlow<List<RepertoireRepository.RepertoireEntry>> =
        title.debounce(200)
            .combine(userSkillLevel) { t, level -> t to level }
            .map { (t, lvl) ->
                if (t.length < 2) emptyList()
                else repertoireRepository.searchRepertoire(t, lvl).take(6)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Existing skills from the DB library. */
    val skillSearchResults: StateFlow<List<Skill>> =
        _skillSearchQuery
            .debounce(150)
            .flatMapLatest { query ->
                if (query.isBlank()) flowOf(emptyList())
                else pieceRepository.searchSkillsInLibrary(query)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Practice items from violin_practice.json matching the skill search query. */
    val practiceSearchResults: StateFlow<List<String>> =
        _skillSearchQuery
            .debounce(150)
            .map { query ->
                if (query.length < 2) emptyList()
                else repertoireRepository.searchPracticeItems(query).take(5)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * Skill suggestions: uses the repertoire entry's Suggested_practice when a repertoire
     * piece was selected; otherwise falls back to type-based SuggestionEngine suggestions.
     */
    val suggestedSkillLabels: StateFlow<List<String>> =
        combine(type, _selectedRepertoireEntry) { pieceType, entry ->
            entry?.suggestedPractice ?: SuggestionEngine.suggestSkills(pieceType, "")
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _saveComplete = MutableSharedFlow<Unit>()
    val saveComplete: SharedFlow<Unit> = _saveComplete.asSharedFlow()

    init {
        if (!isNewPiece) {
            viewModelScope.launch {
                pieceRepository.getPieceWithSkills(pieceId).flowFirstOrNull()?.let { piece ->
                    title.value = piece.title
                    type.value = piece.type
                    composer.value = piece.composer ?: ""
                    book.value = piece.book ?: ""
                    pages.value = piece.pages ?: ""
                    notes.value = piece.notes ?: ""
                    suggestedMinutes.value = piece.suggestedMinutes
                    attachedSkills.value = piece.skills
                    level.value = piece.level
                    levelAlias.value = piece.levelAlias
                    // Restore repertoire entry link if title matches
                    _selectedRepertoireEntry.value = repertoireRepository.findByTitle(piece.title)
                }
            }
        } else {
            viewModelScope.launch {
                settingsStore.settings.flowFirstOrNull()?.let {
                    suggestedMinutes.value = it.defaultSuggestedMinutes
                }
            }
        }
    }

    /** Called when the user selects a piece from the repertoire autocomplete. */
    fun selectRepertoireEntry(entry: RepertoireRepository.RepertoireEntry) {
        title.value = entry.name
        composer.value = entry.composer
        level.value = entry.level
        levelAlias.value = entry.levelAlias
        _selectedRepertoireEntry.value = entry
    }

    fun setSkillSearchQuery(q: String) { _skillSearchQuery.value = q }

    fun addSkillByLabel(label: String) {
        viewModelScope.launch {
            val skill = pieceRepository.getOrCreateSkill(label, SkillScope.PIECE_SPECIFIC)
            if (attachedSkills.value.none { it.id == skill.id }) {
                attachedSkills.value = attachedSkills.value + skill
            }
            _skillSearchQuery.value = ""
        }
    }

    fun removeSkill(skill: Skill) {
        attachedSkills.value = attachedSkills.value.filter { it.id != skill.id }
    }

    fun moveSkillUp(index: Int) {
        if (index <= 0) return
        val list = attachedSkills.value.toMutableList()
        val tmp = list[index]; list[index] = list[index - 1]; list[index - 1] = tmp
        attachedSkills.value = list
    }

    fun moveSkillDown(index: Int) {
        val list = attachedSkills.value.toMutableList()
        if (index >= list.size - 1) return
        val tmp = list[index]; list[index] = list[index + 1]; list[index + 1] = tmp
        attachedSkills.value = list
    }

    fun save() {
        viewModelScope.launch {
            val id = if (isNewPiece) UUID.randomUUID().toString() else pieceId
            val piece = Piece(
                id = id,
                title = title.value.trim(),
                type = type.value,
                composer = composer.value.trim().ifBlank { null },
                book = book.value.trim().ifBlank { null },
                pages = pages.value.trim().ifBlank { null },
                notes = notes.value.trim().ifBlank { null },
                suggestedMinutes = suggestedMinutes.value,
                skills = attachedSkills.value,
                createdAt = Instant.now(),
                level = level.value,
                levelAlias = levelAlias.value
            )
            pieceRepository.savePiece(piece)
            _saveComplete.emit(Unit)
        }
    }
}
