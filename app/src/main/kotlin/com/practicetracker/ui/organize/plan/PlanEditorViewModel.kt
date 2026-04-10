package com.practicetracker.ui.organize.plan

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.practicetracker.data.datastore.UserProfileStore
import com.practicetracker.data.repository.PieceRepository
import com.practicetracker.data.repository.PlanRepository
import com.practicetracker.data.repository.RepertoireRepository
import com.practicetracker.domain.engine.SuggestionEngine
import com.practicetracker.domain.model.Piece
import com.practicetracker.domain.model.PieceType
import com.practicetracker.domain.model.PlanEntry
import com.practicetracker.domain.model.PracticePlan
import com.practicetracker.domain.model.Schedule
import com.practicetracker.domain.model.ScheduleType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.firstOrNull as flowFirstOrNull

data class PlanEntryUiModel(
    val entry: PlanEntry,
    val piece: Piece?
)

@HiltViewModel
class PlanEditorViewModel @Inject constructor(
    private val planRepository: PlanRepository,
    private val pieceRepository: PieceRepository,
    private val repertoireRepository: RepertoireRepository,
    private val userProfileStore: UserProfileStore,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val planId: String = savedStateHandle["planId"] ?: "new"
    val isNewPlan = planId == "new"

    val name = MutableStateFlow("")
    val scheduleType = MutableStateFlow(ScheduleType.DAILY)
    val scheduleDays = MutableStateFlow<Set<DayOfWeek>>(emptySet())
    val scheduleStartDate = MutableStateFlow(LocalDate.now())

    val planEntries = MutableStateFlow<List<PlanEntryUiModel>>(emptyList())

    private val _conflictPlan = MutableStateFlow<PracticePlan?>(null)
    val conflictPlan: StateFlow<PracticePlan?> = _conflictPlan.asStateFlow()

    private val _saveComplete = MutableSharedFlow<Unit>()
    val saveComplete: SharedFlow<Unit> = _saveComplete.asSharedFlow()

    val scaleSuggestions: StateFlow<List<String>> = planEntries.map { entries ->
        val titles = entries.mapNotNull { it.piece?.title }
        SuggestionEngine.suggestScales("violin", titles)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * Practice items suggested by the repertoire for the pieces currently in the plan.
     * Items already in the plan (by title) are excluded.
     */
    val practiceSuggestions: StateFlow<List<String>> = planEntries.map { entries ->
        val inPlanTitles = entries.mapNotNull { it.piece?.title }
            .map { it.lowercase() }.toSet()

        entries.flatMap { model ->
            model.piece?.title
                ?.let { repertoireRepository.findByTitle(it)?.suggestedPractice }
                ?: emptyList()
        }
            .distinct()
            .filter { it.lowercase() !in inPlanTitles }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        if (!isNewPlan) {
            viewModelScope.launch {
                planRepository.getPlanWithEntries(planId).flowFirstOrNull()?.let { plan ->
                    name.value = plan.name
                    scheduleType.value = when (plan.schedule) {
                        is Schedule.Daily -> ScheduleType.DAILY
                        is Schedule.EveryOtherDay -> ScheduleType.EVERY_OTHER_DAY
                        is Schedule.DaysOfWeek -> ScheduleType.DAYS_OF_WEEK
                        is Schedule.Manual -> ScheduleType.MANUAL
                        else -> ScheduleType.DAILY
                    }
                    if (plan.schedule is Schedule.DaysOfWeek) scheduleDays.value = plan.schedule.days
                    if (plan.schedule is Schedule.EveryOtherDay) scheduleStartDate.value = plan.schedule.startDate

                    val entryUiModels = plan.entries.map { entry ->
                        val piece = entry.pieceId?.let {
                            pieceRepository.getPieceWithSkills(it).flowFirstOrNull()
                        }
                        PlanEntryUiModel(entry, piece)
                    }
                    planEntries.value = entryUiModels
                }
            }
        }
    }

    fun addPiece(piece: Piece) {
        val currentPlanId = if (isNewPlan) UUID.randomUUID().toString() else planId
        val entry = PlanEntry(
            id = UUID.randomUUID().toString(),
            planId = currentPlanId,
            pieceId = piece.id,
            order = planEntries.value.size,
            overrideMinutes = null
        )
        planEntries.value = planEntries.value + PlanEntryUiModel(entry, piece)
    }

    /**
     * Finds or creates a piece for [title] and adds it to the plan.
     * Infers piece type from the title and looks up level metadata from the repertoire.
     */
    fun addSuggestedPractice(title: String) {
        viewModelScope.launch {
            val repEntry = repertoireRepository.findByTitle(title)
            val type = inferPieceType(title)
            val piece = pieceRepository.getOrCreatePiece(
                title = title,
                type = type,
                level = repEntry?.level,
                levelAlias = repEntry?.levelAlias
            )
            addPiece(piece)
        }
    }

    private fun inferPieceType(title: String): PieceType {
        val lower = title.lowercase()
        return when {
            lower.contains("scale") || lower.contains("arpeggio") -> PieceType.SCALE
            lower.contains("etude") || lower.contains("étude") ||
                lower.contains("op.") || lower.contains("no.") -> PieceType.ETUDE
            else -> PieceType.EXERCISE
        }
    }

    fun removeEntry(index: Int) {
        planEntries.value = planEntries.value.toMutableList().also { it.removeAt(index) }
    }

    fun moveEntryUp(index: Int) {
        if (index <= 0) return
        val list = planEntries.value.toMutableList()
        val tmp = list[index]; list[index] = list[index - 1]; list[index - 1] = tmp
        planEntries.value = list
    }

    fun moveEntryDown(index: Int) {
        val list = planEntries.value.toMutableList()
        if (index >= list.size - 1) return
        val tmp = list[index]; list[index] = list[index + 1]; list[index + 1] = tmp
        planEntries.value = list
    }

    fun updateOverrideMinutes(index: Int, minutes: Int?) {
        val list = planEntries.value.toMutableList()
        list[index] = list[index].copy(entry = list[index].entry.copy(overrideMinutes = minutes))
        planEntries.value = list
    }

    fun save() {
        viewModelScope.launch {
            val id = if (isNewPlan) UUID.randomUUID().toString() else planId
            val schedule: Schedule = buildSchedule()
            val plan = PracticePlan(
                id = id,
                name = name.value.trim(),
                entries = planEntries.value.mapIndexed { i, model ->
                    model.entry.copy(planId = id, order = i)
                },
                schedule = schedule,
                clonedFromId = null,
                createdAt = Instant.now()
            )
            val conflict = planRepository.findConflictingPlan(plan)
            if (conflict != null) {
                _conflictPlan.value = conflict
                return@launch
            }
            planRepository.savePlan(plan)
            _saveComplete.emit(Unit)
        }
    }

    fun dismissConflict() { _conflictPlan.value = null }

    fun saveIgnoringConflict() {
        _conflictPlan.value = null
        viewModelScope.launch {
            val id = if (isNewPlan) UUID.randomUUID().toString() else planId
            val schedule: Schedule = buildSchedule()
            val plan = PracticePlan(
                id = id,
                name = name.value.trim(),
                entries = planEntries.value.mapIndexed { i, model ->
                    model.entry.copy(planId = id, order = i)
                },
                schedule = schedule,
                clonedFromId = null,
                createdAt = Instant.now()
            )
            planRepository.savePlan(plan)
            _saveComplete.emit(Unit)
        }
    }

    private fun buildSchedule(): Schedule = when (scheduleType.value) {
        ScheduleType.DAILY -> Schedule.Daily
        ScheduleType.EVERY_OTHER_DAY -> Schedule.EveryOtherDay(scheduleStartDate.value)
        ScheduleType.DAYS_OF_WEEK -> Schedule.DaysOfWeek(scheduleDays.value)
        ScheduleType.MANUAL -> Schedule.Manual
    }
}
