package com.udroid.app.ui.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.udroid.app.agent.AgentManager
import com.udroid.app.model.DistroVariant
import com.udroid.app.session.SessionConfig
import com.udroid.app.session.UbuntuSessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SetupWizardViewModel @Inject constructor(
    private val sessionManager: UbuntuSessionManager,
    private val agentManager: AgentManager
) : ViewModel() {

    private val _selectedDistro = MutableStateFlow<DistroVariant>(DistroVariant.JAMMY_XFCE4)
    val selectedDistro: StateFlow<DistroVariant> = _selectedDistro.asStateFlow()

    private val _sessionName = MutableStateFlow("Ubuntu Session")
    val sessionName: StateFlow<String> = _sessionName.asStateFlow()

    private val _installAgentTools = MutableStateFlow(true)
    val installAgentTools: StateFlow<Boolean> = _installAgentTools.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _agentInstallProgress = MutableStateFlow<String?>(null)
    val agentInstallProgress: StateFlow<String?> = _agentInstallProgress.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _setupComplete = MutableStateFlow<String?>(null)
    val setupComplete: StateFlow<String?> = _setupComplete.asStateFlow()

    fun selectDistro(distro: DistroVariant) {
        _selectedDistro.value = distro
        Timber.d("Selected distro: ${distro.id}")
    }

    fun updateSessionName(name: String) {
        _sessionName.value = name
    }

    fun toggleAgentTools() {
        _installAgentTools.value = !_installAgentTools.value
    }

    fun createSession() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null

                val config = SessionConfig(
                    name = _sessionName.value.ifBlank { "Ubuntu Session" },
                    distro = _selectedDistro.value,
                    desktopEnvironment = _selectedDistro.value.desktop,
                    enableSound = true,
                    enableNetwork = true
                )

                Timber.d("Creating session: ${config.name} with ${config.distro.id}")

                val result = sessionManager.createSession(config)
                
                result.fold(
                    onSuccess = { session ->
                        Timber.d("Session created: ${session.id}")

                        // Start the session first if agent tools need to be installed
                        if (_installAgentTools.value) {
                            installAgentToolsInSession(session.id)
                        } else {
                            _setupComplete.value = session.id
                        }
                    },
                    onFailure = { error ->
                        Timber.e(error, "Failed to create session")
                        _errorMessage.value = error.message ?: "Failed to create session"
                    }
                )
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    private fun installAgentToolsInSession(sessionId: String) {
        viewModelScope.launch {
            try {
                _agentInstallProgress.value = "Starting session..."

                // Start the session first
                val startResult = sessionManager.startSession(sessionId)
                if (startResult.isFailure) {
                    _errorMessage.value = "Failed to start session: ${startResult.exceptionOrNull()?.message}"
                    _isLoading.value = false
                    return@launch
                }

                _agentInstallProgress.value = "Installing agent tools..."

                // Install agent tools
                val installResult = agentManager.installAgent(sessionId) { progress ->
                    _agentInstallProgress.value = progress
                }

                installResult.fold(
                    onSuccess = {
                        Timber.d("Agent tools installed successfully")
                        _agentInstallProgress.value = "Setup complete!"
                        _setupComplete.value = sessionId
                    },
                    onFailure = { error ->
                        Timber.e(error, "Failed to install agent tools")
                        // Don't fail the whole setup, just warn
                        _agentInstallProgress.value = "Agent tools install failed, continuing..."
                        _setupComplete.value = sessionId
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "Error during agent installation")
                _setupComplete.value = sessionId
            } finally {
                _isLoading.value = false
            }
        }
    }
}
