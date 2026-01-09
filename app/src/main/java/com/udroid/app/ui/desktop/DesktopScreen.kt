package com.udroid.app.ui.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.udroid.app.model.SessionState
import timber.log.Timber

@Composable
fun DesktopScreen(
    sessionId: String,
    onBack: () -> Unit,
    viewModel: DesktopViewModel = hiltViewModel()
) {
    val sessionState by viewModel.sessionState.collectAsState()
    val isRunning by viewModel.isRunning.collectAsState()

    LaunchedEffect(sessionId) {
        viewModel.connectToSession(sessionId)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        when (sessionState) {
            is SessionState.Created, is SessionState.Starting -> {
                LoadingState(
                    message = "Starting Ubuntu session...",
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            is SessionState.Running -> {
                DesktopSurface(
                    modifier = Modifier.fillMaxSize(),
                    onTouch = { x, y, action ->
                        viewModel.sendTouchEvent(x, y, action)
                    }
                )
            }
            is SessionState.Stopped, is SessionState.Stopping -> {
                SessionEndedState(
                    onBack = onBack,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
            is SessionState.Error -> {
                ErrorState(
                    message = (sessionState as SessionState.Error).message,
                    onBack = onBack,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }

        // Top bar
        TopBar(
            sessionId = sessionId,
            isRunning = isRunning,
            onStop = { viewModel.stopSession() },
            onBack = onBack,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

@Composable
fun DesktopSurface(
    modifier: Modifier = Modifier,
    onTouch: (Float, Float, String) -> Unit
) {
    // TODO: Implement actual SurfaceView with VNC rendering
    Box(modifier = modifier.background(Color.Black)) {
        Text(
            text = "Ubuntu Desktop\n(VNC placeholder)",
            color = Color.White,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
fun TopBar(
    sessionId: String,
    isRunning: Boolean,
    onStop: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.Black.copy(alpha = 0.5f),
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) {
                Text("← Back", color = Color.White)
            }

            Text(
                text = "Ubuntu Session - $sessionId",
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium
            )

            if (isRunning) {
                TextButton(onClick = onStop) {
                    Text("Stop")
                }
            }
        }
    }
}

@Composable
fun LoadingState(
    message: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(color = Color.White)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            color = Color.White,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
fun SessionEndedState(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Session Ended",
            color = Color.White,
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onBack) {
            Text("Back to Sessions")
        }
    }
}

@Composable
fun ErrorState(
    message: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Error",
            color = Color.Red,
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            color = Color.White,
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onBack) {
            Text("Back")
        }
    }
}
