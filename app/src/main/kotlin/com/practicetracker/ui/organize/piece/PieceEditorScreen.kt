package com.practicetracker.ui.organize.piece

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.practicetracker.domain.model.PieceType
import com.practicetracker.domain.model.Skill

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PieceEditorScreen(
    pieceId: String,
    onNavigateBack: () -> Unit,
    viewModel: PieceEditorViewModel = hiltViewModel()
) {
    val title by viewModel.title.collectAsStateWithLifecycle()
    val type by viewModel.type.collectAsStateWithLifecycle()
    val composer by viewModel.composer.collectAsStateWithLifecycle()
    val book by viewModel.book.collectAsStateWithLifecycle()
    val pages by viewModel.pages.collectAsStateWithLifecycle()
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val suggestedMinutes by viewModel.suggestedMinutes.collectAsStateWithLifecycle()
    val attachedSkills by viewModel.attachedSkills.collectAsStateWithLifecycle()
    val skillSearchQuery by viewModel.skillSearchQuery.collectAsStateWithLifecycle()
    val skillSearchResults by viewModel.skillSearchResults.collectAsStateWithLifecycle()
    val suggestedSkillLabels by viewModel.suggestedSkillLabels.collectAsStateWithLifecycle()

    var titleError by remember { mutableStateOf(false) }
    var showSkillDropdown by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel.saveComplete) {
        viewModel.saveComplete.collect { onNavigateBack() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (viewModel.isNewPiece) "New Piece" else "Edit Piece") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        if (title.isBlank()) {
                            titleError = true
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
                value = title,
                onValueChange = {
                    viewModel.title.value = it
                    titleError = false
                },
                label = { Text("Title *") },
                isError = titleError,
                supportingText = if (titleError) {
                    { Text("Title is required") }
                } else null,
                modifier = Modifier.fillMaxWidth()
            )

            // Type selector
            Text(
                text = "Type",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(PieceType.values()) { pieceType ->
                    FilterChip(
                        selected = type == pieceType,
                        onClick = { viewModel.type.value = pieceType },
                        label = {
                            Text(pieceType.name.lowercase().replaceFirstChar { it.uppercase() })
                        }
                    )
                }
            }

            OutlinedTextField(
                value = composer,
                onValueChange = { viewModel.composer.value = it },
                label = { Text("Composer (optional)") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = book,
                onValueChange = { viewModel.book.value = it },
                label = { Text("Book (optional)") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = pages,
                onValueChange = { viewModel.pages.value = it },
                label = { Text("Pages (optional)") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { viewModel.notes.value = it },
                label = { Text("Notes (optional)") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 4
            )

            // Suggested minutes stepper
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Suggested minutes:",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = { if (suggestedMinutes > 1) viewModel.suggestedMinutes.value = suggestedMinutes - 1 }
                ) {
                    Text("-", style = MaterialTheme.typography.titleLarge)
                }
                Text(
                    text = "$suggestedMinutes",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.widthIn(min = 32.dp),
                )
                IconButton(
                    onClick = { if (suggestedMinutes < 120) viewModel.suggestedMinutes.value = suggestedMinutes + 1 }
                ) {
                    Text("+", style = MaterialTheme.typography.titleLarge)
                }
            }

            // Skills section
            Divider()
            Text(
                text = "Skills",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            // Skill search
            ExposedDropdownMenuBox(
                expanded = showSkillDropdown && (skillSearchResults.isNotEmpty() || skillSearchQuery.isNotBlank()),
                onExpandedChange = { showSkillDropdown = it }
            ) {
                OutlinedTextField(
                    value = skillSearchQuery,
                    onValueChange = {
                        viewModel.setSkillSearchQuery(it)
                        showSkillDropdown = true
                    },
                    label = { Text("Search or add skill") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = showSkillDropdown && (skillSearchResults.isNotEmpty() || skillSearchQuery.isNotBlank()),
                    onDismissRequest = { showSkillDropdown = false }
                ) {
                    val resultsToShow = skillSearchResults.take(5)
                    resultsToShow.forEach { skill ->
                        DropdownMenuItem(
                            text = { Text(skill.label) },
                            onClick = {
                                viewModel.addSkillByLabel(skill.label)
                                showSkillDropdown = false
                            }
                        )
                    }
                    if (skillSearchQuery.isNotBlank() &&
                        skillSearchResults.none { it.label.equals(skillSearchQuery, ignoreCase = true) }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Create \"${skillSearchQuery}\"") },
                            onClick = {
                                viewModel.addSkillByLabel(skillSearchQuery)
                                showSkillDropdown = false
                            }
                        )
                    }
                }
            }

            // Suggestion chips
            if (suggestedSkillLabels.isNotEmpty()) {
                Text(
                    text = "Suggestions:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(suggestedSkillLabels) { label ->
                        SuggestionChip(
                            onClick = { viewModel.addSkillByLabel(label) },
                            label = { Text(label, style = MaterialTheme.typography.labelMedium) }
                        )
                    }
                }
            }

            // Attached skills list
            attachedSkills.forEachIndexed { index, skill ->
                if (index > 0) Divider()
                SkillRow(
                    skill = skill,
                    canMoveUp = index > 0,
                    canMoveDown = index < attachedSkills.size - 1,
                    onMoveUp = { viewModel.moveSkillUp(index) },
                    onMoveDown = { viewModel.moveSkillDown(index) },
                    onRemove = { viewModel.removeSkill(skill) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SkillRow(
    skill: Skill,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = skill.label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onMoveUp, enabled = canMoveUp) {
            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move up")
        }
        IconButton(onClick = onMoveDown, enabled = canMoveDown) {
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move down")
        }
        IconButton(onClick = onRemove) {
            Icon(Icons.Default.Close, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
        }
    }
}
