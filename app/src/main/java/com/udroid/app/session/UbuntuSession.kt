package com.udroid.app.session

import com.udroid.app.model.DistroVariant
import com.udroid.app.model.DesktopEnvironment
import com.udroid.app.model.ProcessResult
import com.udroid.app.model.SessionState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

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
    val stateFlow: StateFlow<SessionState>

    /**
     * Returns the actual OS PID of the running proot process.
     * Returns -1 if the session is not running or the PID cannot be determined.
     */
    suspend fun getPid(): Long

    suspend fun start(): Result<Unit>
    suspend fun stop(): Result<Unit>

    /**
     * Executes a command in the running session.
     *
     * @param command The command to execute
     * @param timeoutSeconds Maximum time to wait for command completion.
     *        Use 0 or negative value to wait indefinitely (no timeout).
     *        Default is 300 seconds (5 minutes).
     * @return Result containing ProcessResult on success, or failure with exception
     */
    suspend fun exec(command: String, timeoutSeconds: Long = 300): Result<ProcessResult>

    /**
     * Executes a command in the running session with stdin input support.
     * This is useful for interactive commands that require input piped to them,
     * such as REPL sessions, password prompts, or commands that read from stdin.
     *
     * @param command The command to execute
     * @param stdinInput Input to pipe to the command's stdin. Can be null for no input.
     * @param timeoutSeconds Maximum time to wait for command completion.
     *        Use 0 or negative value to wait indefinitely (no timeout).
     *        Default is 300 seconds (5 minutes).
     * @return Result containing ProcessResult on success, or failure with exception
     */
    suspend fun execInteractive(
        command: String,
        stdinInput: String?,
        timeoutSeconds: Long = 300
    ): Result<ProcessResult>
}
