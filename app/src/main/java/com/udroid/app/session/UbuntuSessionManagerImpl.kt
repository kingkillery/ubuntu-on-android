package com.udroid.app.session

import com.udroid.app.model.ProcessResult
import com.udroid.app.model.SessionState
import com.udroid.app.nativebridge.NativeBridge
import com.udroid.app.proxy.ProxyManager
import com.udroid.app.rootfs.RootfsManager
import com.udroid.app.storage.SessionInfo
import com.udroid.app.storage.SessionRepository
import com.udroid.app.storage.toData
import com.udroid.app.storage.toDomain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import android.util.Log
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "UbuntuSessionManager"

@Singleton
class UbuntuSessionManagerImpl @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val nativeBridge: NativeBridge,
    private val rootfsManager: RootfsManager,
    private val proxyManager: ProxyManager
) : UbuntuSessionManager {

    override suspend fun createSession(config: SessionConfig): Result<UbuntuSession> {
        return try {
            val sessionId = UUID.randomUUID().toString()
            val sessionInfo = SessionInfo(
                id = sessionId,
                name = config.name,
                distroId = config.distro.id,
                createdAt = System.currentTimeMillis()
            )
            
            sessionRepository.saveSession(sessionInfo)
            
            val session = UbuntuSessionImpl(
                id = sessionId,
                config = config,
                sessionRepository = sessionRepository,
                nativeBridge = nativeBridge,
                rootfsManager = rootfsManager,
                proxyManager = proxyManager
            )

            Timber.d("Created session: $sessionId (${config.name})")
            Result.success(session)
        } catch (e: Exception) {
            Timber.e(e, "Failed to create session")
            Result.failure(e)
        }
    }

    override fun listSessions(): Flow<List<UbuntuSession>> {
        return sessionRepository.observeSessions().map { sessionInfos ->
            sessionInfos.map { info ->
                UbuntuSessionImpl(
                    id = info.id,
                    config = info.toConfig(),
                    sessionRepository = sessionRepository,
                    nativeBridge = nativeBridge,
                    rootfsManager = rootfsManager,
                    proxyManager = proxyManager,
                    initialState = info.state.toDomain()
                )
            }
        }
    }

    override suspend fun getSession(sessionId: String): UbuntuSession? {
        val sessionInfo = sessionRepository.loadSession(sessionId) ?: return null
        return UbuntuSessionImpl(
            id = sessionInfo.id,
            config = sessionInfo.toConfig(),
            sessionRepository = sessionRepository,
            nativeBridge = nativeBridge,
            rootfsManager = rootfsManager,
            proxyManager = proxyManager,
            initialState = sessionInfo.state.toDomain()
        )
    }

    override suspend fun deleteSession(sessionId: String): Result<Unit> {
        return try {
            val session = getSession(sessionId)
            session?.let {
                if (it.state is SessionState.Running) {
                    it.stop()
                }
            }
            sessionRepository.deleteSession(sessionId)
            Timber.d("Deleted session: $sessionId")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete session: $sessionId")
            Result.failure(e)
        }
    }

    override suspend fun startSession(sessionId: String): Result<Unit> {
        return try {
            val session = getSession(sessionId)
            if (session == null) {
                Result.failure(NoSuchElementException("Session not found: $sessionId"))
            } else {
                session.start()
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to start session: $sessionId")
            Result.failure(e)
        }
    }

    override suspend fun stopSession(sessionId: String): Result<Unit> {
        return try {
            val session = getSession(sessionId)
            if (session == null) {
                Result.failure(NoSuchElementException("Session not found: $sessionId"))
            } else {
                session.stop()
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to stop session: $sessionId")
            Result.failure(e)
        }
    }
}

class UbuntuSessionImpl(
    override val id: String,
    override val config: SessionConfig,
    private val sessionRepository: SessionRepository,
    private val nativeBridge: NativeBridge,
    private val rootfsManager: RootfsManager,
    private val proxyManager: ProxyManager,
    initialState: SessionState = SessionState.Created
) : UbuntuSession {

    // Use MutableStateFlow for immediate state updates to UI
    private val _stateFlow = MutableStateFlow(initialState)
    private var vncPort: Int = 5901
    private var rootfsPath: java.io.File? = null
    private var proxyUrl: String? = null

    override val state: SessionState get() = _stateFlow.value

    /**
     * Returns the actual OS PID of the running proot process.
     * Returns -1 if the session is not running or the PID cannot be determined.
     */
    override suspend fun getPid(): Long {
        if (_stateFlow.value !is SessionState.Running) {
            return -1L
        }
        return nativeBridge.getPid(id)
    }

    override val stateFlow: StateFlow<SessionState> = _stateFlow.asStateFlow()

    /**
     * Updates internal state immediately and persists to repository.
     * UI observers see the change instantly via stateFlow.
     */
    private suspend fun updateState(newState: SessionState) {
        _stateFlow.value = newState
        sessionRepository.updateSessionState(id, newState.toData())
    }

    override suspend fun start(): Result<Unit> {
        return try {
            Log.d(TAG, "=== UbuntuSessionImpl.start() called for session: $id ===")
            if (_stateFlow.value is SessionState.Running) {
                Log.d(TAG, "Session already running, returning failure")
                return Result.failure(IllegalStateException("Session already running"))
            }

            updateState(SessionState.Starting)

            Log.d(TAG, "Starting session: $id with distro ${config.distro.id}")
            Timber.d("Starting session: $id with distro ${config.distro.id}")

            // Check if rootfs is installed
            Log.d(TAG, "Checking if rootfs is installed...")
            if (!rootfsManager.isRootfsInstalled(config.distro)) {
                Log.d(TAG, "Rootfs not installed for ${config.distro.id}, installing...")
                Timber.d("Rootfs not installed for ${config.distro.id}, installing...")

                if (config.distro.bundled) {
                    // Install bundled rootfs
                    Log.d(TAG, "Installing bundled rootfs...")
                    val installResult = rootfsManager.installBundledRootfs(config.distro) { progress ->
                        Log.d(TAG, "Installing rootfs: $progress%")
                        Timber.d("Installing rootfs: $progress%")
                    }
                    if (installResult.isFailure) {
                        Log.e(TAG, "Failed to install rootfs: ${installResult.exceptionOrNull()?.message}")
                        throw installResult.exceptionOrNull() ?: Exception("Failed to install rootfs")
                    }
                    Log.d(TAG, "Bundled rootfs installed successfully")
                } else {
                    throw Exception("Rootfs not installed and distro is not bundled. Please download first.")
                }
            } else {
                Log.d(TAG, "Rootfs already installed")
            }

            rootfsPath = rootfsManager.getRootfsPath(config.distro)
            Log.d(TAG, "Using rootfs at: ${rootfsPath!!.absolutePath}")
            Timber.d("Using rootfs at: ${rootfsPath!!.absolutePath}")

            // Start proxy for network access
            Log.d(TAG, "Acquiring proxy for network access...")
            proxyUrl = proxyManager.acquire()
            if (proxyUrl != null) {
                Log.d(TAG, "Proxy acquired: $proxyUrl")
                Timber.d("Proxy acquired: $proxyUrl")
            } else {
                Log.w(TAG, "Failed to acquire proxy, continuing without network proxy")
                Timber.w("Failed to acquire proxy, continuing without network proxy")
            }

            // Build environment variables including proxy settings
            val envVars = mutableMapOf(
                "HOME" to "/root",
                "USER" to "root",
                "TERM" to "xterm-256color",
                "TMPDIR" to rootfsManager.getTempDir().absolutePath
            )
            proxyUrl?.let { proxy ->
                envVars["HTTP_PROXY"] = proxy
                envVars["http_proxy"] = proxy
                envVars["HTTPS_PROXY"] = proxy
                envVars["https_proxy"] = proxy
                envVars["NO_PROXY"] = "localhost,127.0.0.1"
                envVars["no_proxy"] = "localhost,127.0.0.1"
            }

            // Verify proot works with a quick test command
            Log.d(TAG, "Testing proot environment with 20s timeout...")
            Timber.d("Testing proot environment...")
            val testResult = nativeBridge.launchProot(
                sessionId = id,
                rootfsPath = rootfsPath!!.absolutePath,
                sessionDir = rootfsPath!!.absolutePath,
                bindMounts = emptyList(),
                envVars = envVars,
                command = "echo 'PRoot OK' && cat /etc/os-release | head -1 || echo 'Alpine Linux' && echo 'HTTP_PROXY=' \$HTTP_PROXY && echo 'Testing network...' && curl -s --max-time 5 -x \$HTTP_PROXY http://httpbin.org/ip 2>&1 | head -5 || echo 'Network test failed'",
                timeoutSeconds = 20
            )

            Log.d(TAG, "PRoot test result: exitCode=${testResult.exitCode}, stdout=${testResult.stdout.take(100)}, stderr=${testResult.stderr.take(100)}")

            if (testResult.exitCode != 0 && testResult.stderr.isNotEmpty()) {
                Log.e(TAG, "PRoot test failed: ${testResult.stderr}")
                Timber.e("PRoot test failed: ${testResult.stderr}")
                throw Exception("PRoot initialization failed: ${testResult.stderr}")
            }

            Log.d(TAG, "PRoot test successful: ${testResult.stdout.trim()}")
            Timber.d("PRoot test successful: ${testResult.stdout.trim()}")

            vncPort = 5901

            updateState(SessionState.Running(vncPort))

            Log.d(TAG, "Session started successfully: $id")
            Timber.d("Session started successfully: $id")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start session: $id - ${e.message}", e)
            Timber.e(e, "Failed to start session: $id")
            updateState(SessionState.Error(e.message ?: "Unknown error"))
            Result.failure(e)
        }
    }

    override suspend fun stop(): Result<Unit> {
        return try {
            if (_stateFlow.value !is SessionState.Running) {
                return Result.failure(IllegalStateException("Session not running"))
            }

            updateState(SessionState.Stopping)

            Log.d(TAG, "Stopping session: $id with child process cleanup")
            Timber.d("Stopping session: $id with child process cleanup")

            // Kill the proot process and all its children to prevent orphans.
            // This handles services started with nohup, background jobs, etc.
            val path = rootfsPath
            if (path != null) {
                nativeBridge.killProotWithChildren(
                    sessionId = id,
                    rootfsPath = path.absolutePath,
                    sessionDir = path.absolutePath
                )
            } else {
                // Fallback to simple kill if rootfs path is not available
                Log.w(TAG, "Rootfs path not available, falling back to simple kill")
                nativeBridge.killProot(id, 15) // SIGTERM
            }

            // Release proxy reference
            if (proxyUrl != null) {
                Log.d(TAG, "Releasing proxy reference")
                proxyManager.release()
                proxyUrl = null
            }

            updateState(SessionState.Stopped)

            Log.d(TAG, "Session stopped: $id")
            Timber.d("Session stopped: $id")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop session: $id - ${e.message}", e)
            Timber.e(e, "Failed to stop session: $id")
            updateState(SessionState.Error(e.message ?: "Unknown error"))
            Result.failure(e)
        }
    }

    /**
     * Build environment variables map including proxy settings if available.
     */
    private fun buildEnvVars(): Map<String, String> {
        val env = mutableMapOf(
            "HOME" to "/home/udroid",
            "USER" to "udroid",
            "TERM" to "xterm-256color",
            "PATH" to "/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin",
            "TMPDIR" to rootfsManager.getTempDir().absolutePath
        )
        proxyUrl?.let { proxy ->
            env["HTTP_PROXY"] = proxy
            env["http_proxy"] = proxy
            env["HTTPS_PROXY"] = proxy
            env["https_proxy"] = proxy
            env["NO_PROXY"] = "localhost,127.0.0.1"
            env["no_proxy"] = "localhost,127.0.0.1"
        }
        return env
    }

    override suspend fun exec(command: String, timeoutSeconds: Long): Result<ProcessResult> {
        return try {
            if (_stateFlow.value !is SessionState.Running) {
                return Result.failure(IllegalStateException("Session not running"))
            }

            val path = rootfsPath ?: return Result.failure(IllegalStateException("Rootfs path not set"))

            Timber.d("Executing command in session $id: $command")

            val result = nativeBridge.launchProot(
                sessionId = id,
                rootfsPath = path.absolutePath,
                sessionDir = path.absolutePath,
                bindMounts = emptyList(),
                envVars = buildEnvVars(),
                command = command,
                timeoutSeconds = timeoutSeconds
            )

            Timber.d("Command result: exit=${result.exitCode}, stdout=${result.stdout.take(100)}")
            Result.success(result)
        } catch (e: Exception) {
            Timber.e(e, "Failed to execute command in session $id")
            Result.failure(e)
        }
    }

    override suspend fun execInteractive(
        command: String,
        stdinInput: String?,
        timeoutSeconds: Long
    ): Result<ProcessResult> {
        return try {
            if (_stateFlow.value !is SessionState.Running) {
                return Result.failure(IllegalStateException("Session not running"))
            }

            val path = rootfsPath ?: return Result.failure(IllegalStateException("Rootfs path not set"))

            Timber.d("Executing interactive command in session $id: $command, stdinInput=${stdinInput?.take(50)}")

            val result = nativeBridge.launchProotInteractive(
                sessionId = id,
                rootfsPath = path.absolutePath,
                sessionDir = path.absolutePath,
                bindMounts = emptyList(),
                envVars = buildEnvVars(),
                command = command,
                stdinInput = stdinInput,
                timeoutSeconds = timeoutSeconds
            )

            Timber.d("Interactive command result: exit=${result.exitCode}, stdout=${result.stdout.take(100)}")
            Result.success(result)
        } catch (e: Exception) {
            Timber.e(e, "Failed to execute interactive command in session $id")
            Result.failure(e)
        }
    }
}
