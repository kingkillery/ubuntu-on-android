package com.udroid.app.ui.tui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * TuiScreen - Main container composable for TUI-style views.
 * Assembles Banner, content area, and StatusBar into a cohesive terminal interface.
 *
 * Layout:
 * ┌─────────────────────────────────────┐
 * │           TuiBanner                 │
 * ├─────────────────────────────────────┤
 * │                                     │
 * │           Content Area              │
 * │         (messages, etc.)            │
 * │                                     │
 * ├─────────────────────────────────────┤
 * │           TuiInputBox               │
 * ├─────────────────────────────────────┤
 * │           TuiStatusBar              │
 * └─────────────────────────────────────┘
 *
 * @param version Version string for banner
 * @param showBanner Whether to show banner (full or minimal based on scroll)
 * @param minimalBanner Force minimal banner mode
 * @param agents List of agent statuses for banner
 * @param agentName Current active agent for status bar
 * @param messageCount Message count for status bar
 * @param tokenCount Token count for status bar
 * @param mcpStatus MCP connection status
 * @param isLoading Whether agent is processing
 * @param loadingText Loading indicator text
 * @param inputValue Current input text
 * @param onInputChange Input text change callback
 * @param onSubmit Submit callback
 * @param inputEnabled Whether input is enabled
 * @param history Command history for navigation
 * @param content Content slot for main area
 * @param modifier Compose modifier
 */
@Composable
fun TuiScreen(
    version: String = "0.1.0",
    showBanner: Boolean = true,
    minimalBanner: Boolean = false,
    agents: List<AgentStatus> = emptyList(),
    agentName: String = "claude",
    messageCount: Int = 0,
    tokenCount: Int = 0,
    mcpStatus: McpStatus = McpStatus.LOCAL,
    isLoading: Boolean = false,
    loadingText: String = "thinking...",
    inputValue: String = "",
    onInputChange: (String) -> Unit = {},
    onSubmit: (String) -> Unit = {},
    inputEnabled: Boolean = true,
    history: List<String> = emptyList(),
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TuiColors.Background)
    ) {
        // Banner (full or minimal)
        if (showBanner) {
            TuiBanner(
                version = version,
                minimal = minimalBanner,
                agents = agents,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Content area (fills remaining space)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 4.dp)
        ) {
            content()
        }

        // Input box
        TuiInputBox(
            value = inputValue,
            onValueChange = onInputChange,
            onSubmit = onSubmit,
            enabled = inputEnabled,
            history = history,
            modifier = Modifier.fillMaxWidth()
        )

        // Status bar
        TuiStatusBar(
            agentName = agentName,
            messageCount = messageCount,
            tokenCount = tokenCount,
            mcpStatus = mcpStatus,
            isLoading = isLoading,
            loadingText = loadingText,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * TuiScreen with message list - convenience wrapper for chat-style screens.
 *
 * @param messages List of messages to display
 * @param showMetadata Whether to show token/duration metadata on messages
 * @param onMessageClick Optional click handler for messages
 */
@Composable
fun TuiChatScreen(
    messages: List<TuiMessage>,
    version: String = "0.1.0",
    showBanner: Boolean = true,
    minimalBanner: Boolean = false,
    agents: List<AgentStatus> = emptyList(),
    agentName: String = "claude",
    tokenCount: Int = 0,
    mcpStatus: McpStatus = McpStatus.LOCAL,
    isLoading: Boolean = false,
    loadingText: String = "thinking...",
    inputValue: String = "",
    onInputChange: (String) -> Unit = {},
    onSubmit: (String) -> Unit = {},
    inputEnabled: Boolean = true,
    history: List<String> = emptyList(),
    showMetadata: Boolean = false,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    TuiScreen(
        version = version,
        showBanner = showBanner,
        minimalBanner = minimalBanner || messages.isNotEmpty(),
        agents = agents,
        agentName = agentName,
        messageCount = messages.size,
        tokenCount = tokenCount,
        mcpStatus = mcpStatus,
        isLoading = isLoading,
        loadingText = loadingText,
        inputValue = inputValue,
        onInputChange = onInputChange,
        onSubmit = onSubmit,
        inputEnabled = inputEnabled,
        history = history,
        modifier = modifier
    ) {
        if (messages.isEmpty() && !isLoading) {
            // Show welcome message and quick start when empty
            Column {
                TuiWelcomeMessage()
                Spacer(modifier = Modifier.height(8.dp))
                TuiQuickStart(
                    onCommandClick = { command ->
                        onInputChange(command)
                    }
                )
            }
        } else {
            // Message list
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(
                    items = messages,
                    key = { it.id }
                ) { message ->
                    TuiMessageBubble(
                        message = message,
                        showMetadata = showMetadata
                    )
                }

                // Loading indicator at bottom
                if (isLoading) {
                    item {
                        TuiLoadingBubble(
                            agent = agentName,
                            text = loadingText
                        )
                    }
                }
            }
        }
    }
}

/**
 * Empty TUI screen for setup/onboarding flows.
 */
@Composable
fun TuiEmptyScreen(
    title: String,
    message: String,
    version: String = "0.1.0",
    modifier: Modifier = Modifier,
    action: @Composable (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TuiColors.Background)
            .padding(16.dp)
    ) {
        TuiBanner(
            version = version,
            minimal = true
        )

        Spacer(modifier = Modifier.height(24.dp))

        androidx.compose.material3.Text(
            text = title,
            color = TuiColors.White,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
            fontSize = 18.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        androidx.compose.material3.Text(
            text = message,
            color = TuiColors.ForegroundDim,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
            fontSize = 14.sp
        )

        if (action != null) {
            Spacer(modifier = Modifier.height(16.dp))
            action()
        }
    }
}

// Extension removed

