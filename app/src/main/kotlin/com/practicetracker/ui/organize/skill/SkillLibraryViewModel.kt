package com.practicetracker.ui.organize.skill

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.practicetracker.data.db.dao.SkillDao
import com.practicetracker.data.db.dao.SkillWithUsageCount
import com.practicetracker.data.db.entity.SkillEntity
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
class SkillLibraryViewModel @Inject constructor(
    private val skillDao: SkillDao
) : ViewModel() {

    val skillsWithUsage: StateFlow<List<SkillWithUsageCount>> =
        skillDao.getSkillsWithUsageCount()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    val filteredSkills: StateFlow<List<SkillWithUsageCount>> = combine(skillsWithUsage, _searchQuery) { skills, query ->
        if (query.isBlank()) skills
        else skills.filter { it.label.contains(query, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setSearchQuery(q: String) { _searchQuery.value = q }

    fun renameSkill(skill: SkillWithUsageCount, newLabel: String) {
        viewModelScope.launch {
            skillDao.updateSkill(SkillEntity(skill.id, newLabel, skill.scope, skill.createdAt))
        }
    }

    fun deleteSkill(skill: SkillWithUsageCount) {
        viewModelScope.launch {
            skillDao.deleteSkill(SkillEntity(skill.id, skill.label, skill.scope, skill.createdAt))
        }
    }
}
