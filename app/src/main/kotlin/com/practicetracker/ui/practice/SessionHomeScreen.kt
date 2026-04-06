package com.practicetracker.ui.practice

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.practicetracker.domain.model.PracticePlan
import com.practicetracker.ui.organize.plan.PiecePickerSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionHomeScreen(
    onNavigateToActiveSession: (String) -> Unit,
    onNavigateToOrganize: () -> Unit,
    viewModel: SessionHomeViewModel = hiltViewModel()
) {
    val todaysPlan by viewModel.todaysPlan.collectAsStateWithLifecycle()
    val allPlans by viewModel.allPlans.collectAsStateWithLifecycle()
    val inProgressSession by viewModel.inProgressSession.collectAsStateWithLifecycle()

    var showOtherPlans by remember { mutableStateOf(false) }
    var showPiecePicker by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.startedSessionId.collect { sessionId ->
            onNavigateToActiveSession(sessionId)
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Practice") }) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── In-progress session banner ──────────────────────────────────
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
                            contentDescription = null,
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
            }

            // ── Today's plan card ───────────────────────────────────────────
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (todaysPlan != null) {
                        val plan = todaysPlan!!
                        PlanSummaryContent(
                            plan = plan,
                            label = "Today's plan",
                            onStart = { viewModel.startSession(plan) }
                        )
                    } else {
                        Text(
                            text = "No plan scheduled for today",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Pick a plan below, practice a single piece, or go to Organize to set up a schedule.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = onNavigateToOrganize,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Go to Organize")
                        }
                    }
                }
            }

            // ── Other plans ─────────────────────────────────────────────────
            val otherPlans = allPlans.filter { it.id != todaysPlan?.id }
            if (otherPlans.isNotEmpty()) {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Other plans",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = { showOtherPlans = !showOtherPlans }) {
                                Icon(
                                    imageVector = if (showOtherPlans) Icons.Filled.ExpandLess
                                                  else Icons.Filled.ExpandMore,
                                    contentDescription = if (showOtherPlans) "Collapse" else "Expand"
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(if (showOtherPlans) "Hide" else "Show ${otherPlans.size}")
                            }
                        }

                        AnimatedVisibility(visible = showOtherPlans) {
                            Column {
                                HorizontalDivider()
                                otherPlans.forEachIndexed { index, plan ->
                                    if (index > 0) HorizontalDivider()
                                    Column(modifier = Modifier.padding(vertical = 12.dp)) {
                                        PlanSummaryContent(
                                            plan = plan,
                                            label = null,
                                            onStart = { viewModel.startSession(plan) }
                                        )
                                    }
                                }
                            }
                        }
                        // Add bottom spacing when collapsed
                        if (!showOtherPlans) Spacer(Modifier.height(4.dp))
                    }
                }
            }

            // ── Practice a single piece ─────────────────────────────────────
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Practice a single piece",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Pick any piece from your library",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(onClick = { showPiecePicker = true }) {
                        Text("Pick piece")
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }

    if (showPiecePicker) {
        PiecePickerSheet(
            onSelectPiece = { piece ->
                showPiecePicker = false
                viewModel.startSessionWithPiece(piece)
            },
            onDismiss = { showPiecePicker = false }
        )
    }
}

@Composable
private fun PlanSummaryContent(
    plan: PracticePlan,
    label: String?,
    onStart: () -> Unit
) {
    if (label != null) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontStyle = FontStyle.Italic
        )
        Spacer(Modifier.height(2.dp))
    }
    Text(text = plan.name, style = MaterialTheme.typography.headlineSmall)
    Spacer(Modifier.height(8.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Filled.Schedule,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(4.dp))
        val totalMinutes = plan.entries.sumOf { it.overrideMinutes ?: 0 }
        Text(
            text = if (totalMinutes > 0) "$totalMinutes min suggested" else "No time set",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(12.dp))
        Icon(
            imageVector = Icons.Filled.LibraryMusic,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = "${plan.entries.size} piece${if (plan.entries.size != 1) "s" else ""}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    Spacer(Modifier.height(12.dp))
    Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
        Text("Start Session")
    }
}
