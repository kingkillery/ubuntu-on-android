package com.udroid.app.ui.agent

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun AgentBrowserView(
    serverUrl: String,
    onControlAction: (String, String) -> Unit
) {
    // Image URL with timestamp to bust cache
    val imageUrl = "$serverUrl/screenshot?t=${System.currentTimeMillis()}"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Browser Viewport Placeholder (No Coil)
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Browser Stream: $imageUrl", color = Color.White)
        }

        // Overlay Controls (Comet Style)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(50))
                .padding(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Config/Status
                IconButton(onClick = { onControlAction("STATUS", "") }) {
                    Icon(Icons.Filled.Refresh, "Status")
                }

                // Divider
                Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color.Gray))

                // Play/Pause
                FilledIconButton(onClick = { onControlAction("RESUME", "") }) {
                    Icon(Icons.Filled.PlayArrow, "Resume")
                }

                // Stop/Reset
                IconButton(onClick = { onControlAction("STOP_SERVER", "") }) {
                    Icon(Icons.Filled.Delete, "Stop")
                }
            }
        }
    }
}
