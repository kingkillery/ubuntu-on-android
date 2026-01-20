package com.udroid.app.agent

import kotlinx.serialization.Serializable

/**
 * Configuration for agent tools in a session.
 */
@Serializable
data class AgentConfig(
    val anthropicApiKey: String? = null,
    val openaiApiKey: String? = null,
    val geminiApiKey: String? = null,
    val defaultModel: String = "claude-sonnet-4-20250514",
    val maxTokens: Int = 4096,
    val temperature: Double = 0.7,
    val enableShell: Boolean = true,
    val enableFilesystem: Boolean = true,
    val enableNetwork: Boolean = true
)

/**
 * Status of agent tools installation in a session.
 */
sealed class AgentInstallStatus {
    data object NotInstalled : AgentInstallStatus()
    data object Installing : AgentInstallStatus()
    data class Installed(val version: String? = null) : AgentInstallStatus()
    data class Failed(val error: String) : AgentInstallStatus()
}

/**
 * Result of running an agent task.
 */
data class AgentTaskResult(
    val success: Boolean,
    val output: String,
    val error: String? = null,
    val exitCode: Int = 0,
    val durationMs: Long = 0
)
