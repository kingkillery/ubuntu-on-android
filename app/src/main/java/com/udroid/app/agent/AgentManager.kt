package com.udroid.app.agent

import android.content.Context
import com.udroid.app.session.UbuntuSessionManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages agent tools installation and execution within Ubuntu sessions.
 * Provides high-level API for:
 * - Installing pk-puzldai, droid, and gemini CLI tools
 * - Checking installation status
 * - Running agent tasks
 */
@Singleton
class AgentManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessionManager: UbuntuSessionManager
) {

    // Track installation status per session
    private val _installStatus = MutableStateFlow<Map<String, AgentInstallStatus>>(emptyMap())
    val installStatus: StateFlow<Map<String, AgentInstallStatus>> = _installStatus.asStateFlow()

    /**
     * Check if agent tools are installed in a session.
     */
    suspend fun isAgentInstalled(sessionId: String): Boolean {
        val session = sessionManager.getSession(sessionId) ?: return false

        return try {
            val result = session.exec("test -f /home/udroid/.agent-tools-installed && echo 'installed'", timeoutSeconds = 10)
            result.getOrNull()?.stdout?.trim() == "installed"
        } catch (e: Exception) {
            Timber.e(e, "Failed to check agent installation status")
            false
        }
    }

    /**
     * Get the current installation status for a session.
     */
    fun getInstallStatus(sessionId: String): AgentInstallStatus {
        return _installStatus.value[sessionId] ?: AgentInstallStatus.NotInstalled
    }

    /**
     * Install agent tools in a session.
     * Copies the bootstrap script from assets and executes it.
     *
     * @param sessionId The session to install in
     * @param onProgress Callback for installation progress updates
     * @return Result indicating success or failure
     */
    suspend fun installAgent(
        sessionId: String,
        onProgress: (String) -> Unit = {}
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val session = sessionManager.getSession(sessionId)
        if (session == null) {
            return@withContext Result.failure(IllegalStateException("Session not found: $sessionId"))
        }

        updateStatus(sessionId, AgentInstallStatus.Installing)
        onProgress("Starting agent tools installation...")

        try {
            // Read the install script from assets
            val scriptContent = readAssetFile("scripts/install-pk-puzldai.sh")
            if (scriptContent.isEmpty()) {
                throw IllegalStateException("Install script not found in assets")
            }

            onProgress("Copying install script to session...")

            // Create a temporary script file in the session
            val createScriptResult = session.exec(
                "cat > /tmp/install-agent.sh << 'SCRIPT_EOF'\n$scriptContent\nSCRIPT_EOF",
                timeoutSeconds = 30
            )
            if (createScriptResult.isFailure) {
                throw createScriptResult.exceptionOrNull() ?: Exception("Failed to create script")
            }

            // Make it executable
            session.exec("chmod +x /tmp/install-agent.sh", timeoutSeconds = 10)

            onProgress("Running installation (this may take a few minutes)...")

            // Execute the install script with a long timeout
            val installResult = session.exec(
                "/tmp/install-agent.sh 2>&1",
                timeoutSeconds = 600 // 10 minutes for installation
            )

            installResult.fold(
                onSuccess = { result ->
                    if (result.exitCode == 0) {
                        onProgress("Installation completed successfully!")
                        updateStatus(sessionId, AgentInstallStatus.Installed())
                        Result.success(Unit)
                    } else {
                        val errorMsg = result.stderr.ifEmpty { result.stdout }
                        onProgress("Installation failed: $errorMsg")
                        updateStatus(sessionId, AgentInstallStatus.Failed(errorMsg))
                        Result.failure(Exception("Installation failed: $errorMsg"))
                    }
                },
                onFailure = { error ->
                    onProgress("Installation failed: ${error.message}")
                    updateStatus(sessionId, AgentInstallStatus.Failed(error.message ?: "Unknown error"))
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to install agent tools")
            onProgress("Installation failed: ${e.message}")
            updateStatus(sessionId, AgentInstallStatus.Failed(e.message ?: "Unknown error"))
            Result.failure(e)
        }
    }

    /**
     * Run an agent task in a session using pk-puzldai or agent-run.
     *
     * @param sessionId The session to run in
     * @param task The task description or command
     * @param config Optional agent configuration
     * @param timeoutSeconds Maximum time for task execution
     * @return AgentTaskResult with output and status
     */
    suspend fun runAgentTask(
        sessionId: String,
        task: String,
        config: AgentConfig? = null,
        timeoutSeconds: Long = 300
    ): AgentTaskResult = withContext(Dispatchers.IO) {
        val session = sessionManager.getSession(sessionId)
        if (session == null) {
            return@withContext AgentTaskResult(
                success = false,
                output = "",
                error = "Session not found: $sessionId"
            )
        }

        val startTime = System.currentTimeMillis()

        try {
            // Build command with secure environment variable injection
            val baseCommand = "agent-run '$task'"
            val command = buildSecureEnvCommand(config, baseCommand)

            val result = session.exec(command, timeoutSeconds = timeoutSeconds)

            result.fold(
                onSuccess = { processResult ->
                    AgentTaskResult(
                        success = processResult.exitCode == 0,
                        output = processResult.stdout,
                        error = processResult.stderr.ifEmpty { null },
                        exitCode = processResult.exitCode,
                        durationMs = System.currentTimeMillis() - startTime
                    )
                },
                onFailure = { error ->
                    AgentTaskResult(
                        success = false,
                        output = "",
                        error = error.message,
                        exitCode = -1,
                        durationMs = System.currentTimeMillis() - startTime
                    )
                }
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to run agent task")
            AgentTaskResult(
                success = false,
                output = "",
                error = e.message,
                exitCode = -1,
                durationMs = System.currentTimeMillis() - startTime
            )
        }
    }

    /**
     * Run a Gemini query in a session.
     */
    suspend fun runGeminiQuery(
        sessionId: String,
        query: String,
        apiKey: String? = null,
        timeoutSeconds: Long = 60
    ): AgentTaskResult = withContext(Dispatchers.IO) {
        val session = sessionManager.getSession(sessionId)
        if (session == null) {
            return@withContext AgentTaskResult(
                success = false,
                output = "",
                error = "Session not found: $sessionId"
            )
        }

        val startTime = System.currentTimeMillis()

        try {
            // Build command with secure API key injection if provided
            val baseCommand = "gemini '$query'"
            val command = if (apiKey != null) {
                val config = AgentConfig(geminiApiKey = apiKey)
                buildSecureEnvCommand(config, baseCommand)
            } else {
                baseCommand
            }

            val result = session.exec(command, timeoutSeconds = timeoutSeconds)

            result.fold(
                onSuccess = { processResult ->
                    AgentTaskResult(
                        success = processResult.exitCode == 0,
                        output = processResult.stdout,
                        error = processResult.stderr.ifEmpty { null },
                        exitCode = processResult.exitCode,
                        durationMs = System.currentTimeMillis() - startTime
                    )
                },
                onFailure = { error ->
                    AgentTaskResult(
                        success = false,
                        output = "",
                        error = error.message,
                        durationMs = System.currentTimeMillis() - startTime
                    )
                }
            )
        } catch (e: Exception) {
            AgentTaskResult(
                success = false,
                output = "",
                error = e.message,
                durationMs = System.currentTimeMillis() - startTime
            )
        }
    }

    /**
     * Run a droid CLI command in a session.
     */
    suspend fun runDroidCommand(
        sessionId: String,
        command: String,
        timeoutSeconds: Long = 120
    ): AgentTaskResult = withContext(Dispatchers.IO) {
        val session = sessionManager.getSession(sessionId)
        if (session == null) {
            return@withContext AgentTaskResult(
                success = false,
                output = "",
                error = "Session not found: $sessionId"
            )
        }

        val startTime = System.currentTimeMillis()

        try {
            val fullCommand = "droid $command"
            val result = session.exec(fullCommand, timeoutSeconds = timeoutSeconds)

            result.fold(
                onSuccess = { processResult ->
                    AgentTaskResult(
                        success = processResult.exitCode == 0,
                        output = processResult.stdout,
                        error = processResult.stderr.ifEmpty { null },
                        exitCode = processResult.exitCode,
                        durationMs = System.currentTimeMillis() - startTime
                    )
                },
                onFailure = { error ->
                    AgentTaskResult(
                        success = false,
                        output = "",
                        error = error.message,
                        durationMs = System.currentTimeMillis() - startTime
                    )
                }
            )
        } catch (e: Exception) {
            AgentTaskResult(
                success = false,
                output = "",
                error = e.message,
                durationMs = System.currentTimeMillis() - startTime
            )
        }
    }

    /**
     * Read a file from app assets.
     */
    private fun readAssetFile(path: String): String {
        return try {
            context.assets.open(path).use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    reader.readText()
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to read asset: $path")
            ""
        }
    }

    /**
     * Escape a string for safe use in single-quoted shell context.
     * Handles single quotes by ending the quote, adding escaped quote, resuming quote.
     * Example: "it's" becomes 'it'\''s'
     */
    private fun shellEscape(value: String): String {
        // Replace ' with '\'' (end quote, escaped quote, start quote)
        return value.replace("'", "'\\''")
    }

    /**
     * Build a command that securely sets environment variables from a temp file.
     * This avoids exposing API keys in the process list (ps aux).
     *
     * Creates a temp file with restricted permissions (600), sources it, runs the
     * command, then deletes the file - all in a single shell execution.
     *
     * API keys are properly shell-escaped to prevent injection attacks.
     */
    private fun buildSecureEnvCommand(config: AgentConfig?, command: String): String {
        if (config == null) return command

        val envLines = mutableListOf<String>()
        // Shell-escape each API key to prevent injection via special characters
        config.anthropicApiKey?.let { envLines.add("export ANTHROPIC_API_KEY='${shellEscape(it)}'") }
        config.openaiApiKey?.let { envLines.add("export OPENAI_API_KEY='${shellEscape(it)}'") }
        config.geminiApiKey?.let { envLines.add("export GEMINI_API_KEY='${shellEscape(it)}'") }

        if (envLines.isEmpty()) return command

        // Create temp file, set restrictive permissions, write env vars, source it,
        // run command, then delete - all atomically to minimize exposure window
        val envContent = envLines.joinToString("\n")
        return """
            _env_file=$(mktemp /tmp/.agent_env_XXXXXX) && \
            chmod 600 "${'$'}_env_file" && \
            cat > "${'$'}_env_file" << '_ENV_EOF_'
$envContent
_ENV_EOF_
            . "${'$'}_env_file" && \
            rm -f "${'$'}_env_file" && \
            $command
        """.trimIndent().replace("\n            ", " ")
    }

    /**
     * Build environment variable prefix for agent commands.
     * @deprecated Use buildSecureEnvCommand instead to avoid API key exposure in process list
     */
    @Suppress("unused")
    @Deprecated("Use buildSecureEnvCommand instead", ReplaceWith("buildSecureEnvCommand(config, command)"))
    private fun buildEnvPrefix(config: AgentConfig?): String {
        if (config == null) return ""

        val envVars = mutableListOf<String>()
        // Shell-escape for safety even in deprecated method
        config.anthropicApiKey?.let { envVars.add("ANTHROPIC_API_KEY='${shellEscape(it)}'") }
        config.openaiApiKey?.let { envVars.add("OPENAI_API_KEY='${shellEscape(it)}'") }
        config.geminiApiKey?.let { envVars.add("GEMINI_API_KEY='${shellEscape(it)}'") }

        return if (envVars.isNotEmpty()) envVars.joinToString(" ") + " " else ""
    }

    /**
     * Update installation status for a session.
     */
    private fun updateStatus(sessionId: String, status: AgentInstallStatus) {
        _installStatus.value = _installStatus.value.toMutableMap().apply {
            put(sessionId, status)
        }
    }
}
