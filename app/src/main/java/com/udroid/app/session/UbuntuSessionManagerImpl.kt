package com.udroid.app.session

import com.udroid.app.model.ProcessResult
import com.udroid.app.model.SessionState
import com.udroid.app.nativebridge.NativeBridge
import com.udroid.app.rootfs.RootfsManager
import com.udroid.app.storage.SessionInfo
import com.udroid.app.storage.SessionRepository
import com.udroid.app.storage.toData
import com.udroid.app.storage.toDomain
import kotlinx.coroutines.flow.Flow
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
    private val rootfsManager: RootfsManager
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
                rootfsManager = rootfsManager
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
                    rootfsManager = rootfsManager
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
            rootfsManager = rootfsManager
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
    private val rootfsManager: RootfsManager
) : UbuntuSession {

    private var _state: SessionState = SessionState.Created
    private var vncPort: Int = 5901
    private var rootfsPath: java.io.File? = null

    override val state: SessionState get() = _state

    /**
     * Returns the actual OS PID of the running proot process.
     * Returns -1 if the session is not running or the PID cannot be determined.
     */
    override suspend fun getPid(): Long {
        if (_state !is SessionState.Running) {
            return -1L
        }
        return nativeBridge.getPid(id)
    }

    override val stateFlow: Flow<SessionState> =
        sessionRepository.observeSessions()
            .map { sessions ->
                sessions.find { it.id == this.id }?.state?.toDomain() ?: SessionState.Created
            }

    override suspend fun start(): Result<Unit> {
        return try {
            Log.d(TAG, "=== UbuntuSessionImpl.start() called for session: $id ===")
            if (_state is SessionState.Running) {
                Log.d(TAG, "Session already running, returning failure")
                return Result.failure(IllegalStateException("Session already running"))
            }

            _state = SessionState.Starting
            sessionRepository.updateSessionState(id, _state.toData())

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

            // Verify proot works with a quick test command
            Log.d(TAG, "Testing proot environment with 10s timeout...")
            Timber.d("Testing proot environment...")
            val testResult = nativeBridge.launchProot(
                sessionId = id,
                rootfsPath = rootfsPath!!.absolutePath,
                sessionDir = rootfsPath!!.absolutePath,
                bindMounts = emptyList(),
                envVars = mapOf(
                    "HOME" to "/root",
                    "USER" to "root",
                    "TERM" to "xterm-256color"
                ),
                command = "echo 'PRoot OK' && cat /etc/os-release | head -1 || echo 'Alpine Linux'",
                timeoutSeconds = 10
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

            _state = SessionState.Running(vncPort)
            sessionRepository.updateSessionState(id, _state.toData())

            Log.d(TAG, "Session started successfully: $id")
            Timber.d("Session started successfully: $id")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start session: $id - ${e.message}", e)
            Timber.e(e, "Failed to start session: $id")
            _state = SessionState.Error(e.message ?: "Unknown error")
            sessionRepository.updateSessionState(id, _state.toData())
            Result.failure(e)
        }
    }

    override suspend fun stop(): Result<Unit> {
        return try {
            if (_state !is SessionState.Running) {
                return Result.failure(IllegalStateException("Session not running"))
            }

            _state = SessionState.Stopping
            sessionRepository.updateSessionState(id, _state.toData())
            
            Timber.d("Stopping session: $id")

            // Kill the proot process for this session
            nativeBridge.killProot(id, 15) // SIGTERM
            
            _state = SessionState.Stopped
            sessionRepository.updateSessionState(id, _state.toData())
            
            Timber.d("Session stopped: $id")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to stop session: $id")
            _state = SessionState.Error(e.message ?: "Unknown error")
            sessionRepository.updateSessionState(id, _state.toData())
            Result.failure(e)
        }
    }

    override suspend fun exec(command: String, timeoutSeconds: Long): Result<ProcessResult> {
        return try {
            if (_state !is SessionState.Running) {
                return Result.failure(IllegalStateException("Session not running"))
            }

            val path = rootfsPath ?: return Result.failure(IllegalStateException("Rootfs path not set"))

            Timber.d("Executing command in session $id: $command")

            val result = nativeBridge.launchProot(
                sessionId = id,
                rootfsPath = path.absolutePath,
                sessionDir = path.absolutePath,
                bindMounts = emptyList(),
                envVars = mapOf(
                    "HOME" to "/home/udroid",
                    "USER" to "udroid",
                    "TERM" to "xterm-256color",
                    "PATH" to "/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"
                ),
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
}
