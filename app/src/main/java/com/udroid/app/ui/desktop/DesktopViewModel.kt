package com.udroid.app.ui.desktop

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.udroid.app.model.SessionState
import com.udroid.app.session.UbuntuSessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject

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

    fun connectToSession(id: String) {
        Timber.d("Connecting to session: $id")
        // Observe session state
        // TODO: Implement actual VNC connection
    }

    fun sendTouchEvent(x: Float, y: Float, action: String) {
        // TODO: Send touch events to VNC bridge
        Timber.d("Touch event: ($x, $y) - $action")
    }

    fun stopSession() {
        // Launch coroutine in viewModel scope
        // For now, just log - the actual stop happens via service
        Timber.d("Stop session requested: $sessionId")
    }
}
