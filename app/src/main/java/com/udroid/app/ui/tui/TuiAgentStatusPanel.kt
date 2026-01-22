package com.udroid.app.ui.tui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * TUI AgentStatusPanel showing status of multiple LLM agents.
 * Displays ready/off status for each configured agent.
 * Mimics the right-side panel from Puzld.ai Banner.tsx.
 *
 * @param agents List of agent statuses
 * @param title Panel title
 * @param compact Use compact single-line display
 * @param modifier Compose modifier
 */
@Composable
fun TuiAgentStatusPanel(
    agents: List<AgentStatus>,
    title: String = "Status",
    compact: Boolean = false,
    modifier: Modifier = Modifier
) {
    // Default agent order matching Puzld.ai
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

    if (compact) {
        CompactAgentPanel(agents = defaultAgents, modifier = modifier)
    } else {
        FullAgentPanel(agents = defaultAgents, title = title, modifier = modifier)
    }
}

@Composable
private fun FullAgentPanel(
    agents: List<AgentStatus>,
    title: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.background)
            .padding(4.dp)
    ) {
        // Panel header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Agent list
        agents.forEach { agent ->
            AgentStatusRow(agent = agent)
        }
    }
}

@Composable
private fun AgentStatusRow(
    agent: AgentStatus,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        // Status bullet
        Text(
            text = if (agent.ready) TuiBoxChars.BULLET_FILLED else TuiBoxChars.BULLET_EMPTY,
            color = if (agent.ready) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.width(6.dp))

        // Agent name
        Text(
            text = agent.name.padEnd(8),
            color = MaterialTheme.colorScheme.onBackground,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp
        )

        // Status text
        Text(
            text = if (agent.ready) "ready" else "off",
            color = if (agent.ready) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackgroundDim,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun CompactAgentPanel(
    agents: List<AgentStatus>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        agents.forEach { agent ->
            CompactAgentIndicator(agent = agent)
        }
    }
}

@Composable
private fun CompactAgentIndicator(
    agent: AgentStatus,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (agent.ready) TuiBoxChars.BULLET_FILLED else TuiBoxChars.BULLET_EMPTY,
            color = if (agent.ready) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp
        )
        Spacer(modifier = Modifier.width(2.dp))
        Text(
            text = agent.name.take(3), // Abbreviated name
            color = if (agent.ready) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onBackgroundDim,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp
        )
    }
}

/**
 * Inline agent selector for switching between agents.
 */
@Composable
fun TuiAgentSelector(
    agents: List<AgentStatus>,
    selectedAgent: String,
    onAgentSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Agent:",
            color = MaterialTheme.colorScheme.onBackgroundDim,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp
        )

        agents.filter { it.ready }.forEach { agent ->
            val isSelected = agent.name == selectedAgent
            Text(
                text = "[${if (isSelected) "x" else " "}] ${agent.name}",
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

/**
 * Agent health check summary.
 */
@Composable
fun TuiAgentHealthSummary(
    agents: List<AgentStatus>,
    modifier: Modifier = Modifier
) {
    val readyCount = agents.count { it.ready }
    val totalCount = agents.size

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Agents: ",
            color = MaterialTheme.colorScheme.onBackgroundDim,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp
        )
        Text(
            text = "$readyCount/$totalCount ready",
            color = when {
                readyCount == 0 -> MaterialTheme.colorScheme.error
                readyCount < totalCount -> androidx.compose.ui.graphics.Color(0xFFDCDCAA)
                else -> androidx.compose.ui.graphics.Color(0xFF4EC9B0)
            },
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp
        )
    }
}
