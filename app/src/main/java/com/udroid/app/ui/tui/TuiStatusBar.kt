package com.udroid.app.ui.tui

import androidx.compose.material3.MaterialTheme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * MCP connection status.
 */
enum class McpStatus {
    LOCAL,      // Running locally
    CONNECTED,  // Connected to MCP server
    CONNECTING, // Attempting connection
    ERROR       // Connection error
}

/**
 * TUI StatusBar composable showing agent info, message count, tokens, and MCP status.
 * Displayed at the bottom of TUI screens. Mimics Puzld.ai StatusBar.tsx.
 *
 * @param agentName Current active agent name
 * @param messageCount Number of messages in conversation
 * @param tokenCount Total tokens used
 * @param mcpStatus MCP connection status
 * @param isLoading Whether agent is currently processing
 * @param loadingText Text to show when loading
 * @param modifier Compose modifier
 */
@Composable
fun TuiStatusBar(
    agentName: String = "claude",
    messageCount: Int = 0,
    tokenCount: Int = 0,
    mcpStatus: McpStatus = McpStatus.LOCAL,
    isLoading: Boolean = false,
    loadingText: String = "thinking...",
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top border
        Text(
            text = TuiBoxChars.topBorder(72),
            color = MaterialTheme.colorScheme.outline,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp
        )

        // Status content row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = TuiBoxChars.VERTICAL,
                color = MaterialTheme.colorScheme.outline,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.width(4.dp))

            // Agent indicator
            AgentIndicator(agentName = agentName)

            Spacer(modifier = Modifier.width(12.dp))

            // Message count
            MessageCountIndicator(count = messageCount)

            Spacer(modifier = Modifier.width(12.dp))

            // Token count
            TokenCountIndicator(count = tokenCount)

            Spacer(modifier = Modifier.weight(1f))

            // Loading or MCP status
            if (isLoading) {
                LoadingIndicator(text = loadingText)
            } else {
                McpStatusIndicator(status = mcpStatus)
            }

            Spacer(modifier = Modifier.width(4.dp))

            Text(
                text = TuiBoxChars.VERTICAL,
                color = MaterialTheme.colorScheme.outline,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp
            )
        }

        // Bottom border
        Text(
            text = TuiBoxChars.bottomBorder(72),
            color = MaterialTheme.colorScheme.outline,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun AgentIndicator(agentName: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = TuiBoxChars.BULLET_FILLED,
            color = MaterialTheme.colorScheme.primary,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = agentName,
            color = MaterialTheme.colorScheme.onBackground,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun MessageCountIndicator(count: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "msgs:",
            color = MaterialTheme.colorScheme.onBackgroundDim,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = count.toString(),
            color = MaterialTheme.colorScheme.onBackground,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun TokenCountIndicator(count: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "tokens:",
            color = MaterialTheme.colorScheme.onBackgroundDim,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = TuiTypography.formatTokens(count),
            color = MaterialTheme.colorScheme.onBackground,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun LoadingIndicator(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        // Animated spinner could be added here
        Text(
            text = "...",
            color = MaterialTheme.colorScheme.primary,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            color = MaterialTheme.colorScheme.primary,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun McpStatusIndicator(status: McpStatus) {
    val (text, color) = when (status) {
        McpStatus.LOCAL -> "local" to MaterialTheme.colorScheme.onBackgroundDim
        McpStatus.CONNECTED -> "mcp" to androidx.compose.ui.graphics.Color(0xFF4EC9B0)
        McpStatus.CONNECTING -> "connecting..." to androidx.compose.ui.graphics.Color(0xFFDCDCAA)
        McpStatus.ERROR -> "mcp error" to MaterialTheme.colorScheme.error
    }

    Text(
        text = text,
        color = color,
        fontFamily = FontFamily.Monospace,
        fontSize = 12.sp
    )
}

/**
 * Compact status bar for space-constrained layouts.
 */
@Composable
fun TuiStatusBarCompact(
    agentName: String = "claude",
    isLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = TuiBoxChars.BULLET_FILLED,
            color = MaterialTheme.colorScheme.primary,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = agentName,
            color = MaterialTheme.colorScheme.onBackground,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp
        )
        Spacer(modifier = Modifier.weight(1f))
        if (isLoading) {
            Text(
                text = "...",
                color = MaterialTheme.colorScheme.primary,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp
            )
        }
    }
}
