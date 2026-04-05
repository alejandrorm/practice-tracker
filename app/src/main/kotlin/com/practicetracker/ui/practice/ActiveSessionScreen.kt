package com.practicetracker.ui.practice

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

private fun formatTime(totalSeconds: Long): String {
    val m = totalSeconds / 60
    val s = totalSeconds % 60
    return "%02d:%02d".format(m, s)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveSessionScreen(
    sessionId: String,
    onSessionEnded: (String) -> Unit,
    viewModel: ActiveSessionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showEndConfirmDialog by remember { mutableStateOf(false) }
    var showQueue by remember { mutableStateOf(false) }
    var showJumpConfirm by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(Unit) {
        viewModel.sessionEnded.collect { id ->
            onSessionEnded(id)
        }
    }

    if (uiState.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (showEndConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showEndConfirmDialog = false },
            title = { Text("End session early?") },
            text = { Text("Are you sure you want to end the session now?") },
            confirmButton = {
                Button(onClick = {
                    showEndConfirmDialog = false
                    viewModel.endSession()
                }) { Text("End Session") }
            },
            dismissButton = {
                TextButton(onClick = { showEndConfirmDialog = false }) { Text("Cancel") }
            }
        )
    }

    val jumpIndex = showJumpConfirm
    if (jumpIndex != null && jumpIndex < uiState.entries.size) {
        val targetTitle = uiState.entries[jumpIndex].pieceTitle
        AlertDialog(
            onDismissRequest = { showJumpConfirm = null },
            title = { Text("Jump to piece?") },
            text = { Text("Complete current piece and jump to \"$targetTitle\"?") },
            confirmButton = {
                Button(onClick = {
                    showJumpConfirm = null
                    viewModel.jumpToPiece(jumpIndex)
                }) { Text("Jump") }
            },
            dismissButton = {
                TextButton(onClick = { showJumpConfirm = null }) { Text("Cancel") }
            }
        )
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Session Header Card
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = formatTime(uiState.sessionElapsedSeconds),
                            style = MaterialTheme.typography.displaySmall
                        )
                        Text(
                            text = "Total time",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${uiState.currentPieceIndex + 1} / ${uiState.entries.size}",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "Pieces",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Column {
                        IconButton(onClick = { viewModel.togglePause() }) {
                            Icon(
                                imageVector = if (uiState.isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                                contentDescription = if (uiState.isPaused) "Resume" else "Pause"
                            )
                        }
                        IconButton(onClick = { showEndConfirmDialog = true }) {
                            Icon(
                                imageVector = Icons.Filled.Stop,
                                contentDescription = "End session"
                            )
                        }
                    }
                }
            }

            // Current Piece content
            val currentEntry = uiState.entries.getOrNull(uiState.currentPieceIndex)

            if (currentEntry != null) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                ) {
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (currentEntry.pieceType.isNotEmpty()) {
                                    SuggestionChip(
                                        onClick = {},
                                        label = { Text(currentEntry.pieceType) }
                                    )
                                    Spacer(Modifier.width(8.dp))
                                }
                                Spacer(Modifier.weight(1f))
                                if (currentEntry.suggestedMinutes > 0) {
                                    SuggestionChip(
                                        onClick = {},
                                        label = { Text("${currentEntry.suggestedMinutes} min") }
                                    )
                                }
                            }

                            Text(
                                text = currentEntry.pieceTitle,
                                style = MaterialTheme.typography.headlineSmall
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = formatTime(currentEntry.elapsedSeconds),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )

                            // Expandable metadata
                            val hasMetadata = currentEntry.composer != null ||
                                currentEntry.book != null ||
                                currentEntry.pages != null ||
                                currentEntry.notes != null
                            if (hasMetadata) {
                                var expanded by remember { mutableStateOf(false) }
                                TextButton(onClick = { expanded = !expanded }) {
                                    Text(if (expanded) "Hide details" else "Show details")
                                }
                                AnimatedVisibility(visible = expanded) {
                                    Column {
                                        currentEntry.composer?.let {
                                            LabeledRow("Composer", it)
                                        }
                                        currentEntry.book?.let {
                                            LabeledRow("Book", it)
                                        }
                                        currentEntry.pages?.let {
                                            LabeledRow("Pages", it)
                                        }
                                        currentEntry.notes?.let {
                                            LabeledRow("Notes", it)
                                        }
                                    }
                                }
                            }

                            HorizontalDivider(Modifier.padding(vertical = 8.dp))

                            Text(
                                text = "Focus on:",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(4.dp))
                            if (currentEntry.skills.isEmpty()) {
                                Text(
                                    text = "No skills defined for this piece",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                currentEntry.skills.forEach { skill ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(
                                            checked = skill.id in currentEntry.checkedSkillIds,
                                            onCheckedChange = { checked ->
                                                if (checked) viewModel.checkSkill(skill.id)
                                                else viewModel.uncheckSkill(skill.id)
                                            }
                                        )
                                        Text(skill.label, style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }

                            Spacer(Modifier.height(16.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedButton(
                                    onClick = { viewModel.skipPiece() },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Skip")
                                }
                                Button(
                                    onClick = { viewModel.donePiece() },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Done")
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }

            // Queue trigger at bottom
            val remaining = uiState.entries.size - uiState.currentPieceIndex - 1
            TextButton(
                onClick = { showQueue = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Text("$remaining remaining  \u2191")
            }
        }
    }

    // Bottom sheet for piece queue
    if (showQueue) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { showQueue = false },
            sheetState = sheetState
        ) {
            Text(
                text = "Practice Queue",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(16.dp)
            )
            LazyColumn {
                itemsIndexed(uiState.entries) { index, entry ->
                    ListItem(
                        headlineContent = { Text(entry.pieceTitle) },
                        leadingContent = {
                            when {
                                entry.isDone -> Icon(
                                    Icons.Filled.CheckCircle,
                                    contentDescription = "Done",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                entry.isSkipped -> Icon(
                                    Icons.Filled.Cancel,
                                    contentDescription = "Skipped",
                                    tint = MaterialTheme.colorScheme.outline
                                )
                                index == uiState.currentPieceIndex -> Icon(
                                    Icons.Filled.PlayArrow,
                                    contentDescription = "Current",
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                                else -> Icon(
                                    Icons.Filled.RadioButtonUnchecked,
                                    contentDescription = "Upcoming",
                                    tint = MaterialTheme.colorScheme.outline
                                )
                            }
                        },
                        trailingContent = {
                            if (index > uiState.currentPieceIndex && !entry.isDone && !entry.isSkipped) {
                                TextButton(onClick = {
                                    showQueue = false
                                    showJumpConfirm = index
                                }) {
                                    Text("Jump")
                                }
                            }
                        }
                    )
                    HorizontalDivider()
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun LabeledRow(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall
        )
    }
}
