package com.udroid.app.ui.tui
import androidx.compose.material3.MaterialTheme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * TUI InputBox composable for command/message input.
 * Supports command history navigation with up/down arrows.
 * Mimics terminal-style input with prompt prefix.
 *
 * @param value Current input text
 * @param onValueChange Callback when text changes
 * @param onSubmit Callback when Enter is pressed
 * @param prompt Prompt prefix (e.g., "> " or "$ ")
 * @param placeholder Placeholder text when empty
 * @param enabled Whether input is enabled
 * @param history Command history for up/down navigation
 * @param onHistoryNavigate Callback when navigating history (direction: -1 up, +1 down)
 * @param modifier Compose modifier
 */
@Composable
fun TuiInputBox(
    value: String,
    onValueChange: (String) -> Unit,
    onSubmit: (String) -> Unit,
    prompt: String = "> ",
    placeholder: String = "Type a message or /help...",
    enabled: Boolean = true,
    history: List<String> = emptyList(),
    onHistoryNavigate: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    var historyIndex by remember { mutableIntStateOf(-1) }

    // Reset history index when value changes from user input
    LaunchedEffect(value) {
        if (historyIndex >= 0 && history.getOrNull(historyIndex) != value) {
            historyIndex = -1
        }
    }

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

        // Input row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = TuiBoxChars.VERTICAL,
                color = MaterialTheme.colorScheme.outline,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.width(4.dp))

            // Prompt
            Text(
                text = prompt,
                color = MaterialTheme.colorScheme.primary,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp
            )

            // Input field
            Box(modifier = Modifier.weight(1f)) {
                BasicTextField(
                    value = value,
                    onValueChange = { newValue ->
                        onValueChange(newValue)
                        historyIndex = -1
                    },
                    enabled = enabled,
                    singleLine = true,
                    textStyle = TextStyle(
                        color = MaterialTheme.colorScheme.onBackground,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Send
                    ),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (value.isNotBlank()) {
                                onSubmit(value)
                            }
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .onPreviewKeyEvent { keyEvent ->
                            when {
                                // Enter to submit
                                keyEvent.key == Key.Enter && keyEvent.type == KeyEventType.KeyDown -> {
                                    if (value.isNotBlank()) {
                                        onSubmit(value)
                                    }
                                    true
                                }
                                // Up arrow for history navigation
                                keyEvent.key == Key.DirectionUp && keyEvent.type == KeyEventType.KeyDown -> {
                                    if (history.isNotEmpty() && onHistoryNavigate != null) {
                                        val newIndex = (historyIndex + 1).coerceAtMost(history.lastIndex)
                                        if (newIndex != historyIndex) {
                                            historyIndex = newIndex
                                            onHistoryNavigate(-1)
                                            history.getOrNull(historyIndex)?.let { onValueChange(it) }
                                        }
                                    }
                                    true
                                }
                                // Down arrow for history navigation
                                keyEvent.key == Key.DirectionDown && keyEvent.type == KeyEventType.KeyDown -> {
                                    if (history.isNotEmpty() && onHistoryNavigate != null) {
                                        val newIndex = (historyIndex - 1).coerceAtLeast(-1)
                                        historyIndex = newIndex
                                        onHistoryNavigate(1)
                                        if (newIndex >= 0) {
                                            history.getOrNull(historyIndex)?.let { onValueChange(it) }
                                        } else {
                                            onValueChange("")
                                        }
                                    }
                                    true
                                }
                                else -> false
                            }
                        }
                )

                // Placeholder
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        color = MaterialTheme.colorScheme.onBackgroundMuted,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp
                    )
                }
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

    // Request focus on first composition
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

/**
 * Compact input box without borders for inline use.
 */
@Composable
fun TuiInputBoxCompact(
    value: String,
    onValueChange: (String) -> Unit,
    onSubmit: (String) -> Unit,
    prompt: String = "> ",
    placeholder: String = "...",
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = prompt,
            color = MaterialTheme.colorScheme.primary,
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp
        )

        Box(modifier = Modifier.weight(1f)) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = enabled,
                singleLine = true,
                textStyle = TextStyle(
                    color = MaterialTheme.colorScheme.onBackground,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Send
                ),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (value.isNotBlank()) {
                            onSubmit(value)
                        }
                    }
                ),
                modifier = Modifier.fillMaxWidth()
            )

            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    color = MaterialTheme.colorScheme.onBackgroundMuted,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp
                )
            }
        }
    }
}
