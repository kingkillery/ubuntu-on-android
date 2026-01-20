package com.udroid.app.devbox

import java.util.UUID

data class ServiceTemplate(
    val id: String,
    val displayName: String,
    val description: String,
    val defaultPort: Int,
    val startCommand: String,
    val healthCheck: String,
    val installCommand: String? = null,
    val checkCommand: String? = null, // Command to check if binary exists/is installed
    val isCustom: Boolean = false
) {
    companion object {
        val SSH = ServiceTemplate(
            id = "ssh",
            displayName = "SSH Server",
            description = "Remote shell access via SSH",
            defaultPort = 8022,
            startCommand = "dropbear -F -E -p 0.0.0.0:8022",
            healthCheck = "nc -z 127.0.0.1 8022",
            checkCommand = "which dropbear",
            installCommand = "apt-get update && apt-get install -y dropbear"
        )

        val JUPYTER = ServiceTemplate(
            id = "jupyter",
            displayName = "Jupyter Lab",
            description = "Interactive Python notebooks",
            defaultPort = 8888,
            startCommand = "jupyter lab --ip=0.0.0.0 --port=8888 --no-browser --allow-root --NotebookApp.token=''",
            healthCheck = "nc -z 127.0.0.1 8888",
            checkCommand = "which jupyter",
            installCommand = "apt-get update && apt-get install -y python3-pip && pip3 install jupyterlab"
        )

        val HTTP_SERVER = ServiceTemplate(
            id = "http",
            displayName = "HTTP Server",
            description = "Simple web server for file sharing",
            defaultPort = 8000,
            startCommand = "python3 -m http.server 8000 --bind 0.0.0.0",
            healthCheck = "nc -z 127.0.0.1 8000",
            checkCommand = "which python3",
            installCommand = "apt-get update && apt-get install -y python3"
        )
        
        val NGINX = ServiceTemplate(
            id = "nginx",
            displayName = "Nginx Web Server",
            description = "High performance web server",
            defaultPort = 8080,
            startCommand = "nginx -g 'daemon off;' -c /etc/nginx/nginx.conf",
            healthCheck = "nc -z 127.0.0.1 8080",
            checkCommand = "which nginx",
            installCommand = "apt-get update && apt-get install -y nginx"
        )
        
        val NODEJS = ServiceTemplate(
            id = "nodejs",
            displayName = "Node.js HTTP Server",
            description = "JavaScript runtime with HTTP server",
            defaultPort = 3000,
            startCommand = "npx --yes http-server -p 3000 -a 0.0.0.0",
            healthCheck = "nc -z 127.0.0.1 3000",
            checkCommand = "which node",
            installCommand = "apt-get update && apt-get install -y nodejs npm"
        )

        val AGENT_TOOLS = ServiceTemplate(
            id = "agent",
            displayName = "AI Agent Tools",
            description = "pk-puzldai, factory.ai droid, and Gemini CLI for AI orchestration",
            defaultPort = 0, // No port needed for agent tools
            startCommand = "", // Agent tools are CLI-based, not a daemon
            healthCheck = "test -f /opt/agent-tools/venv/bin/pk-puzldai && test -f /opt/agent-tools/venv/bin/droid && test -f /opt/agent-tools/venv/bin/gemini",
            checkCommand = "test -f /usr/local/bin/pk-puzldai",
            installCommand = "/data/data/com.udroid.app/files/scripts/install-pk-puzldai.sh"
        )

        val PRESETS = listOf(SSH, JUPYTER, HTTP_SERVER, NGINX, NODEJS, AGENT_TOOLS)

        fun fromId(id: String): ServiceTemplate? =
            PRESETS.find { it.id == id }
    }
}

data class DevServiceInstance(
    val id: String = UUID.randomUUID().toString(),
    val template: ServiceTemplate,
    val sessionId: String,
    val port: Int = template.defaultPort,
    val bindMode: BindMode = BindMode.LAN,
    val state: ServiceState = ServiceState.Stopped
)

enum class BindMode(val displayName: String, val bindAddress: String) {
    DEVICE_ONLY("Device only", "127.0.0.1"),
    LAN("LAN", "0.0.0.0")
}

sealed class ServiceState {
    data object Stopped : ServiceState()
    data object Starting : ServiceState()
    data object Installing : ServiceState()
    data class Running(val startedAt: Long = System.currentTimeMillis()) : ServiceState()
    data class Error(val message: String) : ServiceState()
}

data class ConnectInfo(
    val displayText: String,
    val copyText: String,
    val qrContent: String,
    val lanAddress: String,
    val port: Int
)

data class LogEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val serviceId: String,
    val level: LogLevel,
    val message: String
)

enum class LogLevel {
    INFO, WARN, ERROR
}

/**
 * Data model for devbox sharing state.
 * Tracks whether a service is being shared and its connection details.
 */
data class DevboxShareState(
    val serviceId: String,
    val sessionId: String,
    val isSharing: Boolean = false,
    val shareUrl: String? = null, // The URL others can use to connect
    val sharePort: Int? = null,
    val shareProtocol: ShareProtocol = ShareProtocol.HTTP,
    val startedAt: Long? = null,
    val expiresAt: Long? = null // Optional expiration time
)

enum class ShareProtocol(val displayName: String, val urlScheme: String) {
    HTTP("HTTP", "http"),
    HTTPS("HTTPS", "https"),
    SSH("SSH", "ssh"),
    CUSTOM("Custom", "")
}
