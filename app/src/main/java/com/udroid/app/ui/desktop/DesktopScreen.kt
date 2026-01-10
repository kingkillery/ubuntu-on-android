package com.udroid.app.ui.desktop

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
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
    val shareUiState by viewModel.shareUiState.collectAsState()

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
            onShare = { viewModel.toggleSharePanel() },
            onBack = onBack,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // Share panel dialog
        if (shareUiState.isSharePanelVisible) {
            ShareDialog(
                shareUrl = shareUiState.shareUrl,
                onDismiss = { viewModel.hideSharePanel() }
            )
        }
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
    onShare: () -> Unit,
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
                text = "Ubuntu Session",
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )

            Row {
                if (isRunning) {
                    IconButton(onClick = onShare) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share session",
                            tint = Color.White
                        )
                    }
                    TextButton(onClick = onStop) {
                        Text("Stop", color = Color.White)
                    }
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

@Composable
fun ShareDialog(
    shareUrl: String,
    onDismiss: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .widthIn(min = 280.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Share Session",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // QR Code
                val qrBitmap = remember(shareUrl) {
                    generateQrCode(shareUrl, 200)
                }
                if (qrBitmap != null) {
                    Image(
                        bitmap = qrBitmap.asImageBitmap(),
                        contentDescription = "QR Code for $shareUrl",
                        modifier = Modifier.size(200.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .background(Color.Gray),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("QR Generation Failed")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Scan to connect",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                // URL display
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = shareUrl,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Copy button
                Button(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(shareUrl))
                        copied = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (copied) "Copied!" else "Copy URL")
                }
            }
        }
    }
}

/**
 * Generates a QR code bitmap for the given content.
 */
private fun generateQrCode(content: String, size: Int): Bitmap? {
    return try {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(
                    x, y,
                    if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE
                )
            }
        }
        bitmap
    } catch (e: Exception) {
        Timber.e(e, "Failed to generate QR code")
        null
    }
}
