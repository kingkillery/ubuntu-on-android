package com.udroid.app.ui.agent

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentTaskScreen(
    sessionId: String,
    onBack: () -> Unit,
    viewModel: AgentTaskViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    // Auto-scroll when output changes
    LaunchedEffect(uiState.currentOutput) {
        if (uiState.currentOutput.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Agent Tasks",
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (uiState.sessionName.isNotEmpty()) {
                            Text(
                                text = uiState.sessionName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (uiState.currentOutput.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearOutput() }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Clear Output"
                            )
                        }
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
            // Check if agent is installed
            when {
                uiState.isCheckingInstall -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                !uiState.isAgentInstalled -> {
                    AgentNotInstalledContent(
                        onInstall = { viewModel.installAgentTools() },
                        isInstalling = uiState.isRunning,
                        output = uiState.currentOutput
                    )
                }
                else -> {
                    AgentTaskContent(
                        uiState = uiState,
                        listState = listState,
                        onTaskInputChange = { viewModel.updateTaskInput(it) },
                        onSelectTool = { viewModel.selectTool(it) },
                        onRunTask = { viewModel.runTask() }
                    )
                }
            }
        }
    }

    // Error snackbar
    uiState.errorMessage?.let { error ->
        LaunchedEffect(error) {
            kotlinx.coroutines.delay(3000)
            viewModel.clearError()
        }
    }
}

@Composable
fun AgentNotInstalledContent(
    onInstall: () -> Unit,
    isInstalling: Boolean,
    output: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (isInstalling) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Installing agent tools...",
                style = MaterialTheme.typography.bodyLarge
            )
            if (output.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF1E1E1E))
                            .padding(8.dp)
                    ) {
                        item {
                            Text(
                                text = output,
                                style = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    color = Color(0xFFD4D4D4)
                                )
                            )
                        }
                    }
                }
            }
        } else {
            Icon(
                Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Agent Tools Not Installed",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Install pk-puzldai, droid, and Gemini CLI to run agent tasks",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onInstall) {
                Text("Install Agent Tools")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentTaskContent(
    uiState: AgentTaskUiState,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onTaskInputChange: (String) -> Unit,
    onSelectTool: (AgentTool) -> Unit,
    onRunTask: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Tool selector
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(AgentTool.entries) { tool ->
                FilterChip(
                    selected = uiState.selectedTool == tool,
                    onClick = { onSelectTool(tool) },
                    label = { Text(tool.displayName) }
                )
            }
        }

        // Output area
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF1E1E1E))
                    .padding(8.dp),
                reverseLayout = true
            ) {
                if (uiState.currentOutput.isNotEmpty()) {
                    item {
                        Text(
                            text = uiState.currentOutput,
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                color = Color(0xFFD4D4D4)
                            )
                        )
                    }
                } else {
                    item {
                        Text(
                            text = "Enter a task below and press Run",
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                color = Color(0xFF808080)
                            )
                        )
                    }
                }
            }
        }

        // Input area
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = uiState.taskInput,
                    onValueChange = onTaskInputChange,
                    enabled = !uiState.isRunning,
                    textStyle = TextStyle(
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { onRunTask() }),
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    decorationBox = { innerTextField ->
                        Box {
                            if (uiState.taskInput.isEmpty()) {
                                Text(
                                    text = when (uiState.selectedTool) {
                                        AgentTool.AGENT_RUN -> "Describe a task to run..."
                                        AgentTool.GEMINI -> "Ask Gemini a question..."
                                        AgentTool.DROID -> "Enter a droid command..."
                                    },
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            innerTextField()
                        }
                    }
                )

                if (uiState.isRunning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    IconButton(
                        onClick = onRunTask,
                        enabled = uiState.taskInput.isNotBlank()
                    ) {
                        Icon(
                            Icons.Filled.Send,
                            contentDescription = "Run Task",
                            tint = if (uiState.taskInput.isNotBlank())
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
