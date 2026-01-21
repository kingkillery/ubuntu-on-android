package com.udroid.app.ui.tui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Quick start command definition.
 */
data class QuickCommand(
    val command: String,
    val description: String,
    val example: String? = null
)

/**
 * Default quick start commands matching Puzld.ai.
 */
val defaultQuickCommands = listOf(
    QuickCommand(
        command = "/compare",
        description = "Compare responses from multiple agents",
        example = "/compare claude,gemini \"explain recursion\""
    ),
    QuickCommand(
        command = "/autopilot",
        description = "Generate and execute an AI-planned workflow",
        example = "/autopilot \"refactor auth module\""
    ),
    QuickCommand(
        command = "/workflow",
        description = "Run a predefined workflow template",
        example = "/workflow code-review \"src/main.kt\""
    ),
    QuickCommand(
        command = "/do",
        description = "Execute a task with automatic approach selection",
        example = "/do \"fix the login bug\""
    ),
    QuickCommand(
        command = "/chat",
        description = "Start conversational chat mode",
        example = "/chat"
    ),
    QuickCommand(
        command = "/help",
        description = "Show all available commands",
        example = "/help"
    )
)

/**
 * TUI QuickStart panel showing common commands.
 * Tappable items populate the input field.
 *
 * @param commands List of quick commands to display
 * @param onCommandClick Callback when a command is tapped
 * @param maxCommands Maximum number of commands to show
 * @param showDescriptions Whether to show command descriptions
 * @param modifier Compose modifier
 */
@Composable
fun TuiQuickStart(
    commands: List<QuickCommand> = defaultQuickCommands,
    onCommandClick: (String) -> Unit = {},
    maxCommands: Int = 6,
    showDescriptions: Boolean = true,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
    ) {
        // Header
        Text(
            text = "Quick start:",
            color = TuiColors.ForegroundDim,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Command list
        commands.take(maxCommands).forEach { cmd ->
            QuickCommandRow(
                command = cmd,
                showDescription = showDescriptions,
                onClick = { onCommandClick(cmd.example ?: cmd.command) }
            )
        }
    }
}

@Composable
private fun QuickCommandRow(
    command: QuickCommand,
    showDescription: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp)
    ) {
        Row {
            Text(
                text = "  ",
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp
            )
            Text(
                text = command.command,
                color = TuiColors.Info,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp
            )
            if (showDescription) {
                Text(
                    text = " - ${command.description}",
                    color = TuiColors.ForegroundDim,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                )
            }
        }

        // Show example if available
        if (command.example != null && showDescription) {
            Text(
                text = "    ${command.example}",
                color = TuiColors.ForegroundMuted,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

/**
 * Compact quick start for space-constrained layouts.
 * Shows only command names as tappable chips.
 */
@Composable
fun TuiQuickStartCompact(
    commands: List<QuickCommand> = defaultQuickCommands.take(4),
    onCommandClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        commands.forEach { cmd ->
            Text(
                text = cmd.command,
                color = TuiColors.Info,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                modifier = Modifier
                    .clickable { onCommandClick(cmd.example ?: cmd.command) }
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

/**
 * Help panel showing all available commands.
 */
@Composable
fun TuiHelpPanel(
    modifier: Modifier = Modifier
) {
    val allCommands = listOf(
        // Primary commands
        QuickCommand("/do <task>", "Execute task with auto approach"),
        QuickCommand("/chat", "Conversational chat mode"),
        QuickCommand("/compare <agents> <task>", "Compare agent responses"),
        QuickCommand("/autopilot <task>", "AI-planned workflow"),

        // Collaboration
        QuickCommand("/correct <task>", "Cross-agent correction"),
        QuickCommand("/debate <topic>", "Multi-agent debate"),
        QuickCommand("/consensus <task>", "Build agent consensus"),

        // Workflows
        QuickCommand("/workflow <name> <args>", "Run workflow template"),
        QuickCommand("/pickbuild <task>", "Compare plans, pick best, build"),
        QuickCommand("/pkpoet <task>", "Deep REASON->DISCOVER->EXECUTE"),

        // Session
        QuickCommand("/session list", "List sessions"),
        QuickCommand("/session new", "New session"),
        QuickCommand("/session clear", "Clear current session"),

        // Settings
        QuickCommand("/model set <agent> <model>", "Set model for agent"),
        QuickCommand("/model show", "Show current models"),

        // Other
        QuickCommand("/help", "Show this help"),
        QuickCommand("/clear", "Clear screen"),
        QuickCommand("/exit", "Exit TUI")
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Text(
            text = "Available Commands:",
            color = TuiColors.White,
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = TuiBoxChars.horizontalLine(60),
            color = TuiColors.Border,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp
        )

        allCommands.forEach { cmd ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
            ) {
                Text(
                    text = TuiTypography.padEnd(cmd.command, 30),
                    color = TuiColors.Info,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp
                )
                Text(
                    text = cmd.description,
                    color = TuiColors.ForegroundDim,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp
                )
            }
        }
    }
}
