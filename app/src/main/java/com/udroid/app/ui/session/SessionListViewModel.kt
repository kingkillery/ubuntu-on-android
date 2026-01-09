package com.udroid.app.ui.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.udroid.app.session.UbuntuSessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SessionListViewModel @Inject constructor(
    private val sessionManager: UbuntuSessionManager
) : ViewModel() {

    private val _sessions = sessionManager.listSessions()
    val sessions: StateFlow<List<com.udroid.app.session.UbuntuSession>> = _sessions
        .asStateFlow(initial = emptyList())

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                Timber.d("Deleting session: $sessionId")
                
                val result = sessionManager.deleteSession(sessionId)
                
                result.fold(
                    onSuccess = {
                        Timber.d("Session deleted: $sessionId")
                    },
                    onFailure = { error ->
                        Timber.e(error, "Failed to delete session")
                        _errorMessage.value = error.message ?: "Failed to delete session"
                    }
                )
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun startSession(sessionId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                Timber.d("Starting session: $sessionId")
                
                val result = sessionManager.startSession(sessionId)
                
                result.fold(
                    onSuccess = {
                        Timber.d("Session started: $sessionId")
                    },
                    onFailure = { error ->
                        Timber.e(error, "Failed to start session")
                        _errorMessage.value = error.message ?: "Failed to start session"
                    }
                )
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun stopSession(sessionId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                Timber.d("Stopping session: $sessionId")
                
                val result = sessionManager.stopSession(sessionId)
                
                result.fold(
                    onSuccess = {
                        Timber.d("Session stopped: $sessionId")
                    },
                    onFailure = { error ->
                        Timber.e(error, "Failed to stop session")
                        _errorMessage.value = error.message ?: "Failed to stop session"
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
