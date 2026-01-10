package com.udroid.app.session

import com.udroid.app.model.ProcessResult
import com.udroid.app.model.SessionState
import com.udroid.app.nativebridge.NativeBridge
import com.udroid.app.storage.SessionInfo
import com.udroid.app.storage.SessionRepository
import com.udroid.app.storage.toData
import com.udroid.app.storage.toDomain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UbuntuSessionManagerImpl @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val nativeBridge: NativeBridge
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
                nativeBridge = nativeBridge
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
                    nativeBridge = nativeBridge
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
            nativeBridge = nativeBridge
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

class UbuntuSessionImpl @Inject constructor(
    override val id: String,
    override val config: SessionConfig,
    private val sessionRepository: SessionRepository,
    private val nativeBridge: NativeBridge
) : UbuntuSession {
    
    private var _state: SessionState = SessionState.Created
    private var processPid: Long? = null
    private var vncPort: Int = 5901

    override val state: SessionState get() = _state

    override val stateFlow: Flow<SessionState> =
        sessionRepository.observeSessions()
            .map { sessions ->
                sessions.find { it.id == this.id }?.state?.toDomain() ?: SessionState.Created
            }

    override suspend fun start(): Result<Unit> {
        return try {
            if (_state is SessionState.Running) {
                return Result.failure(IllegalStateException("Session already running"))
            }

            _state = SessionState.Starting
            
            Timber.d("Starting session: $id")
            
            // TODO: Implement actual PRoot launch
            // For now, simulate startup
            processPid = System.currentTimeMillis()
            vncPort = 5901
            
            _state = SessionState.Running(vncPort)
            sessionRepository.updateSessionState(id, _state.toData())
            
            Timber.d("Session started: $id (VNC port: $vncPort)")
            Result.success(Unit)
        } catch (e: Exception) {
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
            
            processPid?.let { pid ->
                nativeBridge.killProot(pid, 15) // SIGTERM
            }
            
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

    override suspend fun exec(command: String): Result<ProcessResult> {
        return try {
            if (_state !is SessionState.Running) {
                return Result.failure(IllegalStateException("Session not running"))
            }

            Timber.d("Executing command in session $id: $command")
            
            // TODO: Implement actual command execution
            val result = ProcessResult(
                exitCode = 0,
                stdout = "Command executed (stub): $command",
                stderr = ""
            )
            
            Result.success(result)
        } catch (e: Exception) {
            Timber.e(e, "Failed to execute command in session $id")
            Result.failure(e)
        }
    }
}
