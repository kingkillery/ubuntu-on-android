package com.udroid.app.ui.puzldai

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.udroid.app.ui.tui.*

/**
 * PK-Puzldai TUI Screen - Main screen for the pk-puzldai integration.
 * Shows the TUI banner by default and provides a terminal-style chat interface.
 *
 * @param sessionId The Ubuntu session ID to connect to
 * @param onBack Navigation callback for back button
 * @param viewModel The ViewModel (injected by Hilt)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PkPuzldaiScreen(
    sessionId: String,
    onBack: () -> Unit,
    viewModel: PkPuzldaiViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "PK-Puzld",
                            fontFamily = FontFamily.Monospace,
                            color = TuiColors.Red
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "● ${uiState.currentAgent}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = TuiColors.AgentReady
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = TuiColors.Background,
                    titleContentColor = TuiColors.Foreground,
                    navigationIconContentColor = TuiColors.Foreground
                )
            )
        },
        containerColor = TuiColors.Background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                // Checking installation status
                uiState.isCheckingInstall -> {
                    LoadingState(message = "Checking pk-puzldai installation...")
                }

                // Session not running
                !uiState.isSessionRunning -> {
                    SessionNotRunningState()
                }

                // pk-puzldai not installed
                !uiState.isPuzldaiInstalled -> {
                    InstallPromptState(
                        isInstalling = uiState.isLoading,
                        onInstall = { viewModel.installPuzldai() }
                    )
                }

                // Ready - show TUI
                else -> {
                    TuiChatScreen(
                        messages = uiState.messages,
                        version = uiState.version,
                        showBanner = uiState.showBanner && uiState.messages.isEmpty(),
                        minimalBanner = uiState.messages.isNotEmpty(),
                        agents = uiState.agents,
                        agentName = uiState.currentAgent,
                        tokenCount = uiState.tokenCount,
                        mcpStatus = uiState.mcpStatus,
                        isLoading = uiState.isLoading,
                        loadingText = uiState.loadingText,
                        inputValue = uiState.inputText,
                        onInputChange = { viewModel.updateInput(it) },
                        onSubmit = { viewModel.submitInput(it) },
                        inputEnabled = !uiState.isLoading,
                        history = uiState.commandHistory,
                        showMetadata = true,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Error snackbar
            if (uiState.errorMessage != null) {
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    action = {
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("Dismiss", color = TuiColors.Foreground)
                        }
                    },
                    containerColor = TuiColors.Error.copy(alpha = 0.9f)
                ) {
                    Text(
                        text = uiState.errorMessage ?: "",
                        color = TuiColors.White,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingState(
    message: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            color = TuiColors.Info,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            color = TuiColors.ForegroundDim,
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun SessionNotRunningState(
    modifier: Modifier = Modifier
) {
    TuiEmptyScreen(
        title = "Session Not Running",
        message = "Start the Ubuntu session to use pk-puzldai.\n\nReturn to the session list and start the session first.",
        modifier = modifier
    )
}

@Composable
private fun InstallPromptState(
    isInstalling: Boolean,
    onInstall: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TuiColors.Background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Mini banner
        TuiBanner(
            version = "0.1.0",
            minimal = true
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Install prompt box
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(TuiColors.Surface)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "pk-puzldai not installed",
                color = TuiColors.Warning,
                fontFamily = FontFamily.Monospace,
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = """
                    pk-puzldai is a multi-LLM orchestrator that provides:

                    • Multi-agent chat (Claude, Gemini, Ollama)
                    • Agentic task execution
                    • Workflow automation
                    • Code analysis and generation

                    Install now to get started.
                """.trimIndent(),
                color = TuiColors.ForegroundDim,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (isInstalling) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        color = TuiColors.Info,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Installing...",
                        color = TuiColors.Info,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp
                    )
                }
            } else {
                Button(
                    onClick = onInstall,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TuiColors.Red,
                        contentColor = TuiColors.White
                    )
                ) {
                    Text(
                        text = "Install pk-puzldai",
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "This will install Node.js and the pk-puzldai CLI",
            color = TuiColors.ForegroundMuted,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp
        )
    }
}
