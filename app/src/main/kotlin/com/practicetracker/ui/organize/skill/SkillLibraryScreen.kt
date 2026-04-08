package com.practicetracker.ui.organize.skill

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Tune
import com.practicetracker.ui.common.EmptyState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.practicetracker.data.db.dao.SkillWithUsageCount

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkillLibraryScreen(
    onNavigateBack: () -> Unit,
    viewModel: SkillLibraryViewModel = hiltViewModel()
) {
    val filteredSkills by viewModel.filteredSkills.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    var skillToRename by remember { mutableStateOf<SkillWithUsageCount?>(null) }
    var skillToDelete by remember { mutableStateOf<SkillWithUsageCount?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Skill Library") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = viewModel::setSearchQuery,
                label = { Text("Search skills") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true
            )

            if (filteredSkills.isEmpty()) {
                EmptyState(
                    icon = Icons.Filled.Tune,
                    title = if (searchQuery.isNotBlank()) "No skills match \"$searchQuery\"" else "No skills yet",
                    subtitle = if (searchQuery.isBlank()) "Skills are created when you add them to pieces." else null
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filteredSkills) { skill ->
                        SkillRow(
                            skill = skill,
                            onRename = { skillToRename = skill },
                            onDelete = { skillToDelete = skill }
                        )
                    }
                }
            }
        }
    }

    // Rename dialog
    skillToRename?.let { skill ->
        var newLabel by remember(skill) { mutableStateOf(skill.label) }
        AlertDialog(
            onDismissRequest = { skillToRename = null },
            title = { Text("Rename Skill") },
            text = {
                OutlinedTextField(
                    value = newLabel,
                    onValueChange = { newLabel = it },
                    label = { Text("Skill name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newLabel.isNotBlank()) {
                            viewModel.renameSkill(skill, newLabel.trim())
                        }
                        skillToRename = null
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { skillToRename = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete dialog
    skillToDelete?.let { skill ->
        AlertDialog(
            onDismissRequest = { skillToDelete = null },
            title = { Text("Delete Skill") },
            text = {
                if (skill.usageCount > 0) {
                    Text(
                        "\"${skill.label}\" is used in ${skill.usageCount} piece(s). " +
                                "Removing it will detach it from all pieces."
                    )
                } else {
                    Text("Delete \"${skill.label}\"? This cannot be undone.")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteSkill(skill)
                        skillToDelete = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { skillToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SkillRow(
    skill: SkillWithUsageCount,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = skill.label,
                    style = MaterialTheme.typography.titleSmall
                )
                AssistChip(
                    onClick = {},
                    label = {
                        Text(
                            "Used in ${skill.usageCount} piece(s)",
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            IconButton(onClick = onRename) {
                Icon(Icons.Default.Edit, contentDescription = "Rename")
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
