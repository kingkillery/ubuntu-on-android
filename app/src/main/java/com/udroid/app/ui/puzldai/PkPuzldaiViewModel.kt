package com.udroid.app.ui.puzldai

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.udroid.app.agent.AgentManager
import com.udroid.app.model.SessionState
import com.udroid.app.session.UbuntuSessionManager
import com.udroid.app.ui.tui.AgentStatus
import com.udroid.app.ui.tui.McpStatus
import com.udroid.app.ui.tui.MessageRole
import com.udroid.app.ui.tui.TuiMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

/**
 * UI state for the PK-Puzldai TUI screen.
 */
data class PkPuzldaiUiState(
    val sessionId: String = "",
    val sessionName: String = "",
    val isSessionRunning: Boolean = false,
    val isPuzldaiInstalled: Boolean = false,
    val isCheckingInstall: Boolean = true,

    // Chat state
    val messages: List<TuiMessage> = emptyList(),
    val inputText: String = "",
    val commandHistory: List<String> = emptyList(),
    val historyIndex: Int = -1,

    // Agent state
    val currentAgent: String = "claude",
    val agents: List<AgentStatus> = listOf(
        AgentStatus("claude", false),
        AgentStatus("gemini", false),
        AgentStatus("codex", false),
        AgentStatus("ollama", false),
        AgentStatus("mistral", false),
        AgentStatus("factory", false)
    ),

    // Status
    val isLoading: Boolean = false,
    val loadingText: String = "thinking...",
    val tokenCount: Int = 0,
    val mcpStatus: McpStatus = McpStatus.LOCAL,

    // Display
    val showBanner: Boolean = true,
    val errorMessage: String? = null,
    val version: String = "0.1.0"
)

