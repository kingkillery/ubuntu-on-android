package com.udroid.app.ui.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Terminal color scheme
object TerminalColors {
    val background = Color(0xFF1E1E1E)
    val foreground = Color(0xFFD4D4D4)
    val command = Color(0xFF569CD6)
    val error = Color(0xFFF44747)
    val prompt = Color(0xFF4EC9B0)
    val cursor = Color(0xFFAEAFAD)
}

@Composable
fun TerminalComponent(
    lines: List<TerminalLine>,
    currentInput: String,
    isExecuting: Boolean,
    onInputChange: (String) -> Unit,
    onExecute: () -> Unit,
    onHistoryUp: () -> Unit,
    onHistoryDown: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }

    // Auto-scroll to bottom when new lines are added
    LaunchedEffect(lines.size) {
        if (lines.isNotEmpty()) {
            listState.animateScrollToItem(lines.size - 1)
        }
    }

    // Request focus on mount
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TerminalColors.background)
            .padding(8.dp)
    ) {
        // Terminal output area
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            items(lines) { line ->
                TerminalLineRow(line = line)
            }
        }

        // Input area
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Prompt
            Text(
                text = if (isExecuting) "..." else "$ ",
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    color = TerminalColors.prompt
                )
            )

            // Input field
            BasicTextField(
                value = currentInput,
                onValueChange = onInputChange,
                enabled = !isExecuting,
                textStyle = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    color = TerminalColors.foreground
                ),
                cursorBrush = SolidColor(TerminalColors.cursor),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { onExecute() }
                ),
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester)
                    .onPreviewKeyEvent { keyEvent ->
                        when {
                            keyEvent.key == Key.DirectionUp && keyEvent.type == KeyEventType.KeyDown -> {
                                onHistoryUp()
                                true
                            }
                            keyEvent.key == Key.DirectionDown && keyEvent.type == KeyEventType.KeyDown -> {
                                onHistoryDown()
                                true
                            }
                            keyEvent.key == Key.Enter && keyEvent.type == KeyEventType.KeyDown -> {
                                onExecute()
                                true
                            }
                            else -> false
                        }
                    },
                singleLine = true
            )
        }
    }
}

@Composable
fun TerminalLineRow(line: TerminalLine) {
    val textColor = when {
        line.isError -> TerminalColors.error
        line.isCommand -> TerminalColors.command
        else -> TerminalColors.foreground
    }

    Text(
        text = line.text,
        style = TextStyle(
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp,
            color = textColor
        ),
        modifier = Modifier.fillMaxWidth()
    )
}
