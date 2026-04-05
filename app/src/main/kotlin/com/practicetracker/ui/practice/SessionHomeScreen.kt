package com.practicetracker.ui.practice

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionHomeScreen(
    onNavigateToActiveSession: (String) -> Unit,
    onNavigateToOrganize: () -> Unit,
    viewModel: SessionHomeViewModel = hiltViewModel()
) {
    val todaysPlan by viewModel.todaysPlan.collectAsStateWithLifecycle()
    val inProgressSession by viewModel.inProgressSession.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.startedSessionId.collect { sessionId ->
            onNavigateToActiveSession(sessionId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Practice") })
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // In-progress session banner
            if (inProgressSession != null) {
                val session = inProgressSession!!
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = "Warning",
                            tint = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Session in progress",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                        Button(onClick = { onNavigateToActiveSession(session.id) }) {
                            Text("Resume")
                        }
                        Spacer(Modifier.width(4.dp))
                        TextButton(onClick = { viewModel.discardInProgressSession() }) {
                            Text("Discard")
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // Today's plan card
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (todaysPlan != null) {
                        val plan = todaysPlan!!
                        val totalMinutes = plan.entries.sumOf { it.overrideMinutes ?: 0 }
                        Text(
                            text = plan.name,
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Schedule,
                                contentDescription = "Time",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "$totalMinutes min suggested",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.LibraryMusic,
                                contentDescription = "Pieces",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "${plan.entries.size} pieces",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.startSession(plan) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Start Session")
                        }
                    } else {
                        Text(
                            text = "No plan scheduled for today",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Go to Organize to set up a practice plan, or start a free session.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(16.dp))
                        Row {
                            OutlinedButton(onClick = onNavigateToOrganize) {
                                Text("Go to Organize")
                            }
                            Spacer(Modifier.width(8.dp))
                            Button(onClick = { viewModel.startSession(null) }) {
                                Text("Free Session")
                            }
                        }
                    }
                }
            }
        }
    }
}
