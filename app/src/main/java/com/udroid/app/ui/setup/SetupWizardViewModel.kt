package com.udroid.app.ui.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    private val sessionManager: UbuntuSessionManager
) : ViewModel() {

    private val _selectedDistro = MutableStateFlow<DistroVariant>(DistroVariant.JAMMY_XFCE4)
    val selectedDistro: StateFlow<DistroVariant> = _selectedDistro.asStateFlow()

    private val _sessionName = MutableStateFlow("Ubuntu Session")
    val sessionName: StateFlow<String> = _sessionName.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

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
                        _setupComplete.value = session.id
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
}
