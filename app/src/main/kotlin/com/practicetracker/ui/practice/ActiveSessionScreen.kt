package com.practicetracker.ui.practice

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Remove
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
import androidx.compose.material3.Slider
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

private fun formatTime(totalSeconds: Long): String {
    val m = totalSeconds / 60
    val s = totalSeconds % 60
    return "%02d:%02d".format(m, s)
}

@Composable
fun ActiveSessionScreen(
    sessionId: String,
    onSessionEnded: (String) -> Unit,
    viewModel: ActiveSessionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showEndConfirmDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.sessionEnded.collect { id -> onSessionEnded(id) }
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
            title = { Text("End session?") },
            text = { Text("Are you sure you want to end the session?") },
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

    if (uiState.selectedPieceIndex == null) {
        SessionOverviewContent(
            uiState = uiState,
            onSelectPiece = viewModel::selectPiece,
            onTogglePause = viewModel::togglePause,
            onEndSession = { showEndConfirmDialog = true },
            onToggleMetronome = viewModel::toggleMetronome,
            onMetronomeBpmChange = viewModel::setMetronomeBpm,
            onMetronomeBeatsChange = viewModel::setMetronomeBeatsPerMeasure
        )
    } else {
        BackHandler { viewModel.returnToOverview() }
        PiecePracticeContent(
            uiState = uiState,
            onBack = viewModel::returnToOverview,
            onTogglePause = viewModel::togglePause,
            onCheckSkill = viewModel::checkSkill,
            onUncheckSkill = viewModel::uncheckSkill,
            onDone = viewModel::donePiece,
            onSkip = viewModel::skipPiece,
            onEndSession = { showEndConfirmDialog = true },
            onToggleMetronome = viewModel::toggleMetronome,
            onMetronomeBpmChange = viewModel::setMetronomeBpm,
            onMetronomeBeatsChange = viewModel::setMetronomeBeatsPerMeasure
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SessionOverviewContent(
    uiState: ActiveSessionUiState,
    onSelectPiece: (Int) -> Unit,
    onTogglePause: () -> Unit,
    onEndSession: () -> Unit,
    onToggleMetronome: () -> Unit,
    onMetronomeBpmChange: (Int) -> Unit,
    onMetronomeBeatsChange: (Int) -> Unit
) {
    var showMetronomeSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    if (showMetronomeSheet) {
        ModalBottomSheet(
            onDismissRequest = { showMetronomeSheet = false },
            sheetState = sheetState
        ) {
            MetronomeSheetContent(
                metronome = uiState.metronome,
                onToggle = onToggleMetronome,
                onBpmChange = onMetronomeBpmChange,
                onBeatsChange = onMetronomeBeatsChange,
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.planName ?: "Session") },
                actions = {
                    IconButton(onClick = { showMetronomeSheet = true }) {
                        Icon(
                            imageVector = Icons.Filled.MusicNote,
                            contentDescription = "Metronome",
                            tint = if (uiState.metronome.isRunning) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onTogglePause) {
                        Icon(
                            imageVector = if (uiState.isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                            contentDescription = if (uiState.isPaused) "Resume" else "Pause"
                        )
                    }
                    IconButton(onClick = onEndSession) {
                        Icon(Icons.Filled.Stop, contentDescription = "End session")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatTime(uiState.sessionElapsedSeconds),
                        style = MaterialTheme.typography.displaySmall
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "total",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                HorizontalDivider()
            }

            itemsIndexed(uiState.entries) { index, entry ->
                ListItem(
                    headlineContent = {
                        Text(entry.pieceTitle, style = MaterialTheme.typography.bodyLarge)
                    },
                    supportingContent = {
                        if (entry.elapsedSeconds > 0) {
                            Text(
                                text = formatTime(entry.elapsedSeconds),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
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
                            entry.isStarted -> Icon(
                                Icons.Filled.PlayArrow,
                                contentDescription = "In progress",
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            else -> Icon(
                                Icons.Filled.RadioButtonUnchecked,
                                contentDescription = "Not started",
                                tint = MaterialTheme.colorScheme.outline
                            )
                        }
                    },
                    trailingContent = {
                        Icon(
                            Icons.Filled.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    modifier = Modifier.clickable { onSelectPiece(index) }
                )
                HorizontalDivider()
            }

            item {
                Spacer(Modifier.height(16.dp))
                OutlinedButton(
                    onClick = onEndSession,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Icon(Icons.Filled.Stop, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("End Session")
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PiecePracticeContent(
    uiState: ActiveSessionUiState,
    onBack: () -> Unit,
    onTogglePause: () -> Unit,
    onCheckSkill: (String) -> Unit,
    onUncheckSkill: (String) -> Unit,
    onDone: () -> Unit,
    onSkip: () -> Unit,
    onEndSession: () -> Unit,
    onToggleMetronome: () -> Unit,
    onMetronomeBpmChange: (Int) -> Unit,
    onMetronomeBeatsChange: (Int) -> Unit
) {
    val currentEntry = uiState.entries.getOrNull(uiState.selectedPieceIndex ?: return)
        ?: return

    var showMetronomeSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    if (showMetronomeSheet) {
        ModalBottomSheet(
            onDismissRequest = { showMetronomeSheet = false },
            sheetState = sheetState
        ) {
            MetronomeSheetContent(
                metronome = uiState.metronome,
                onToggle = onToggleMetronome,
                onBpmChange = onMetronomeBpmChange,
                onBeatsChange = onMetronomeBeatsChange,
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.planName ?: "Session") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to overview")
                    }
                },
                actions = {
                    IconButton(onClick = { showMetronomeSheet = true }) {
                        Icon(
                            imageVector = Icons.Filled.MusicNote,
                            contentDescription = "Metronome",
                            tint = if (uiState.metronome.isRunning) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onTogglePause) {
                        Icon(
                            imageVector = if (uiState.isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                            contentDescription = if (uiState.isPaused) "Resume" else "Pause"
                        )
                    }
                    IconButton(onClick = onEndSession) {
                        Icon(Icons.Filled.Stop, contentDescription = "End session")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
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
                                currentEntry.composer?.let { LabeledRow("Composer", it) }
                                currentEntry.book?.let { LabeledRow("Book", it) }
                                currentEntry.pages?.let { LabeledRow("Pages", it) }
                                currentEntry.notes?.let { LabeledRow("Notes", it) }
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
                                        if (checked) onCheckSkill(skill.id)
                                        else onUncheckSkill(skill.id)
                                    }
                                )
                                Text(skill.label, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick = onSkip,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Skip")
                        }
                        Button(
                            onClick = onDone,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Done")
                        }
                    }
                }
            }

        }
    }
}

@Composable
private fun MetronomeSheetContent(
    metronome: MetronomeUiState,
    onToggle: () -> Unit,
    onBpmChange: (Int) -> Unit,
    onBeatsChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Metronome",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onToggle) {
                    Icon(
                        imageVector = if (metronome.isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (metronome.isRunning) "Stop metronome" else "Start metronome",
                        tint = if (metronome.isRunning) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Beat indicator dots
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
                repeat(metronome.beatsPerMeasure) { index ->
                    val isActive = metronome.isRunning && index == metronome.currentBeat
                    val isAccent = index == 0
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (isAccent) 14.dp else 10.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isActive && isAccent -> MaterialTheme.colorScheme.primary
                                    isActive -> MaterialTheme.colorScheme.secondary
                                    isAccent -> MaterialTheme.colorScheme.primaryContainer
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                }
                            )
                    )
                }
            }

            // BPM display and slider
            Text(
                text = "${metronome.bpm} BPM",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Slider(
                value = metronome.bpm.toFloat(),
                onValueChange = { onBpmChange(it.toInt()) },
                valueRange = MetronomeManager.BPM_MIN.toFloat()..MetronomeManager.BPM_MAX.toFloat(),
                modifier = Modifier.fillMaxWidth()
            )

            // Fine BPM adjustment
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = { onBpmChange(metronome.bpm - 5) }) {
                    Icon(Icons.Filled.Remove, contentDescription = "-5 BPM")
                }
                Text(
                    text = "-5",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(16.dp))
                IconButton(onClick = { onBpmChange(metronome.bpm - 1) }) {
                    Icon(Icons.Filled.Remove, contentDescription = "-1 BPM")
                }
                Text(
                    text = "-1",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "+1",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                IconButton(onClick = { onBpmChange(metronome.bpm + 1) }) {
                    Icon(Icons.Filled.Add, contentDescription = "+1 BPM")
                }
                Spacer(Modifier.width(16.dp))
                Text(
                    text = "+5",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                IconButton(onClick = { onBpmChange(metronome.bpm + 5) }) {
                    Icon(Icons.Filled.Add, contentDescription = "+5 BPM")
                }
            }

            // Beats per measure
            Spacer(Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Beats/measure",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = { onBeatsChange(metronome.beatsPerMeasure - 1) },
                    enabled = metronome.beatsPerMeasure > 1
                ) {
                    Icon(Icons.Filled.Remove, contentDescription = "Fewer beats")
                }
                Text(
                    text = metronome.beatsPerMeasure.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(min = 32.dp)
                )
                IconButton(
                    onClick = { onBeatsChange(metronome.beatsPerMeasure + 1) },
                    enabled = metronome.beatsPerMeasure < 16
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "More beats")
                }
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
