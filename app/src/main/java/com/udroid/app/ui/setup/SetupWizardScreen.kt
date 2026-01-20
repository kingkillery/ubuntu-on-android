package com.udroid.app.ui.setup

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.udroid.app.model.DistroVariant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupWizardScreen(
    onComplete: (sessionId: String) -> Unit,
    onBack: () -> Unit,
    viewModel: SetupWizardViewModel = hiltViewModel()
) {
    val selectedDistro by viewModel.selectedDistro.collectAsState()
    val sessionName by viewModel.sessionName.collectAsState()
    val installAgentTools by viewModel.installAgentTools.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val agentInstallProgress by viewModel.agentInstallProgress.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val setupComplete by viewModel.setupComplete.collectAsState()

    LaunchedEffect(setupComplete) {
        setupComplete?.let { sessionId ->
            onComplete(sessionId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Setup Ubuntu") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Welcome to Ubuntu on Android",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                Text(
                    text = "Choose a Ubuntu distribution to get started.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                Divider()
            }

            item {
                Text(
                    text = "Select Distribution",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            items(DistroVariant.entries) { distro ->
                DistroItem(
                    distro = distro,
                    isSelected = selectedDistro == distro,
                    onClick = { viewModel.selectDistro(distro) }
                )
            }

            item {
                Divider()
            }

            item {
                OutlinedTextField(
                    value = sessionName,
                    onValueChange = { viewModel.updateSessionName(it) },
                    label = { Text("Session Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            item {
                Divider()
            }

            item {
                Text(
                    text = "Agent Tools",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                AgentToolsOption(
                    isEnabled = installAgentTools,
                    onToggle = { viewModel.toggleAgentTools() }
                )
            }

            item {
                Divider()
            }

            item {
                if (isLoading) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        agentInstallProgress?.let { progress ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = progress,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    Button(
                        onClick = { viewModel.createSession() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Create Session")
                    }
                }

                errorMessage?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun DistroItem(
    distro: DistroVariant,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        border = if (isSelected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            null
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = distro.displayName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${distro.sizeBytes / (1024 * 1024 * 1024)} GB",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun AgentToolsOption(
    isEnabled: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
        border = if (isEnabled) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            null
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = if (isEnabled) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Install Agent Tools",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isEnabled) FontWeight.Bold else FontWeight.Normal
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "pk-puzldai, factory.ai droid, Google Gemini CLI",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Enables AI-powered task orchestration",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = isEnabled,
                onCheckedChange = { onToggle() }
            )
        }
    }
}
