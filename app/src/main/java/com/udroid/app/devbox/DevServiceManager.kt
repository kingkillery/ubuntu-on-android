package com.udroid.app.devbox

import android.content.Context
import com.udroid.app.session.UbuntuSessionManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DevServiceManager @Inject constructor(
    private val sessionManager: UbuntuSessionManager,
    private val networkUtils: NetworkUtils,
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val servicesFile by lazy { java.io.File(context.filesDir, "dev_services.json") }

    private val _services = MutableStateFlow<List<DevServiceInstance>>(emptyList())
    val services: StateFlow<List<DevServiceInstance>> = _services.asStateFlow()

    private val _logs = MutableSharedFlow<LogEntry>(replay = 100)
    val logs: SharedFlow<LogEntry> = _logs.asSharedFlow()

    init {
        loadServices()
    }

    private fun loadServices() {
        if (!servicesFile.exists()) return
        try {
            val json = servicesFile.readText()
            val jsonArray = org.json.JSONArray(json)
            val list = mutableListOf<DevServiceInstance>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val templateId = obj.getString("templateId")
                
                // Try to find in presets first
                val existingTemplate = ServiceTemplate.fromId(templateId)
                
                val template = if (existingTemplate != null) {
                    existingTemplate
                } else {
                    // Reconstruct custom template
                    if (obj.has("customTemplate")) {
                        val customObj = obj.getJSONObject("customTemplate")
                        ServiceTemplate(
                            id = customObj.getString("id"),
                            displayName = customObj.getString("displayName"),
                            description = customObj.getString("description"),
                            defaultPort = customObj.getInt("defaultPort"),
                            startCommand = customObj.getString("startCommand"),
                            healthCheck = customObj.getString("healthCheck"),
                            isCustom = true
                        )
                    } else {
                        continue // Skip invalid/legacy without custom data
                    }
                }

                list.add(DevServiceInstance(
                    id = obj.getString("id"),
                    template = template,
                    sessionId = obj.getString("sessionId"),
                    port = obj.getInt("port"),
                    bindMode = BindMode.valueOf(obj.getString("bindMode")),
                    state = ServiceState.Stopped // Reset to stopped on app restart
                ))
            }
            _services.value = list
        } catch (e: Exception) {
            Timber.e(e, "Failed to load services")
        }
    }

    private fun saveServices() {
        try {
            val jsonArray = org.json.JSONArray()
            _services.value.forEach { service ->
                val obj = org.json.JSONObject()
                obj.put("id", service.id)
                obj.put("templateId", service.template.id)
                obj.put("sessionId", service.sessionId)
                obj.put("port", service.port)
                obj.put("bindMode", service.bindMode.name)
                
                if (service.template.isCustom) {
                    val customObj = org.json.JSONObject()
                    customObj.put("id", service.template.id)
                    customObj.put("displayName", service.template.displayName)
                    customObj.put("description", service.template.description)
                    customObj.put("defaultPort", service.template.defaultPort)
                    customObj.put("startCommand", service.template.startCommand)
                    customObj.put("healthCheck", service.template.healthCheck)
                    obj.put("customTemplate", customObj)
                }
                
                jsonArray.put(obj)
            }
            servicesFile.writeText(jsonArray.toString(2))
        } catch (e: Exception) {
            Timber.e(e, "Failed to save services")
        }
    }

    suspend fun createCustomService(
        displayName: String,
        port: Int,
        command: String,
        sessionId: String
    ): Result<DevServiceInstance> {
        val id = UUID.randomUUID().toString()
        val template = ServiceTemplate(
            id = "custom_$id",
            displayName = displayName,
            description = "Custom Service: $command",
            defaultPort = port,
            startCommand = command,
            healthCheck = "nc -z 127.0.0.1 $port", // Basic default healthcheck
            isCustom = true
        )
        
        return startService(template, sessionId, port)
    }

    suspend fun installService(
        template: ServiceTemplate,
        sessionId: String
    ): Result<Unit> {
        val serviceId = UUID.randomUUID().toString() // Temporary ID for logging? Or passed in?
        // Actually, installation is per-template/distro, not per service instance. 
        // But we run it in a session.
        
        return try {
            val session = sessionManager.getSession(sessionId)
                ?: return Result.failure(NoSuchElementException("Session not found"))

            if (template.installCommand.isNullOrEmpty()) {
                return Result.success(Unit) // Nothing to install
            }

            emitLog("INSTALL", LogLevel.INFO, "Installing ${template.displayName}...")
            
            val result = session.exec(template.installCommand)
            if (result.isSuccess && result.getOrNull()?.exitCode == 0) {
                emitLog("INSTALL", LogLevel.INFO, "Installation successful")
                Result.success(Unit)
            } else {
                val err = result.getOrNull()?.stderr ?: "Unknown error"
                emitLog("INSTALL", LogLevel.ERROR, "Installation failed: $err")
                Result.failure(Exception("Installation failed: $err"))
            }
        } catch (e: Exception) {
             emitLog("INSTALL", LogLevel.ERROR, "Installation error: ${e.message}")
             Result.failure(e)
        }
    }

    suspend fun startService(
        template: ServiceTemplate,
        sessionId: String,
        port: Int = template.defaultPort,
        bindMode: BindMode = BindMode.LAN
    ): Result<DevServiceInstance> {
        return try {
            Timber.d("Starting service: ${template.displayName} on port $port")

            // Check if service already running on this port
            val existing = _services.value.find {
                it.sessionId == sessionId && it.template.id == template.id && it.state is ServiceState.Running
            }
            if (existing != null) {
                return Result.failure(IllegalStateException("Service already running"))
            }

            // Check if installed first
            val session = sessionManager.getSession(sessionId)
                ?: return Result.failure(NoSuchElementException("Session not found"))

            if (!template.checkCommand.isNullOrEmpty()) {
                 val check = session.exec(template.checkCommand)
                 if (check.isFailure || check.getOrNull()?.exitCode != 0) {
                     return Result.failure(IllegalStateException("Service not installed. Please install first."))
                 }
            }

            val serviceId = UUID.randomUUID().toString()
            val service = DevServiceInstance(
                id = serviceId,
                template = template,
                sessionId = sessionId,
                port = port,
                bindMode = bindMode,
                state = ServiceState.Starting
            )

            // Add to list and save
            updateServiceList { it + service }
            emitLog(serviceId, LogLevel.INFO, "Starting ${template.displayName}...")

            // Build command
            val command = buildStartCommand(template, port, bindMode)
            emitLog(serviceId, LogLevel.INFO, "Executing: $command")

            // Execute in background
            scope.launch {
                val result = session.exec("nohup $command > /tmp/${template.id}.log 2>&1 &")
                if (result.isFailure) {
                    Timber.e(result.exceptionOrNull(), "Failed to start service")
                    updateServiceState(serviceId, ServiceState.Error(
                        result.exceptionOrNull()?.message ?: "Failed to start"
                    ))
                    emitLog(serviceId, LogLevel.ERROR, "Failed: ${result.exceptionOrNull()?.message}")
                }
            }

            // Wait a moment for service to start
            delay(1000)

            // Check health
            val healthResult = session.exec(template.healthCheck)
            val isHealthy = healthResult.isSuccess && healthResult.getOrNull()?.exitCode == 0

            if (isHealthy) {
                updateServiceState(serviceId, ServiceState.Running())
                emitLog(serviceId, LogLevel.INFO, "${template.displayName} is running on port $port")
                Result.success(getService(serviceId)!!)
            } else {
                updateServiceState(serviceId, ServiceState.Running()) // Optimistic
                emitLog(serviceId, LogLevel.WARN, "Service started but health check pending")
                Result.success(getService(serviceId)!!)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error starting service")
            Result.failure(e)
        }
    }

    suspend fun stopService(serviceId: String): Result<Unit> {
        return try {
            val service = getService(serviceId)
                ?: return Result.failure(NoSuchElementException("Service not found"))

            Timber.d("Stopping service: ${service.template.displayName}")
            emitLog(serviceId, LogLevel.INFO, "Stopping ${service.template.displayName}...")

            val session = sessionManager.getSession(service.sessionId)
                ?: return Result.failure(NoSuchElementException("Session not found"))

            // Kill the process
            val killCommand = buildKillCommand(service.template)
            val result = session.exec(killCommand)

            if (result.isSuccess) {
                updateServiceState(serviceId, ServiceState.Stopped)
                emitLog(serviceId, LogLevel.INFO, "${service.template.displayName} stopped")
                Result.success(Unit)
            } else {
                // If it fails, maybe it's already dead? mark as stopped anyway
                updateServiceState(serviceId, ServiceState.Stopped)
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error stopping service")
            Result.failure(e)
        }
    }

    fun getService(serviceId: String): DevServiceInstance? {
        return _services.value.find { it.id == serviceId }
    }

    fun getServicesForSession(sessionId: String): List<DevServiceInstance> {
        return _services.value.filter { it.sessionId == sessionId }
    }

    fun getConnectInfo(serviceId: String): ConnectInfo? {
        val service = getService(serviceId) ?: return null
        return networkUtils.buildConnectInfo(service.template, service.port, service.bindMode)
    }

    suspend fun checkHealth(serviceId: String): Boolean {
        val service = getService(serviceId) ?: return false
        val session = sessionManager.getSession(service.sessionId) ?: return false

        return try {
            val result = session.exec(service.template.healthCheck)
            result.isSuccess && result.getOrNull()?.exitCode == 0
        } catch (e: Exception) {
            false
        }
    }
    
    suspend fun isInstalled(template: ServiceTemplate, sessionId: String): Boolean {
        if (template.checkCommand == null) return true // No check needed
        val session = sessionManager.getSession(sessionId) ?: return false
        val result = session.exec(template.checkCommand)
        return result.isSuccess && result.getOrNull()?.exitCode == 0
    }

    private fun buildStartCommand(template: ServiceTemplate, port: Int, bindMode: BindMode): String {
        val bindAddr = bindMode.bindAddress
        // Simple string replacement if possible, otherwise callback to legacy hardcoded logic for now
        // But we changed ServiceTemplate to Data Class.
        
        return when (template.id) {
            "ssh" -> "dropbear -F -E -p $bindAddr:$port"
            "jupyter" -> "jupyter lab --ip=$bindAddr --port=$port --no-browser --allow-root --NotebookApp.token=''"
            "http" -> "python3 -m http.server $port --bind $bindAddr"
            "nginx" -> "nginx -g 'daemon off;' -c /etc/nginx/nginx.conf" // Port needs config file edit usually, ignoing port param for now for nginx unless we sed it.
            "nodejs" -> "node app.js" // Placeholder
            else -> template.startCommand // Use the raw command if no override logic
        }
    }

    private fun buildKillCommand(template: ServiceTemplate): String {
        return when (template.id) {
            "ssh" -> "pkill -f dropbear"
            "jupyter" -> "pkill -f jupyter"
            "http" -> "pkill -f 'python3 -m http.server'"
            "nginx" -> "pkill -f nginx"
            "nodejs" -> "pkill -f node"
            else -> "pkill -f ${template.id}" // Fallback?
        }
    }

    private fun updateServiceList(update: (List<DevServiceInstance>) -> List<DevServiceInstance>) {
        _services.update(update)
        saveServices()
    }

    private fun updateServiceState(serviceId: String, state: ServiceState) {
        updateServiceList { list ->
            list.map { service ->
                if (service.id == serviceId) {
                    service.copy(state = state)
                } else {
                    service
                }
            }
        }
    }

    private suspend fun emitLog(serviceId: String, level: LogLevel, message: String) {
        _logs.emit(LogEntry(
            serviceId = serviceId,
            level = level,
            message = message
        ))
    }
}
