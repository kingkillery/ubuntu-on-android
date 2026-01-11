package com.udroid.app.ui.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.udroid.app.model.DistroVariant
import com.udroid.app.rootfs.RootfsManager
import com.udroid.app.session.UbuntuSessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class DownloadRequest(
    val sessionId: String,
    val distro: DistroVariant
)

@HiltViewModel
class SessionListViewModel @Inject constructor(
    private val sessionManager: UbuntuSessionManager,
    private val rootfsManager: RootfsManager
) : ViewModel() {

    val sessions: StateFlow<List<com.udroid.app.session.UbuntuSession>> = sessionManager.listSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Event to trigger download in UI layer
    private val _downloadRequest = MutableStateFlow<DownloadRequest?>(null)
    val downloadRequest: StateFlow<DownloadRequest?> = _downloadRequest.asStateFlow()

    fun clearDownloadRequest() {
        _downloadRequest.value = null
    }

    fun isRootfsInstalled(distro: DistroVariant): Boolean {
        return rootfsManager.isRootfsInstalled(distro)
    }

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

                // Find the session to get its distro
                val session = sessions.value.find { it.id == sessionId }
                if (session == null) {
                    _errorMessage.value = "Session not found"
                    return@launch
                }

                val distro = session.config.distro

                // Check if rootfs needs to be downloaded
                if (!distro.bundled && !rootfsManager.isRootfsInstalled(distro)) {
                    if (distro.downloadUrl != null) {
                        Timber.d("Rootfs not installed, requesting download for ${distro.id}")
                        _downloadRequest.value = DownloadRequest(sessionId, distro)
                        _errorMessage.value = "Downloading ${distro.displayName}..."
                        return@launch
                    } else {
                        _errorMessage.value = "No download URL available for ${distro.displayName}"
                        return@launch
                    }
                }

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
