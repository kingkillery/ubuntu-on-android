package com.udroid.app.ui.desktop

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.udroid.app.model.SessionState
import com.udroid.app.session.UbuntuSessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.util.Log
import timber.log.Timber
import javax.inject.Inject

private const val TAG = "DesktopViewModel"

/**
 * Data class holding share state for the desktop UI.
 */
data class ShareUiState(
    val isSharePanelVisible: Boolean = false,
    val shareUrl: String = "",
    val sharePort: Int = 5901
)

@HiltViewModel
class DesktopViewModel @Inject constructor(
    private val sessionManager: UbuntuSessionManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val sessionId: String = checkNotNull(savedStateHandle["sessionId"])

    private val _sessionState = MutableStateFlow<SessionState>(SessionState.Created)
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _shareUiState = MutableStateFlow(ShareUiState())
    val shareUiState: StateFlow<ShareUiState> = _shareUiState.asStateFlow()

    init {
        // Auto-start session when ViewModel is created
        startSessionIfNeeded()
    }

    private fun startSessionIfNeeded() {
        viewModelScope.launch {
            Log.d(TAG, "=== startSessionIfNeeded called for: $sessionId ===")
            Timber.d("Checking session state: $sessionId")
            val session = sessionManager.getSession(sessionId)

            if (session == null) {
                Log.e(TAG, "Session not found: $sessionId")
                Timber.e("Session not found: $sessionId")
                _sessionState.value = SessionState.Error("Session not found")
                return@launch
            }

            Log.d(TAG, "Current session state: ${session.state}")
            Timber.d("Current session state: ${session.state}")
            _sessionState.value = session.state

            // Start session if not running
            when (session.state) {
                is SessionState.Running -> {
                    Log.d(TAG, "Session already running")
                    Timber.d("Session already running")
                    _isRunning.value = true
                }
                is SessionState.Created, is SessionState.Stopped -> {
                    Log.d(TAG, "Starting session... calling sessionManager.startSession")
                    Timber.d("Starting session...")
                    _sessionState.value = SessionState.Starting

                    val result = sessionManager.startSession(sessionId)
                    Log.d(TAG, "startSession returned: isSuccess=${result.isSuccess}")
                    result.fold(
                        onSuccess = {
                            Log.d(TAG, "Session started successfully!")
                            Timber.d("Session started successfully")
                            _sessionState.value = SessionState.Running(5901)
                            _isRunning.value = true
                        },
                        onFailure = { error ->
                            Log.e(TAG, "Failed to start session: ${error.message}", error)
                            Timber.e(error, "Failed to start session")
                            _sessionState.value = SessionState.Error(error.message ?: "Failed to start")
                            _errorMessage.value = error.message
                        }
                    )
                }
                is SessionState.Starting -> {
                    Log.d(TAG, "Session is already starting")
                    Timber.d("Session is already starting")
                }
                is SessionState.Error -> {
                    Log.d(TAG, "Session in error state, attempting restart")
                    Timber.d("Session in error state, attempting restart")
                    _sessionState.value = SessionState.Starting
                    sessionManager.startSession(sessionId)
                }
                else -> {}
            }
        }
    }

    fun connectToSession(id: String) {
        Timber.d("Connecting to session: $id")
    }

    fun sendTouchEvent(x: Float, y: Float, action: String) {
        Timber.d("Touch event: ($x, $y) - $action")
    }

    fun stopSession() {
        viewModelScope.launch {
            Timber.d("Stop session requested: $sessionId")
            _sessionState.value = SessionState.Stopping

            val result = sessionManager.stopSession(sessionId)
            result.fold(
                onSuccess = {
                    _sessionState.value = SessionState.Stopped
                    _isRunning.value = false
                },
                onFailure = { error ->
                    Timber.e(error, "Failed to stop session")
                    _errorMessage.value = error.message
                }
            )
        }
    }

    fun toggleSharePanel() {
        val currentState = _shareUiState.value
        _shareUiState.value = currentState.copy(
            isSharePanelVisible = !currentState.isSharePanelVisible,
            shareUrl = if (!currentState.isSharePanelVisible) generateStubShareUrl() else currentState.shareUrl
        )
        Timber.d("Share panel toggled: ${_shareUiState.value.isSharePanelVisible}")
    }

    fun hideSharePanel() {
        _shareUiState.value = _shareUiState.value.copy(isSharePanelVisible = false)
    }

    private fun generateStubShareUrl(): String {
        // Stub URL for now - will be replaced with actual networking in future story
        val port = (_sessionState.value as? SessionState.Running)?.vncPort ?: 5901
        return "vnc://192.168.1.100:$port"
    }
}
