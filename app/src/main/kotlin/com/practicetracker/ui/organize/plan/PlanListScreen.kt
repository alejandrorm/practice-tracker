package com.practicetracker.ui.organize.plan

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.practicetracker.domain.model.PracticePlan
import com.practicetracker.domain.model.Schedule
import java.time.DayOfWeek

fun Schedule.toDisplayString(): String = when (this) {
    is Schedule.Daily -> "Daily"
    is Schedule.EveryOtherDay -> "Every other day"
    is Schedule.DaysOfWeek -> days.sortedBy { it.value }
        .joinToString(" / ") { it.name.take(3).lowercase().replaceFirstChar { c -> c.uppercase() } }
    is Schedule.Manual -> "Manual"
    else -> "Unknown"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanListScreen(
    onNavigateToPlanEditor: (String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: PlanListViewModel = hiltViewModel()
) {
    val plans by viewModel.plans.collectAsStateWithLifecycle()
    var planToDelete by remember { mutableStateOf<PracticePlan?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Practice Plans") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onNavigateToPlanEditor("new") }) {
                Icon(Icons.Default.Add, contentDescription = "Add plan")
            }
        }
    ) { paddingValues ->
        if (plans.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "No plans yet — tap + to create one",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(plans) { plan ->
                    PlanCard(
                        plan = plan,
                        onEdit = { onNavigateToPlanEditor(plan.id) },
                        onClone = { viewModel.clonePlan(plan) },
                        onDelete = { planToDelete = plan }
                    )
                }
            }
        }
    }

    planToDelete?.let { plan ->
        AlertDialog(
            onDismissRequest = { planToDelete = null },
            title = { Text("Delete Plan") },
            text = { Text("Delete \"${plan.name}\"? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deletePlan(plan)
                    planToDelete = null
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { planToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun PlanCard(
    plan: PracticePlan,
    onEdit: () -> Unit,
    onClone: () -> Unit,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = plan.name,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AssistChip(
                    onClick = {},
                    label = {
                        Text(
                            text = plan.schedule.toDisplayString(),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                )
                Text(
                    text = "${plan.entries.size} piece(s)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit plan")
                }
                IconButton(onClick = onClone) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Clone plan"
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete plan",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
