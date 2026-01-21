package com.udroid.app.ui.tui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Agent status for banner display.
 */
data class AgentStatus(
    val name: String,
    val ready: Boolean
)

/**
 * TUI Banner composable displaying PK-Puzld branding in ASCII art style.
 * Mimics the Puzld.ai Banner.tsx component.
 *
 * @param version Current version string
 * @param minimal If true, shows condensed single-line banner
 * @param agents List of agent statuses to display
 * @param modifier Compose modifier
 */
@Composable
fun TuiBanner(
    version: String = "0.1.0",
    minimal: Boolean = false,
    agents: List<AgentStatus> = emptyList(),
    modifier: Modifier = Modifier
) {
    if (minimal) {
        MinimalBanner(version = version, modifier = modifier)
    } else {
        FullBanner(version = version, agents = agents, modifier = modifier)
    }
}

@Composable
private fun MinimalBanner(
    version: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "PK-Puzld",
            color = TuiColors.White,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp
        )
        Text(
            text = " v$version",
            color = TuiColors.ForegroundDim,
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun FullBanner(
    version: String,
    agents: List<AgentStatus>,
    modifier: Modifier = Modifier
) {
    val defaultAgents = if (agents.isEmpty()) {
        listOf(
            AgentStatus("claude", false),
            AgentStatus("gemini", false),
            AgentStatus("codex", false),
            AgentStatus("ollama", false),
            AgentStatus("mistral", false),
            AgentStatus("factory", false)
        )
    } else {
        agents
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(TuiColors.Background)
            .padding(bottom = 8.dp)
    ) {
        // Top border with version tag
        TopBorderWithTitle(version = version)

        // Empty padding line
        BorderedEmptyLine()

        // ASCII Art Logo
        AsciiLogo()

        // Version subtitle
        VersionSubtitle(version = version)

        // Empty padding line
        BorderedEmptyLine()

        // Middle divider
        MiddleDivider()

        // Panel headers
        PanelHeaders()

        // Quick start commands + Agent status (interleaved rows)
        QuickStartWithAgents(agents = defaultAgents)

        // Bottom border
        BottomBorder()
    }
}

@Composable
private fun TopBorderWithTitle(version: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${TuiBoxChars.TOP_LEFT}${TuiBoxChars.HORIZONTAL.repeat(3)} ",
            color = TuiColors.Gray,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp
        )
        Text(
            text = "PK-Puzld",
            color = TuiColors.Red,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp
        )
        Text(
            text = " v$version ",
            color = TuiColors.ForegroundDim,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp
        )
        Text(
            text = "${TuiBoxChars.HORIZONTAL.repeat(40)}${TuiBoxChars.TOP_RIGHT}",
            color = TuiColors.Gray,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun BorderedEmptyLine() {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = TuiBoxChars.VERTICAL,
            color = TuiColors.Gray,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = TuiBoxChars.VERTICAL,
            color = TuiColors.Gray,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun AsciiLogo() {
    // ASCII art for "PK" in white and "puzld" in red
    // Using Small figlet-style font approximation
    val pkLines = listOf(
        " ___  _  __",
        "| _ \\| |/ /",
        "|  _/|   < ",
        "|_|  |_|\\_\\"
    )
    val puzldLines = listOf(
        "                 _     _ ",
        " _ __  _  _ ____| | __| |",
        "| '_ \\| || |_ / | |/ _` |",
        "| .__/ \\_,_/__/_|_|\\__,_|",
        "|_|                      "
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val maxLines = maxOf(pkLines.size, puzldLines.size)
        for (i in 0 until maxLines) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = TuiBoxChars.VERTICAL,
                    color = TuiColors.Gray,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                // PK part (white)
                Text(
                    text = pkLines.getOrElse(i) { " ".repeat(12) },
                    color = TuiColors.White,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                )
                Text(
                    text = "  ",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                )
                // puzld part (red)
                Text(
                    text = puzldLines.getOrElse(i) { "" },
                    color = TuiColors.Red,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = TuiBoxChars.VERTICAL,
                    color = TuiColors.Gray,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun VersionSubtitle(version: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = TuiBoxChars.VERTICAL,
            color = TuiColors.Gray,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp
        )
        Text(
            text = TuiTypography.center("v$version - Multi-LLM Orchestrator", 70),
            color = TuiColors.ForegroundDim,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center
        )
        Text(
            text = TuiBoxChars.VERTICAL,
            color = TuiColors.Gray,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun MiddleDivider() {
    Text(
        text = "${TuiBoxChars.LEFT_T}${TuiBoxChars.HORIZONTAL.repeat(50)}${TuiBoxChars.TOP_T}${TuiBoxChars.HORIZONTAL.repeat(20)}${TuiBoxChars.RIGHT_T}",
        color = TuiColors.Gray,
        fontFamily = FontFamily.Monospace,
        fontSize = 12.sp
    )
}

@Composable
private fun PanelHeaders() {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = TuiBoxChars.VERTICAL,
            color = TuiColors.Gray,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp
        )
        Text(
            text = TuiTypography.padEnd(" Quick start", 50),
            color = TuiColors.White,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp
        )
        Text(
            text = TuiBoxChars.VERTICAL,
            color = TuiColors.Gray,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp
        )
        Text(
            text = TuiTypography.center("Status", 20),
            color = TuiColors.White,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp
        )
        Text(
            text = TuiBoxChars.VERTICAL,
            color = TuiColors.Gray,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun QuickStartWithAgents(agents: List<AgentStatus>) {
    val commands = listOf(
        " /compare claude,gemini \"task\"",
        " /autopilot \"complex task\"",
        " /workflow code-review \"code\""
    )

    // Show commands on left, agents on right
    commands.forEachIndexed { index, command ->
        val agent = agents.getOrNull(index)
        QuickStartRow(
            leftContent = command,
            agent = agent
        )
    }

    // Show remaining agents
    agents.drop(3).forEach { agent ->
        QuickStartRow(
            leftContent = "",
            agent = agent
        )
    }
}

@Composable
private fun QuickStartRow(
    leftContent: String,
    agent: AgentStatus?
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = TuiBoxChars.VERTICAL,
            color = TuiColors.Gray,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp
        )
        Text(
            text = TuiTypography.padEnd(leftContent, 50),
            color = TuiColors.ForegroundDim,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp
        )
        Text(
            text = TuiBoxChars.VERTICAL,
            color = TuiColors.Gray,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp
        )
        if (agent != null) {
            AgentStatusCell(agent = agent, width = 20)
        } else {
            Text(
                text = " ".repeat(20),
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp
            )
        }
        Text(
            text = TuiBoxChars.VERTICAL,
            color = TuiColors.Gray,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun AgentStatusCell(agent: AgentStatus, width: Int) {
    val bullet = if (agent.ready) TuiBoxChars.BULLET_FILLED else TuiBoxChars.BULLET_EMPTY
    val statusText = if (agent.ready) "ready" else "off"
    val displayText = " $bullet ${agent.name.padEnd(7)} $statusText"

    Text(
        text = TuiTypography.center(displayText.trim(), width),
        color = if (agent.ready) TuiColors.AgentReady else TuiColors.ForegroundDim,
        fontFamily = FontFamily.Monospace,
        fontSize = 12.sp
    )
}

@Composable
private fun BottomBorder() {
    Text(
        text = "${TuiBoxChars.BOTTOM_LEFT}${TuiBoxChars.HORIZONTAL.repeat(50)}${TuiBoxChars.BOTTOM_T}${TuiBoxChars.HORIZONTAL.repeat(20)}${TuiBoxChars.BOTTOM_RIGHT}",
        color = TuiColors.Gray,
        fontFamily = FontFamily.Monospace,
        fontSize = 12.sp
    )
}

/**
 * Welcome message shown below banner.
 */
@Composable
fun TuiWelcomeMessage(
    modifier: Modifier = Modifier
) {
    Text(
        text = "Type a message or use /help for commands",
        color = TuiColors.ForegroundDim,
        fontFamily = FontFamily.Monospace,
        fontSize = 12.sp,
        modifier = modifier.padding(bottom = 8.dp)
    )
}
