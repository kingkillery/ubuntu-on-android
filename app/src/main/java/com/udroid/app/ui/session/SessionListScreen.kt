package com.udroid.app.ui.session

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionListScreen(
    onCreateSession: () -> Unit,
    onSessionClick: (sessionId: String) -> Unit,
    viewModel: SessionListViewModel = hiltViewModel()
) {
    val sessions by viewModel.sessions.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ubuntu Sessions") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateSession
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Session")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (sessions.isEmpty()) {
                EmptyState(
                    onCreateSession = onCreateSession,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(sessions, key = { it.id }) { session ->
                        SessionItem(
                            session = session,
                            onClick = { onSessionClick(session.id) },
                            onDelete = { viewModel.deleteSession(session.id) },
                            onToggle = {
                                when (session.state) {
                                    is com.udroid.app.model.SessionState.Running -> {
                                        viewModel.stopSession(session.id)
                                    }
                                    else -> {
                                        viewModel.startSession(session.id)
                                    }
                                }
                            }
                        )
                    }
                }
            }

            errorMessage?.let { error ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                ) {
                    Text(text = error)
                }
            }
        }
    }
}

@Composable
fun SessionItem(
    session: com.udroid.app.session.UbuntuSession,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onToggle: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.config.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = session.config.distro.displayName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                SessionStateBadge(state = session.state)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = onToggle) {
                    Icon(
                        if (session.state is com.udroid.app.model.SessionState.Running) {
                            Icons.Filled.Close
                        } else {
                            Icons.Filled.PlayArrow
                        },
                        contentDescription = if (session.state is com.udroid.app.model.SessionState.Running) {
                            "Stop"
                        } else {
                            "Start"
                        }
                    )
                }
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete"
                    )
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Session?") },
            text = { Text("Are you sure you want to delete ${session.config.name}?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SessionStateBadge(state: com.udroid.app.model.SessionState) {
    val (text, color) = when (state) {
        is com.udroid.app.model.SessionState.Created -> "Created" to MaterialTheme.colorScheme.outline
        is com.udroid.app.model.SessionState.Starting -> "Starting..." to MaterialTheme.colorScheme.tertiary
        is com.udroid.app.model.SessionState.Running -> "Running" to MaterialTheme.colorScheme.primary
        is com.udroid.app.model.SessionState.Stopping -> "Stopping..." to MaterialTheme.colorScheme.tertiary
        is com.udroid.app.model.SessionState.Stopped -> "Stopped" to MaterialTheme.colorScheme.outline
        is com.udroid.app.model.SessionState.Error -> "Error" to MaterialTheme.colorScheme.error
    }

    Surface(
        color = color.copy(alpha = 0.12f),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun EmptyState(
    onCreateSession: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "No Ubuntu sessions yet",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onCreateSession) {
            Text("Create Your First Session")
        }
    }
}
