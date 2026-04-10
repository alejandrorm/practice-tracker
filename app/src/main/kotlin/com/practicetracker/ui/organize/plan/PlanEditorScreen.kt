package com.practicetracker.ui.organize.plan

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.practicetracker.domain.model.ScheduleType
import java.time.DayOfWeek

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanEditorScreen(
    planId: String,
    onNavigateBack: () -> Unit,
    onNavigateToPieceList: () -> Unit,
    viewModel: PlanEditorViewModel = hiltViewModel()
) {
    val name by viewModel.name.collectAsStateWithLifecycle()
    val scheduleType by viewModel.scheduleType.collectAsStateWithLifecycle()
    val scheduleDays by viewModel.scheduleDays.collectAsStateWithLifecycle()
    val planEntries by viewModel.planEntries.collectAsStateWithLifecycle()
    val conflictPlan by viewModel.conflictPlan.collectAsStateWithLifecycle()
    val scaleSuggestions by viewModel.scaleSuggestions.collectAsStateWithLifecycle()
    val practiceSuggestions by viewModel.practiceSuggestions.collectAsStateWithLifecycle()

    var nameError by remember { mutableStateOf(false) }
    var showPiecePicker by remember { mutableStateOf(false) }
    var suggestionsExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel.saveComplete) {
        viewModel.saveComplete.collect { onNavigateBack() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (viewModel.isNewPlan) "New Plan" else "Edit Plan") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        if (name.isBlank()) {
                            nameError = true
                        } else {
                            viewModel.save()
                        }
                    }) {
                        Icon(Icons.Default.Check, contentDescription = "Save")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = name,
                onValueChange = {
                    viewModel.name.value = it
                    nameError = false
                },
                label = { Text("Plan name *") },
                isError = nameError,
                supportingText = if (nameError) {
                    { Text("Name is required") }
                } else null,
                modifier = Modifier.fillMaxWidth()
            )

            // Schedule picker
            Text(
                text = "Schedule",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ScheduleType.values()) { st ->
                    val label = when (st) {
                        ScheduleType.DAILY -> "Daily"
                        ScheduleType.EVERY_OTHER_DAY -> "Every other day"
                        ScheduleType.DAYS_OF_WEEK -> "Days of week"
                        ScheduleType.MANUAL -> "Manual"
                    }
                    FilterChip(
                        selected = scheduleType == st,
                        onClick = { viewModel.scheduleType.value = st },
                        label = { Text(label) }
                    )
                }
            }

            // Days-of-week picker
            if (scheduleType == ScheduleType.DAYS_OF_WEEK) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    DayOfWeek.values().forEach { day ->
                        FilterChip(
                            selected = day in scheduleDays,
                            onClick = {
                                val current = scheduleDays.toMutableSet()
                                if (day in current) current.remove(day) else current.add(day)
                                viewModel.scheduleDays.value = current
                            },
                            label = { Text(day.name.take(3).lowercase().replaceFirstChar { it.uppercase() }) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Pieces section
            HorizontalDivider()
            Text(
                text = "Pieces",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            planEntries.forEachIndexed { index, model ->
                if (index > 0) HorizontalDivider()
                PlanEntryRow(
                    model = model,
                    index = index,
                    canMoveUp = index > 0,
                    canMoveDown = index < planEntries.size - 1,
                    onMoveUp = { viewModel.moveEntryUp(index) },
                    onMoveDown = { viewModel.moveEntryDown(index) },
                    onRemove = { viewModel.removeEntry(index) },
                    onOverrideMinutesChange = { minutes ->
                        viewModel.updateOverrideMinutes(index, minutes)
                    }
                )
            }

            OutlinedButton(
                onClick = { showPiecePicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Add Piece")
            }

            // Suggestions section
            HorizontalDivider()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Suggestions",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                IconButton(onClick = { suggestionsExpanded = !suggestionsExpanded }) {
                    Icon(
                        if (suggestionsExpanded) Icons.Default.KeyboardArrowUp
                        else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (suggestionsExpanded) "Collapse" else "Expand"
                    )
                }
            }

            if (suggestionsExpanded) {
                if (practiceSuggestions.isNotEmpty()) {
                    Text(
                        text = "Based on pieces in this plan — tap + to add:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    practiceSuggestions.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = item,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { viewModel.addSuggestedPractice(item) }) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add to plan",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                } else if (scaleSuggestions.isNotEmpty()) {
                    Text(
                        text = "Scale suggestions based on your pieces:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    scaleSuggestions.forEach { scaleName ->
                        Text(
                            text = "• $scaleName",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                } else {
                    Text(
                        text = "Add repertoire pieces to see practice suggestions here.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showPiecePicker) {
        PiecePickerSheet(
            onSelectPiece = { piece ->
                viewModel.addPiece(piece)
                showPiecePicker = false
            },
            onDismiss = { showPiecePicker = false }
        )
    }

    conflictPlan?.let { conflict ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissConflict() },
            title = { Text("Schedule Conflict") },
            text = {
                Text(
                    "This plan's schedule overlaps with \"${conflict.name}\". " +
                            "Do you want to save anyway?"
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.saveIgnoringConflict() }) {
                    Text("Save Anyway")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissConflict() }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun PlanEntryRow(
    model: PlanEntryUiModel,
    index: Int,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
    onOverrideMinutesChange: (Int?) -> Unit
) {
    var overrideText by remember(model.entry.overrideMinutes) {
        mutableStateOf(model.entry.overrideMinutes?.toString() ?: "")
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.padding(end = 4.dp)) {
            IconButton(onClick = onMoveUp, enabled = canMoveUp, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move up")
            }
            IconButton(onClick = onMoveDown, enabled = canMoveDown, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move down")
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            if (model.piece != null) {
                Text(
                    text = model.piece.title,
                    style = MaterialTheme.typography.bodyMedium
                )
                AssistChip(
                    onClick = {},
                    label = {
                        Text(
                            model.piece.type.name.lowercase().replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                )
            } else {
                Text(
                    text = "[Deleted piece]",
                    style = MaterialTheme.typography.bodyMedium,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        OutlinedTextField(
            value = overrideText,
            onValueChange = { text ->
                overrideText = text
                onOverrideMinutesChange(text.toIntOrNull())
            },
            label = { Text("Min") },
            modifier = Modifier.width(72.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true
        )
        IconButton(onClick = onRemove) {
            Icon(Icons.Default.Close, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
        }
    }
}
