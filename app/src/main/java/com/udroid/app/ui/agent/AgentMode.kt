package com.udroid.app.ui.agent

/**
 * Agentic interface mode for different user types.
 */
enum class AgentMode(val displayName: String, val description: String, val icon: String) {
    BASIC(
        displayName = "Basic Mode",
        description = "Simplified AI-powered interface. Just describe what you want to do.",
        icon = "Chat"
    ),
    ADVANCED(
        displayName = "Advanced Mode",
        description = "Full control with tools, commands, and detailed output.",
        icon = "Terminal"
    );

    companion object {
        fun fromIcon(icon: String): AgentMode? = entries.find { it.icon == icon }
    }
}

/**
 * Predefined task categories for Basic Mode
 */
enum class PresetCategory(
    val displayName: String,
    val description: String,
    val icon: String,
    val color: String
) {
    SYSTEM(
        displayName = "System",
        description = "System info, diagnostics, and maintenance",
        icon = "Settings",
        color = "#4CAF50"
    ),
    SECURITY(
        displayName = "Security",
        description = "Scan, harden, and secure the system",
        icon = "Security",
        color = "#FF5722"
    ),
    OPTIMIZATION(
        displayName = "Optimize",
        description = "Performance tuning and cleanup",
        icon = "Speed",
        color = "#FF9800"
    ),
    DEVELOPMENT(
        displayName = "Development",
        description = "Setup dev tools and environments",
        icon = "Code",
        color = "#2196F3"
    ),
    NETWORK(
        displayName = "Network",
        description = "Network diagnostics and configuration",
        icon = "Wifi",
        color = "#03A9F4"
    ),
    CUSTOM(
        displayName = "Custom",
        description = "Describe your own custom task",
        icon = "Edit",
        color = "#9C27B0"
    );
}

/**
 * Predefined tasks for Basic Mode
 */
data class PresetTask(
    val id: String,
    val name: String,
    val description: String,
    val category: PresetCategory,
    val templatePrompt: String,
    val estimatedTime: String
)

/**
 * Repository of preset tasks
 */
object PresetTasks {
    val tasks = listOf(        PresetTask(
            id = "sys_info",
            name = "System Information",
            description = "Get comprehensive system info",
            category = PresetCategory.SYSTEM,
            templatePrompt = "Show me a complete system overview including OS version, kernel, memory, CPU, disk space, and running services.",
            estimatedTime = "~30s"
        ),
        PresetTask(
            id = "sys_update",
            name = "Check Updates",
            description = "Check for available package updates",
            category = PresetCategory.SYSTEM,
            templatePrompt = "Check for available system updates and show what can be upgraded.",
            estimatedTime = "~20s"
        ),
        PresetTask(
            id = "sec_scan",
            name = "Security Scan",
            description = "Scan for security vulnerabilities",
            category = PresetCategory.SECURITY,
            templatePrompt = "Scan the system for security vulnerabilities, check for open ports, and review authentication configuration.",
            estimatedTime = "~60s"
        ),
        PresetTask(
            id = "sec_hardening",
            name = "Security Hardening",
            description = "Apply basic security hardening",
            category = PresetCategory.SECURITY,
            templatePrompt = "Apply basic security hardening including firewall setup, SSH key configuration, and fail2ban setup.",
            estimatedTime = "~90s"
        ),
        PresetTask(
            id = "opt_cleanup",
            name = "System Cleanup",
            description = "Clean temporary files and caches",
            category = PresetCategory.OPTIMIZATION,
            templatePrompt = "Clean temporary files, package caches, and old logs to free up disk space.",
            estimatedTime = "~30s"
        ),
        PresetTask(
            id = "opt_autoremove",
            name = "Auto-Remove Unused",
            description = "Remove unused packages and dependencies",
            category = PresetCategory.OPTIMIZATION,
            templatePrompt = "Remove unused packages, auto-remove orphaned dependencies, and clean the package cache.",
            estimatedTime = "~45s"
        ),
        PresetTask(
            id = "dev_python",
            name = "Setup Python Environment",
            description = "Install Python and dev tools",
            category = PresetCategory.DEVELOPMENT,
            templatePrompt = "Install Python 3, pip, and common development tools like git, vim, and build essentials.",
            estimatedTime = "~2min"
        ),
        PresetTask(
            id = "dev_nodejs",
            name = "Setup Node.js Environment",
            description = "Install Node.js and npm",
            category = PresetCategory.DEVELOPMENT,
            templatePrompt = "Install Node.js, npm, and common development tools.",
            estimatedTime = "~2min"
        ),
        PresetTask(
            id = "dev_docker",
            name = "Setup Docker",
            description = "Install Docker container runtime",
            category = PresetCategory.DEVELOPMENT,
            templatePrompt = "Install Docker, Docker Compose, and configure basic containers.",
            estimatedTime = "~3min"
        ),
        PresetTask(
            id = "net_diag",
            name = "Network Diagnostics",
            description = "Test network connectivity",
            category = PresetCategory.NETWORK,
            templatePrompt = "Test network connectivity, check DNS resolution, measure speed, and identify any issues.",
            estimatedTime = "~30s"
        ),
        PresetTask(
            id = "net_ports",
            name = "Open Ports Scan",
            description = "Scan for open network ports",
            category = PresetCategory.NETWORK,
            templatePrompt = "Scan for open network ports, identify listening services, and flag any security concerns.",
            estimatedTime = "~20s"
        ),
        PresetTask(
            id = "custom",
            name = "Custom Task",
            description = "Describe your custom task",
            category = PresetCategory.CUSTOM,
            templatePrompt = "",
            estimatedTime = "Varies"
        )
    )

    fun getById(id: String): PresetTask? = tasks.find { it.id == id }
    fun getByCategory(category: PresetCategory): List<PresetTask> = tasks.filter { it.category == category }
}
