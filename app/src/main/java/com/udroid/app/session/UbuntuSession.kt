package com.udroid.app.session

import com.udroid.app.model.DistroVariant
import com.udroid.app.model.DesktopEnvironment
import com.udroid.app.model.ProcessResult
import com.udroid.app.model.SessionState
import kotlinx.coroutines.flow.Flow

data class SessionConfig(
    val name: String,
    val distro: DistroVariant,
    val desktopEnvironment: DesktopEnvironment,
    val enableSound: Boolean = true,
    val enableNetwork: Boolean = true
)

interface UbuntuSessionManager {
    suspend fun createSession(config: SessionConfig): Result<UbuntuSession>
    fun listSessions(): Flow<List<UbuntuSession>>
    suspend fun getSession(sessionId: String): UbuntuSession?
    suspend fun deleteSession(sessionId: String): Result<Unit>
    suspend fun startSession(sessionId: String): Result<Unit>
    suspend fun stopSession(sessionId: String): Result<Unit>
}

interface UbuntuSession {
    val id: String
    val config: SessionConfig
    val state: SessionState
    val stateFlow: Flow<SessionState>

    suspend fun start(): Result<Unit>
    suspend fun stop(): Result<Unit>
    suspend fun exec(command: String): Result<ProcessResult>
}
