package com.udroid.app.ui.terminal

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.udroid.app.model.ProcessResult
import com.udroid.app.session.UbuntuSessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class TerminalLine(
    val text: String,
    val isCommand: Boolean = false,
    val isError: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

data class TerminalUiState(
    val sessionId: String = "",
    val sessionName: String = "",
    val isConnected: Boolean = false,
    val isExecuting: Boolean = false,
    val lines: List<TerminalLine> = emptyList(),
    val commandHistory: List<String> = emptyList(),
    val historyIndex: Int = -1,
    val currentInput: String = "",
    val errorMessage: String? = null
)

@HiltViewModel
class TerminalViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val sessionManager: UbuntuSessionManager
) : ViewModel() {

    private val sessionId: String = savedStateHandle.get<String>("sessionId") ?: ""

    private val _uiState = MutableStateFlow(TerminalUiState(sessionId = sessionId))
    val uiState: StateFlow<TerminalUiState> = _uiState.asStateFlow()

    init {
        loadSession()
    }

    private fun loadSession() {
        viewModelScope.launch {
            val session = sessionManager.getSession(sessionId)
            if (session != null) {
                _uiState.value = _uiState.value.copy(
                    sessionName = session.config.name,
                    isConnected = true
                )
                addLine(TerminalLine(
                    text = "Connected to ${session.config.name} (${session.config.distro.displayName})",
                    isCommand = false
                ))
                addLine(TerminalLine(
                    text = "Type commands and press Enter to execute. Use Up/Down arrows for history.",
                    isCommand = false
                ))
                addLine(TerminalLine(text = ""))
            } else {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Session not found: $sessionId"
                )
            }
        }
    }

    fun updateInput(input: String) {
        _uiState.value = _uiState.value.copy(currentInput = input)
    }

    fun executeCommand() {
        val command = _uiState.value.currentInput.trim()
        if (command.isEmpty()) return

        viewModelScope.launch {
            // Add command to history
            val newHistory = _uiState.value.commandHistory.toMutableList()
            if (newHistory.isEmpty() || newHistory.last() != command) {
                newHistory.add(command)
            }

            // Show command in terminal
            addLine(TerminalLine(text = "$ $command", isCommand = true))

            _uiState.value = _uiState.value.copy(
                currentInput = "",
                isExecuting = true,
                commandHistory = newHistory,
                historyIndex = -1
            )

            try {
                val session = sessionManager.getSession(sessionId)
                if (session == null) {
                    addLine(TerminalLine(text = "Error: Session not found", isError = true))
                    return@launch
                }

                val result = session.exec(command, timeoutSeconds = 60)
                result.fold(
                    onSuccess = { processResult ->
                        handleProcessResult(processResult)
                    },
                    onFailure = { error ->
                        addLine(TerminalLine(
                            text = "Error: ${error.message ?: "Command failed"}",
                            isError = true
                        ))
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "Failed to execute command: $command")
                addLine(TerminalLine(
                    text = "Error: ${e.message ?: "Unknown error"}",
                    isError = true
                ))
            } finally {
                _uiState.value = _uiState.value.copy(isExecuting = false)
            }
        }
    }

    private fun handleProcessResult(result: ProcessResult) {
        // Add stdout lines
        if (result.stdout.isNotEmpty()) {
            result.stdout.lines().forEach { line ->
                if (line.isNotEmpty()) {
                    addLine(TerminalLine(text = line))
                }
            }
        }

        // Add stderr lines (as errors)
        if (result.stderr.isNotEmpty()) {
            result.stderr.lines().forEach { line ->
                if (line.isNotEmpty()) {
                    addLine(TerminalLine(text = line, isError = true))
                }
            }
        }

        // Show exit code if non-zero
        if (result.exitCode != 0) {
            addLine(TerminalLine(
                text = "[Exit code: ${result.exitCode}]",
                isError = true
            ))
        }
    }

    fun navigateHistory(direction: Int) {
        val history = _uiState.value.commandHistory
        if (history.isEmpty()) return

        val currentIndex = _uiState.value.historyIndex
        val newIndex = when {
            direction < 0 -> { // Up - go back in history
                if (currentIndex < 0) history.size - 1
                else (currentIndex - 1).coerceAtLeast(0)
            }
            direction > 0 -> { // Down - go forward in history
                if (currentIndex < 0) -1
                else if (currentIndex >= history.size - 1) -1
                else currentIndex + 1
            }
            else -> currentIndex
        }

        val newInput = if (newIndex < 0 || newIndex >= history.size) "" else history[newIndex]
        _uiState.value = _uiState.value.copy(
            historyIndex = newIndex,
            currentInput = newInput
        )
    }

    fun clearTerminal() {
        _uiState.value = _uiState.value.copy(lines = emptyList())
        addLine(TerminalLine(text = "Terminal cleared", isCommand = false))
    }

    private fun addLine(line: TerminalLine) {
        val currentLines = _uiState.value.lines.toMutableList()
        currentLines.add(line)
        // Keep last 1000 lines to prevent memory issues
        if (currentLines.size > 1000) {
            currentLines.removeAt(0)
        }
        _uiState.value = _uiState.value.copy(lines = currentLines)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
