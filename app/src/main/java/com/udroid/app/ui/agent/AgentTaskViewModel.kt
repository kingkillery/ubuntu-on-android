package com.udroid.app.ui.agent

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.udroid.app.agent.AgentConfig
import com.udroid.app.agent.AgentManager
import com.udroid.app.agent.AgentTaskResult
import com.udroid.app.model.SessionState
import com.udroid.app.session.UbuntuSessionManager
import com.udroid.app.ui.agent.AgentMode
import com.udroid.app.ui.agent.PresetCategory
import com.udroid.app.ui.agent.PresetTask
import com.udroid.app.ui.agent.PresetTasks
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class TaskHistoryItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val task: String,
    val result: AgentTaskResult? = null,
    val isRunning: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

data class AgentTaskUiState(
    val sessionId: String = "",
    val sessionName: String = "",
    val isSessionRunning: Boolean = false,
    val isAgentInstalled: Boolean = false,
    val isCheckingInstall: Boolean = true,
    val taskInput: String = "",
    val isRunning: Boolean = false,
    val currentOutput: String = "",
    val taskHistory: List<TaskHistoryItem> = emptyList(),
    val errorMessage: String? = null,
    val selectedTool: AgentTool = AgentTool.AGENT_RUN,
    // New fields for Basic/Advanced mode
    val mode: AgentMode = AgentMode.BASIC,
    val selectedCategory: PresetCategory? = null,
    val selectedPreset: PresetTask? = null
)

enum class AgentTool(val displayName: String, val description: String) {
    AGENT_RUN("Agent Run", "Run tasks with AI orchestration"),
    GEMINI("Gemini", "Query Google Gemini for answers"),
    DROID("Droid", "factory.ai droid CLI commands")
}

@HiltViewModel
class AgentTaskViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val sessionManager: UbuntuSessionManager,
    private val agentManager: AgentManager
) : ViewModel() {

    private val sessionId: String = savedStateHandle.get<String>("sessionId") ?: ""

    private val _uiState = MutableStateFlow(AgentTaskUiState(sessionId = sessionId))
    val uiState: StateFlow<AgentTaskUiState> = _uiState.asStateFlow()

    init {
        loadSessionAndObserveState()
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

                    // Only check agent installation when session is running
                    if (isRunning && _uiState.value.isCheckingInstall) {
                        checkAgentInstallation()
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

    private fun checkAgentInstallation() {
        viewModelScope.launch {
            if (!_uiState.value.isSessionRunning) {
                _uiState.value = _uiState.value.copy(
                    isCheckingInstall = false,
                    errorMessage = "Session must be running to check agent installation"
                )
                return@launch
            }

            _uiState.value = _uiState.value.copy(isCheckingInstall = true)
            val isInstalled = agentManager.isAgentInstalled(sessionId)
            _uiState.value = _uiState.value.copy(
                isAgentInstalled = isInstalled,
                isCheckingInstall = false
            )
        }
    }

    fun updateTaskInput(input: String) {
        _uiState.value = _uiState.value.copy(taskInput = input)
    }

    fun selectTool(tool: AgentTool) {
        _uiState.value = _uiState.value.copy(selectedTool = tool)
    }

    fun runTask() {
        val task = _uiState.value.taskInput.trim()
        if (task.isEmpty()) return

        if (!_uiState.value.isSessionRunning) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Session must be running to execute tasks"
            )
            return
        }

        viewModelScope.launch {
            val historyItem = TaskHistoryItem(
                task = task,
                isRunning = true
            )

            _uiState.value = _uiState.value.copy(
                isRunning = true,
                taskInput = "",
                currentOutput = "Running task: $task\n",
                taskHistory = listOf(historyItem) + _uiState.value.taskHistory
            )

            try {
                val result = when (_uiState.value.selectedTool) {
                    AgentTool.AGENT_RUN -> agentManager.runAgentTask(sessionId, task)
                    AgentTool.GEMINI -> agentManager.runGeminiQuery(sessionId, task)
                    AgentTool.DROID -> agentManager.runDroidCommand(sessionId, task)
                }

                val updatedItem = historyItem.copy(
                    result = result,
                    isRunning = false
                )

                val output = buildString {
                    append("Task: $task\n")
                    append("-".repeat(40))
                    append("\n")
                    if (result.output.isNotEmpty()) {
                        append(result.output)
                        if (!result.output.endsWith("\n")) append("\n")
                    }
                    if (result.error != null) {
                        append("Error: ${result.error}\n")
                    }
                    append("-".repeat(40))
                    append("\n")
                    append("Exit code: ${result.exitCode} | Duration: ${result.durationMs}ms\n")
                }

                _uiState.value = _uiState.value.copy(
                    isRunning = false,
                    currentOutput = output,
                    taskHistory = listOf(updatedItem) + _uiState.value.taskHistory.drop(1)
                )

            } catch (e: Exception) {
                Timber.e(e, "Failed to run task")

                val failedItem = historyItem.copy(
                    result = AgentTaskResult(
                        success = false,
                        output = "",
                        error = e.message
                    ),
                    isRunning = false
                )

                _uiState.value = _uiState.value.copy(
                    isRunning = false,
                    currentOutput = "Error: ${e.message}",
                    taskHistory = listOf(failedItem) + _uiState.value.taskHistory.drop(1),
                    errorMessage = e.message
                )
            }
        }
    }

    fun installAgentTools() {
        if (!_uiState.value.isSessionRunning) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Session must be running to install agent tools"
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isRunning = true,
                currentOutput = "Installing agent tools...\n"
            )

            agentManager.installAgent(sessionId) { progress ->
                _uiState.value = _uiState.value.copy(
                    currentOutput = _uiState.value.currentOutput + progress + "\n"
                )
            }.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isAgentInstalled = true,
                        isRunning = false,
                        currentOutput = _uiState.value.currentOutput + "\nInstallation complete!"
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isRunning = false,
                        errorMessage = error.message,
                        currentOutput = _uiState.value.currentOutput + "\nInstallation failed: ${error.message}"
                    )
                }
            )
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun clearOutput() {
        _uiState.value = _uiState.value.copy(currentOutput = "")
    }

    // Basic/Advanced Mode methods
    fun switchMode(mode: AgentMode) {
        _uiState.value = _uiState.value.copy(mode = mode)
        // Reset selections when switching modes
        if (mode == AgentMode.BASIC) {
            _uiState.value = _uiState.value.copy(
                selectedTool = AgentTool.AGENT_RUN,
                selectedPreset = null
            )
        }
    }

    fun selectCategory(category: PresetCategory) {
        _uiState.value = _uiState.value.copy(
            selectedCategory = category,
            selectedPreset = null,
            taskInput = ""
        )
    }

    fun selectPreset(preset: PresetTask) {
        _uiState.value = _uiState.value.copy(
            selectedPreset = preset,
            taskInput = preset.templatePrompt
        )
    }

    fun runPresetTask() {
        val preset = _uiState.value.selectedPreset
        if (preset == null) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Please select a preset task first"
            )
            return
        }

        // For preset tasks, always use Agent Run tool
        _uiState.value = _uiState.value.copy(
            selectedTool = AgentTool.AGENT_RUN,
            taskInput = preset.templatePrompt
        )
        runTask()
    }

    fun getPresetsForCategory(category: PresetCategory): List<PresetTask> {
        return PresetTasks.getByCategory(category)
    }

    fun getAllPresets(): List<PresetTask> = PresetTasks.tasks
}
