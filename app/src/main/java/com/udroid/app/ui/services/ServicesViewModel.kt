package com.udroid.app.ui.services

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.udroid.app.devbox.BindMode
import com.udroid.app.devbox.ConnectInfo
import com.udroid.app.devbox.DevServiceInstance
import com.udroid.app.devbox.DevServiceManager
import com.udroid.app.devbox.LogEntry
import com.udroid.app.devbox.NetworkUtils
import com.udroid.app.devbox.ServiceState
import com.udroid.app.devbox.ServiceTemplate
import com.udroid.app.model.SessionState
import com.udroid.app.session.UbuntuSessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ServicesViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val sessionManager: UbuntuSessionManager,
    private val serviceManager: DevServiceManager,
    private val networkUtils: NetworkUtils,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val sessionId: String = savedStateHandle.get<String>("sessionId") ?: ""

    private val _sessionName = MutableStateFlow("Session")
    val sessionName: StateFlow<String> = _sessionName.asStateFlow()

    private val _sessionState = MutableStateFlow<SessionState>(SessionState.Created)
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    private val _services = MutableStateFlow<List<DevServiceInstance>>(emptyList())
    val services: StateFlow<List<DevServiceInstance>> = _services.asStateFlow()

    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    private val _deviceIp = MutableStateFlow<String?>(null)
    val deviceIp: StateFlow<String?> = _deviceIp.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    private val _error = MutableSharedFlow<String>()
    val error: SharedFlow<String> = _error.asSharedFlow()

    private val _installationStates = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val installationStates: StateFlow<Map<String, Boolean>> = _installationStates.asStateFlow()

    init {
        loadSession()
        observeServices()
        observeLogs()
        refreshDeviceIp()
        checkInstallations()
    }

    private fun checkInstallations() {
        viewModelScope.launch {
            val states = ServiceTemplate.PRESETS.associate { template ->
                template.id to sessionManager.getSession(sessionId)?.let { 
                     // We need to access DevServiceManager.isInstalled but it requires session
                     // Actually DevServiceManager.isInstalled does that lookup.
                     // But wait, isInstalled is in DevServiceManager, not exposed yet?
                     // I need to make sure I call the manager.
                     serviceManager.isInstalled(template, sessionId)
                }
            }.mapValues { it.value ?: false }
            _installationStates.value = states
        }
    }

    private fun loadSession() {
        viewModelScope.launch {
            val session = sessionManager.getSession(sessionId)
            session?.let {
                _sessionName.value = it.config.name
                _sessionState.value = it.state
                checkInstallations() // Check again when session loads
            }
        }
    }

    private fun observeServices() {
        serviceManager.services
            .onEach { allServices ->
                _services.value = allServices.filter { it.sessionId == sessionId }
            }
            .launchIn(viewModelScope)
    }

    private fun observeLogs() {
        serviceManager.logs
            .onEach { entry ->
                if (_services.value.any { it.id == entry.serviceId }) {
                    _logs.value = (_logs.value + entry).takeLast(100)
                }
            }
            .launchIn(viewModelScope)
    }

    fun refreshDeviceIp() {
        _deviceIp.value = networkUtils.getDeviceIpAddress()
    }

    fun installService(template: ServiceTemplate) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Determine installing state? 
                // Maybe add a temporary "Installing..." state in UI via local state or distinct flow.
                // For now, isLoading covers it.
                
                val result = serviceManager.installService(template, sessionId)
                if (result.isSuccess) {
                    _toastMessage.emit("${template.displayName} installed")
                    checkInstallations()
                } else {
                    _error.emit(result.exceptionOrNull()?.message ?: "Installation failed")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error installing service")
                _error.emit(e.message ?: "Unknown error")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun startService(template: ServiceTemplate) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = serviceManager.startService(
                    template = template,
                    sessionId = sessionId,
                    port = template.defaultPort,
                    bindMode = BindMode.LAN
                )
                if (result.isFailure) {
                    _error.emit(result.exceptionOrNull()?.message ?: "Failed to start service")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error starting service")
                _error.emit(e.message ?: "Unknown error")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun createCustomService(name: String, port: Int, command: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = serviceManager.createCustomService(name, port, command, sessionId)
                if (result.isFailure) {
                    _error.emit(result.exceptionOrNull()?.message ?: "Failed to create service")
                } else {
                    _toastMessage.emit("Custom service started")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error creating custom service")
                _error.emit(e.message ?: "Unknown error")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun stopService(serviceId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = serviceManager.stopService(serviceId)
                if (result.isFailure) {
                    _error.emit(result.exceptionOrNull()?.message ?: "Failed to stop service")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error stopping service")
                _error.emit(e.message ?: "Unknown error")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun toggleService(template: ServiceTemplate) {
        val existingService = _services.value.find {
            it.template.id == template.id && it.state is ServiceState.Running
        }

        if (existingService != null) {
            stopService(existingService.id)
        } else {
            // Check if installed
            val isInstalled = _installationStates.value[template.id] ?: false
            if (!isInstalled && !template.installCommand.isNullOrEmpty()) {
                installService(template)
            } else {
                startService(template)
            }
        }
    }

    fun getConnectInfo(serviceId: String): ConnectInfo? {
        return serviceManager.getConnectInfo(serviceId)
    }

    fun getConnectInfoForTemplate(template: ServiceTemplate): ConnectInfo {
        return networkUtils.buildConnectInfo(template, template.defaultPort, BindMode.LAN)
    }

    fun copyToClipboard(text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Connect String", text)
        clipboard.setPrimaryClip(clip)

        viewModelScope.launch {
            _toastMessage.emit("Copied to clipboard")
        }
    }

    fun generateQrCode(content: String): Bitmap {
        return networkUtils.generateQrBitmap(content)
    }

    fun isServiceRunning(template: ServiceTemplate): Boolean {
        return _services.value.any {
            it.template.id == template.id && it.state is ServiceState.Running
        }
    }

    fun getServiceState(template: ServiceTemplate): ServiceState {
        return _services.value.find { it.template.id == template.id }?.state ?: ServiceState.Stopped
    }
}
