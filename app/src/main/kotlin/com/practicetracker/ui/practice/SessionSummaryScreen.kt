package com.practicetracker.ui.practice

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.practicetracker.ui.achievements.BadgeRegistry
import com.practicetracker.ui.achievements.BadgeShareHelper

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
    onNavigateToAchievements: (() -> Unit)? = null,
    viewModel: SessionSummaryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val newlyEarned by viewModel.newlyEarned.collectAsStateWithLifecycle()
    val earnedAchievements by viewModel.earnedAchievements.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showBadgeOverlay by remember { mutableStateOf(true) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Session Complete") }) }
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
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
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Filled.EmojiEvents,
                        contentDescription = null,
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
                                Icon(Icons.Filled.Star, contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary)
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("Breakdown", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            uiState.entryRows.forEach { row ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    ListItem(
                        headlineContent = {
                            Text(row.pieceTitle, style = MaterialTheme.typography.titleSmall)
                        },
                        supportingContent = {
                            if (row.isSkipped) {
                                Text("Skipped", color = MaterialTheme.colorScheme.outline)
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
                                Icon(Icons.Filled.Cancel, contentDescription = "Skipped",
                                    tint = MaterialTheme.colorScheme.outline)
                            } else {
                                Icon(Icons.Filled.CheckCircle, contentDescription = "Completed",
                                    tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("Done") }
            if (onNavigateToAchievements != null) {
                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = onNavigateToAchievements,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("View All Achievements") }
            }
            Spacer(Modifier.height(16.dp))
        }

        // Badge unlock overlay
        AnimatedVisibility(
            visible = newlyEarned.isNotEmpty() && showBadgeOverlay,
            enter = fadeIn() + scaleIn(initialScale = 0.85f),
            exit = fadeOut() + scaleOut(targetScale = 0.85f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.72f))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (newlyEarned.size == 1) "Achievement unlocked!" else "Achievements unlocked!",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(16.dp))

                        newlyEarned.forEach { milestone ->
                            val badge = BadgeRegistry.get(milestone)
                            val achievement = earnedAchievements[milestone]
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = badge.icon,
                                    contentDescription = null,
                                    tint = badge.color,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = milestone.displayName,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                if (achievement != null) {
                                    IconButton(onClick = {
                                        BadgeShareHelper.share(
                                            context, milestone,
                                            achievement.earnedAt, userProfile
                                        )
                                    }) {
                                        Icon(
                                            Icons.Filled.Share,
                                            contentDescription = "Share",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showBadgeOverlay = false },
                                modifier = Modifier.weight(1f)
                            ) { Text("Dismiss") }
                            if (onNavigateToAchievements != null) {
                                Button(
                                    onClick = {
                                        showBadgeOverlay = false
                                        onNavigateToAchievements()
                                    },
                                    modifier = Modifier.weight(1f)
                                ) { Text("View All") }
                            }
                        }
                    }
                }
            }
        }
    }
}
