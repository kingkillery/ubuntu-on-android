package com.udroid.app.ui.tui
import androidx.compose.material3.MaterialTheme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Message role for styling differentiation.
 */
enum class MessageRole {
    USER,
    ASSISTANT,
    SYSTEM,
    ERROR
}

/**
 * TUI Message data class.
 */
data class TuiMessage(
    val id: String,
    val role: MessageRole,
    val content: String,
    val agent: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val tokens: Int? = null,
    val duration: Long? = null
)

/**
 * TUI MessageBubble composable for displaying chat messages in terminal style.
 * Differentiates between user commands and assistant responses.
 *
 * @param message The message to display
 * @param showMetadata Whether to show tokens/duration info
 * @param modifier Compose modifier
 */
@Composable
fun TuiMessageBubble(
    message: TuiMessage,
    showMetadata: Boolean = false,
    modifier: Modifier = Modifier
) {
    when (message.role) {
        MessageRole.USER -> UserMessageBubble(
            content = message.content,
            modifier = modifier
        )
        MessageRole.ASSISTANT -> AssistantMessageBubble(
            content = message.content,
            agent = message.agent,
            tokens = message.tokens,
            duration = message.duration,
            showMetadata = showMetadata,
            modifier = modifier
        )
        MessageRole.SYSTEM -> SystemMessageBubble(
            content = message.content,
            modifier = modifier
        )
        MessageRole.ERROR -> ErrorMessageBubble(
            content = message.content,
            modifier = modifier
        )
    }
}

@Composable
private fun UserMessageBubble(
    content: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        // User prompt prefix
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "> ",
                color = MaterialTheme.colorScheme.primary,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = content,
                color = MaterialTheme.colorScheme.primary,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun AssistantMessageBubble(
    content: String,
    agent: String?,
    tokens: Int?,
    duration: Long?,
    showMetadata: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        // Agent header if provided
        if (agent != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 2.dp)
            ) {
                Text(
                    text = TuiBoxChars.BULLET_FILLED,
                    color = MaterialTheme.colorScheme.primary,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = agent,
                    color = MaterialTheme.colorScheme.onBackgroundDim,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp
                )

                // Metadata
                if (showMetadata) {
                    Spacer(modifier = Modifier.width(8.dp))
                    if (tokens != null) {
                        Text(
                            text = "${TuiTypography.formatTokens(tokens)} tokens",
                            color = MaterialTheme.colorScheme.onBackgroundMuted,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp
                        )
                    }
                    if (duration != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${duration}ms",
                            color = MaterialTheme.colorScheme.onBackgroundMuted,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }

        // Message content
        Text(
            text = content,
            color = MaterialTheme.colorScheme.onSurface,
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp,
            modifier = Modifier.padding(start = if (agent != null) 14.dp else 0.dp)
        )
    }
}

@Composable
private fun SystemMessageBubble(
    content: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "[system] ",
            color = MaterialTheme.colorScheme.secondary,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = content,
            color = MaterialTheme.colorScheme.secondary,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun ErrorMessageBubble(
    content: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f))
            .padding(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "error: ",
            color = MaterialTheme.colorScheme.errorMessage,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = content,
            color = MaterialTheme.colorScheme.errorMessage,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp
        )
    }
}

/**
 * Loading message bubble for when agent is thinking.
 */
@Composable
fun TuiLoadingBubble(
    agent: String = "claude",
    text: String = "thinking...",
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
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
            text = agent,
            color = MaterialTheme.colorScheme.onBackgroundDim,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            color = MaterialTheme.colorScheme.primary,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp
        )
    }
}

/**
 * Tool activity indicator for when agent is using tools.
 */
@Composable
fun TuiToolActivity(
    toolName: String,
    status: String = "running",
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
            text = "[$status] ",
            color = androidx.compose.ui.graphics.Color(0xFFDCDCAA),
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp
        )
        Text(
            text = toolName,
            color = MaterialTheme.colorScheme.onBackground,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp
        )
    }
}
