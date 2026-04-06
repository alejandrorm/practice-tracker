package com.practicetracker.ui.organize.plan

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.practicetracker.domain.model.Piece

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PiecePickerSheet(
    onSelectPiece: (Piece) -> Unit,
    onDismiss: () -> Unit,
    viewModel: PiecePickerViewModel = hiltViewModel()
) {
    val filteredPieces by viewModel.filteredPieces.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Add Piece",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            OutlinedTextField(
                value = searchQuery,
                onValueChange = viewModel::setSearchQuery,
                label = { Text("Search pieces") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                items(filteredPieces) { piece ->
                    ListItem(
                        headlineContent = { Text(piece.title) },
                        supportingContent = {
                            Text(
                                piece.type.name.lowercase().replaceFirstChar { it.uppercase() } +
                                        (piece.composer?.let { " · $it" } ?: "")
                            )
                        },
                        modifier = Modifier.clickable {
                            onSelectPiece(piece)
                            onDismiss()
                        }
                    )
                    HorizontalDivider()
                }

                if (filteredPieces.isEmpty() && searchQuery.isNotBlank()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No pieces matching \"$searchQuery\"",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
