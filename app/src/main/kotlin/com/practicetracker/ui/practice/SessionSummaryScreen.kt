package com.practicetracker.ui.practice

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize

private fun formatTimeSummary(totalSeconds: Long): String {
    val m = totalSeconds / 60
    val s = totalSeconds % 60
    return "%02d:%02d".format(m, s)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionSummaryScreen(
    sessionId: String,
    onDone: () -> Unit,
    viewModel: SessionSummaryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Session Complete") })
        }
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(8.dp))

            // Total time hero card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Filled.EmojiEvents,
                        contentDescription = "Trophy",
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = formatTimeSummary(uiState.totalDurationSeconds),
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Total practice time",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    if (uiState.allPiecesCompleted) {
                        Spacer(Modifier.height(8.dp))
                        AssistChip(
                            onClick = {},
                            label = { Text("Perfect Session!") },
                            leadingIcon = {
                                Icon(
                                    Icons.Filled.Star,
                                    contentDescription = "Star",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(
                text = "Breakdown",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(8.dp))

            uiState.entryRows.forEach { row ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    ListItem(
                        headlineContent = {
                            Text(row.pieceTitle, style = MaterialTheme.typography.titleSmall)
                        },
                        supportingContent = {
                            if (row.isSkipped) {
                                Text(
                                    text = "Skipped",
                                    color = MaterialTheme.colorScheme.outline
                                )
                            } else {
                                Column {
                                    Text(formatTimeSummary(row.durationSeconds))
                                    if (row.checkedSkillLabels.isNotEmpty()) {
                                        Text(
                                            text = "\u2713 ${row.checkedSkillLabels.joinToString(", ")}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        },
                        leadingContent = {
                            if (row.isSkipped) {
                                Icon(
                                    imageVector = Icons.Filled.Cancel,
                                    contentDescription = "Skipped",
                                    tint = MaterialTheme.colorScheme.outline
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = "Completed",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onDone,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Done")
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
