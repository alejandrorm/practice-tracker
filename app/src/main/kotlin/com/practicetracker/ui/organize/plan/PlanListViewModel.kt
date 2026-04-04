package com.practicetracker.ui.organize.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.practicetracker.data.repository.PlanRepository
import com.practicetracker.domain.model.PracticePlan
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class PlanListViewModel @Inject constructor(
    private val planRepository: PlanRepository
) : ViewModel() {

    val plans: StateFlow<List<PracticePlan>> =
        planRepository.getAllPlans()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun deletePlan(plan: PracticePlan) {
        viewModelScope.launch { planRepository.deletePlan(plan) }
    }

    fun clonePlan(plan: PracticePlan) {
        viewModelScope.launch {
            val fixedId = UUID.randomUUID().toString()
            val cloned = plan.copy(
                id = fixedId,
                name = "${plan.name} (Copy)",
                entries = plan.entries.map {
                    it.copy(id = UUID.randomUUID().toString(), planId = fixedId)
                },
                clonedFromId = plan.id,
                createdAt = Instant.now()
            )
            planRepository.savePlan(cloned)
        }
    }
}