@HiltViewModel
class PkPuzldaiViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val sessionManager: UbuntuSessionManager,
    private val agentManager: AgentManager,
    private val chatRepository: PuzldaiChatRepository
) : ViewModel() {

    private val sessionId: String = savedStateHandle.get<String>("sessionId") ?: ""

    private val _uiState = MutableStateFlow(PkPuzldaiUiState(sessionId = sessionId))
    val uiState: StateFlow<PkPuzldaiUiState> = _uiState.asStateFlow()

    init {
        loadSessionAndObserveState()
        loadChatHistory()
    }

    private fun loadChatHistory() {
        viewModelScope.launch {
            val messages = chatRepository.loadChatHistory(sessionId)
            if (messages.isNotEmpty()) {
                _uiState.value = _uiState.value.copy(
                    messages = messages,
                    showBanner = false // Hide banner if we have history
                )
                Timber.d("Loaded ${messages.size} messages from chat history")
            }
        }
    }

    private fun saveChatHistory() {
        viewModelScope.launch {
            chatRepository.saveChatHistory(sessionId, _uiState.value.messages)
        }
    }

    private fun loadSessionAndObserveState() {
        viewModelScope.launch {
            val session = sessionManager.getSession(sessionId)
            if (session != null) {
                _uiState.value = _uiState.value.copy(
                    sessionName = session.config.name
                )

                // Observe session state changes
                session.stateFlow.collectLatest { state ->
                    val isRunning = state is SessionState.Running
                    _uiState.value = _uiState.value.copy(isSessionRunning = isRunning)

                    // Check pk-puzldai installation when session is running
                    if (isRunning && _uiState.value.isCheckingInstall) {
                        checkPuzldaiInstallation()
                    }

                    // Update agent statuses based on what's available
                    if (isRunning) {
                        checkAgentAvailability()
                    }
                }
            } else {
                _uiState.value = _uiState.value.copy(
                    isCheckingInstall = false,
                    errorMessage = "Session not found"
                )
            }
        }
    }

    private fun checkPuzldaiInstallation() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCheckingInstall = true)

            // Check if pk-puzldai is installed by checking marker file
            val session = sessionManager.getSession(sessionId)
            if (session != null) {
                val result = session.exec("test -f /home/udroid/.pk-puzldai-installed && echo 'yes' || echo 'no'")
                result.fold(
                    onSuccess = { processResult ->
                        val isInstalled = processResult.stdout.trim() == "yes"
                        _uiState.value = _uiState.value.copy(
                            isPuzldaiInstalled = isInstalled,
                            isCheckingInstall = false
                        )
                    },
                    onFailure = {
                        _uiState.value = _uiState.value.copy(
                            isPuzldaiInstalled = false,
                            isCheckingInstall = false
                        )
                    }
                )
            }
        }
    }

    private fun checkAgentAvailability() {
        viewModelScope.launch {
            // Check which agents have API keys configured
            val session = sessionManager.getSession(sessionId) ?: return@launch

            val updatedAgents = _uiState.value.agents.map { agent ->
                val envVar = when (agent.name) {
                    "claude" -> "ANTHROPIC_API_KEY"
                    "gemini" -> "GOOGLE_API_KEY"
                    "ollama" -> null // Ollama runs locally
                    else -> null
                }

                if (envVar != null) {
                    val result = session.exec("test -n \"\$$envVar\" && echo 'yes' || echo 'no'")
                    result.fold(
                        onSuccess = { processResult ->
                            agent.copy(ready = processResult.stdout.trim() == "yes")
                        },
                        onFailure = { agent }
                    )
                } else if (agent.name == "ollama") {
                    // Check if ollama is running
                    val result = session.exec("command -v ollama >/dev/null 2>&1 && echo 'yes' || echo 'no'")
                    result.fold(
                        onSuccess = { processResult ->
                            agent.copy(ready = processResult.stdout.trim() == "yes")
                        },
                        onFailure = { agent }
                    )
                } else {
                    agent
                }
            }

            _uiState.value = _uiState.value.copy(agents = updatedAgents)
        }
    }

    fun updateInput(text: String) {
        _uiState.value = _uiState.value.copy(
            inputText = text,
            historyIndex = -1
        )
    }

    fun submitInput(text: String) {
        val trimmedText = text.trim()
        if (trimmedText.isEmpty()) return

        // Add to command history
        val newHistory = listOf(trimmedText) + _uiState.value.commandHistory.take(99)
        _uiState.value = _uiState.value.copy(
            commandHistory = newHistory,
            historyIndex = -1
        )

        // Check for built-in commands
        when {
            trimmedText.startsWith("/help") -> showHelp()
            trimmedText.startsWith("/clear") -> clearMessages()
            trimmedText.startsWith("/agent ") -> switchAgent(trimmedText.removePrefix("/agent ").trim())
            trimmedText.startsWith("/exit") -> {
                // Handle exit - parent navigation will handle
                addSystemMessage("Use the back button to exit")
            }
            else -> executeCommand(trimmedText)
        }

        // Clear input
        _uiState.value = _uiState.value.copy(inputText = "")
    }

    private fun executeCommand(command: String) {
        if (!_uiState.value.isSessionRunning) {
            addErrorMessage("Session must be running to execute commands")
            return
        }

        if (!_uiState.value.isPuzldaiInstalled) {
            addErrorMessage("pk-puzldai is not installed. Run the installer first.")
            return
        }

        // Add user message
        addUserMessage(command)

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                loadingText = "thinking..."
            )

            val session = sessionManager.getSession(sessionId)
            if (session == null) {
                addErrorMessage("Session not found")
                _uiState.value = _uiState.value.copy(isLoading = false)
                return@launch
            }

            try {
                // Execute via pk-puzldai CLI
                val cliCommand = buildCliCommand(command)
                val result = session.exec(cliCommand)

                result.fold(
                    onSuccess = { processResult ->
                        val output = processResult.stdout.trim()
                        if (output.isNotEmpty()) {
                            addAssistantMessage(
                                content = output,
                                agent = _uiState.value.currentAgent
                            )
                        }
                        if (processResult.exitCode != 0 && processResult.stdout.isEmpty()) {
                            addErrorMessage("Command failed with exit code ${processResult.exitCode}")
                        }
                    },
                    onFailure = { error ->
                        addErrorMessage(error.message ?: "Unknown error")
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "Failed to execute command")
                addErrorMessage(e.message ?: "Execution failed")
            }

            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    private fun buildCliCommand(userInput: String): String {
        // Escape single quotes in user input for shell safety
        val escapedInput = userInput.replace("'", "'\\''")

        // Route through pk-puzldai CLI
        return when {
            userInput.startsWith("/") -> {
                // Slash commands go directly to pk-puzldai
                val subCommand = userInput.removePrefix("/")
                "pk-puzldai $subCommand"
            }
            else -> {
                // Regular messages use the do command
                "pk-puzldai do '$escapedInput'"
            }
        }
    }

    private fun addUserMessage(content: String) {
        val message = TuiMessage(
            id = UUID.randomUUID().toString(),
            role = MessageRole.USER,
            content = content
        )
        _uiState.value = _uiState.value.copy(
            messages = _uiState.value.messages + message
        )
        saveChatHistory()
    }

    private fun addAssistantMessage(content: String, agent: String? = null) {
        val message = TuiMessage(
            id = UUID.randomUUID().toString(),
            role = MessageRole.ASSISTANT,
            content = content,
            agent = agent ?: _uiState.value.currentAgent
        )
        _uiState.value = _uiState.value.copy(
            messages = _uiState.value.messages + message
        )
        saveChatHistory()
    }

    private fun addSystemMessage(content: String) {
        val message = TuiMessage(
            id = UUID.randomUUID().toString(),
            role = MessageRole.SYSTEM,
            content = content
        )
        _uiState.value = _uiState.value.copy(
            messages = _uiState.value.messages + message
        )
        saveChatHistory()
    }

    private fun addErrorMessage(content: String) {
        val message = TuiMessage(
            id = UUID.randomUUID().toString(),
            role = MessageRole.ERROR,
            content = content
        )
        _uiState.value = _uiState.value.copy(
            messages = _uiState.value.messages + message
        )
        saveChatHistory()
    }

    private fun showHelp() {
        val helpText = """
            Available Commands:
            ─────────────────────────────────────────
            /do <task>       Execute task with auto approach
            /chat            Conversational chat mode
            /compare <task>  Compare agent responses
            /autopilot       AI-planned workflow

            /agent <name>    Switch to agent (claude, gemini, etc.)
            /model show      Show current models
            /help            Show this help
            /clear           Clear messages
            /exit            Exit TUI

            Or just type a message to chat!
        """.trimIndent()

        addSystemMessage(helpText)
    }

    private fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            messages = emptyList(),
            showBanner = true
        )
        viewModelScope.launch {
            chatRepository.deleteChatHistory(sessionId)
        }
    }

    private fun switchAgent(agentName: String) {
        val validAgents = _uiState.value.agents.map { it.name }
        if (agentName in validAgents) {
            _uiState.value = _uiState.value.copy(currentAgent = agentName)
            addSystemMessage("Switched to agent: $agentName")
        } else {
            addErrorMessage("Unknown agent: $agentName. Available: ${validAgents.joinToString(", ")}")
        }
    }

    fun navigateHistory(direction: Int) {
        val history = _uiState.value.commandHistory
        if (history.isEmpty()) return

        val currentIndex = _uiState.value.historyIndex
        val newIndex = when {
            direction < 0 -> (currentIndex + 1).coerceAtMost(history.lastIndex)
            direction > 0 -> (currentIndex - 1).coerceAtLeast(-1)
            else -> currentIndex
        }

        _uiState.value = _uiState.value.copy(
            historyIndex = newIndex,
            inputText = if (newIndex >= 0) history.getOrElse(newIndex) { "" } else ""
        )
    }

    fun installPuzldai() {
        if (!_uiState.value.isSessionRunning) {
            addErrorMessage("Session must be running to install pk-puzldai")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                loadingText = "installing pk-puzldai..."
            )

            addSystemMessage("Installing pk-puzldai...")

            agentManager.installAgent(sessionId) { progress ->
                addSystemMessage(progress)
            }.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isPuzldaiInstalled = true,
                        isLoading = false
                    )
                    addSystemMessage("pk-puzldai installed successfully!")
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    addErrorMessage("Installation failed: ${error.message}")
                }
            )
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun toggleBanner() {
        _uiState.value = _uiState.value.copy(
            showBanner = !_uiState.value.showBanner
        )
    }
}
