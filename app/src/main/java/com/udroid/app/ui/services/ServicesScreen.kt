package com.udroid.app.ui.services

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.udroid.app.devbox.ServiceState
import com.udroid.app.devbox.ServiceTemplate
import com.udroid.app.model.SessionState
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServicesScreen(
    onBack: () -> Unit,
    viewModel: ServicesViewModel = hiltViewModel()
) {
    val sessionName by viewModel.sessionName.collectAsState()
    val sessionState by viewModel.sessionState.collectAsState()
    val services by viewModel.services.collectAsState()
    val deviceIp by viewModel.deviceIp.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val installationStates by viewModel.installationStates.collectAsState()

    val context = LocalContext.current

    var showQrDialog by remember { mutableStateOf(false) }
    var qrContent by remember { mutableStateOf("") }
    var qrTitle by remember { mutableStateOf("") }
    
    var showCustomDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.toastMessage.collectLatest { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.error.collectLatest { error ->
            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Deploy Services") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showCustomDialog = true }) {
                        Icon(Icons.Filled.Add, contentDescription = "Add Custom Service")
                    }
                    IconButton(onClick = { viewModel.refreshDeviceIp() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh IP")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Session Info Card
            item {
                SessionInfoCard(
                    sessionName = sessionName,
                    sessionState = sessionState,
                    deviceIp = deviceIp
                )
            }

            item {
                Text(
                    text = "Active Services",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // Custom Services section
            val customServices = services.filter { service -> service.template.isCustom }

            if (customServices.isNotEmpty()) {
                items(customServices, key = { it.id }) { service ->
                    ServiceCard(
                        template = service.template,
                        isRunning = true, // It's in the list, so likely running or stopping
                        isInstalled = true,
                        serviceState = service.state,
                        connectInfo = viewModel.getConnectInfo(service.id)?.displayText ?: "",
                        isLoading = isLoading,
                        onToggle = { viewModel.stopService(service.id) },
                        onCopy = { viewModel.getConnectInfo(service.id)?.copyText?.let { text -> viewModel.copyToClipboard(text) } },
                        onShowQr = {
                            val info = viewModel.getConnectInfo(service.id)
                            if (info != null) {
                                qrContent = info.qrContent
                                qrTitle = service.template.displayName
                                showQrDialog = true
                            }
                        }
                    )
                }
            }

            item {
                Text(
                    text = "Presets",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // Service Templates
            items(ServiceTemplate.PRESETS) { template ->
                val isRunning = viewModel.isServiceRunning(template)
                val serviceState = viewModel.getServiceState(template)
                val connectInfo = viewModel.getConnectInfoForTemplate(template)
                val isInstalled = installationStates[template.id] ?: true // Default to true if unknown to avoid blocking, or false? defaulting to true if null to be safe on existing setups. Actually null means check hasn't run.

                ServiceCard(
                    template = template,
                    isRunning = isRunning,
                    isInstalled = isInstalled,
                    serviceState = serviceState,
                    connectInfo = connectInfo.displayText,
                    isLoading = isLoading,
                    onToggle = { viewModel.toggleService(template) },
                    onCopy = { viewModel.copyToClipboard(connectInfo.copyText) },
                    onShowQr = {
                        qrContent = connectInfo.qrContent
                        qrTitle = template.displayName
                        showQrDialog = true
                    }
                )
            }
        }
    }

    // QR Code Dialog
    if (showQrDialog) {
        QrCodeDialog(
            title = qrTitle,
            content = qrContent,
            qrBitmap = viewModel.generateQrCode(qrContent),
            onDismiss = { showQrDialog = false },
            onCopy = { viewModel.copyToClipboard(qrContent) }
        )
    }
}

@Composable
fun SessionInfoCard(
    sessionName: String,
    sessionState: SessionState,
    deviceIp: String?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = sessionName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                SessionStateBadge(state = sessionState)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Device IP: ${deviceIp ?: "Not connected to network"}",
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun SessionStateBadge(state: SessionState) {
    val (text, color) = when (state) {
        is SessionState.Created -> "Created" to MaterialTheme.colorScheme.secondary
        is SessionState.Starting -> "Starting" to MaterialTheme.colorScheme.tertiary
        is SessionState.Running -> "Running" to MaterialTheme.colorScheme.primary
        is SessionState.Stopping -> "Stopping" to MaterialTheme.colorScheme.tertiary
        is SessionState.Stopped -> "Stopped" to MaterialTheme.colorScheme.outline
        is SessionState.Error -> "Error" to MaterialTheme.colorScheme.error
    }

    Surface(
        shape = MaterialTheme.shapes.small,
        color = color.copy(alpha = 0.2f)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            color = color
        )
    }
}

@Composable
fun ServiceCard(
    template: ServiceTemplate,
    isRunning: Boolean,
    isInstalled: Boolean,
    serviceState: ServiceState,
    connectInfo: String,
    isLoading: Boolean,
    onToggle: () -> Unit,
    onCopy: () -> Unit,
    onShowQr: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = template.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = template.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (!isInstalled && !template.installCommand.isNullOrEmpty()) {
                        Text(
                            text = "Not Installed",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                if (!isInstalled && !template.installCommand.isNullOrEmpty()) {
                    Button(
                        onClick = onToggle,
                        enabled = !isLoading
                    ) {
                        Text("Install")
                    }
                } else {
                    Switch(
                        checked = isRunning,
                        onCheckedChange = { onToggle() },
                        enabled = !isLoading && serviceState !is ServiceState.Starting
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Port and status info
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Port ${template.defaultPort}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "•",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "LAN",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.weight(1f))

                when (serviceState) {
                    is ServiceState.Starting -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    }
                    is ServiceState.Error -> {
                        Text(
                            text = "Error",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    else -> {}
                }
            }

            if (isRunning || serviceState is ServiceState.Starting) {
                Spacer(modifier = Modifier.height(12.dp))

                // Connect info
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = connectInfo,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onCopy,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copy")
                    }

                    OutlinedButton(
                        onClick = onShowQr,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Filled.Share,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("QR")
                    }
                }
            }
        }
    }
}

@Composable
fun QrCodeDialog(
    title: String,
    content: String,
    qrBitmap: android.graphics.Bitmap,
    onDismiss: () -> Unit,
    onCopy: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(title)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                androidx.compose.foundation.Image(
                    bitmap = qrBitmap.asImageBitmap(),
                    contentDescription = "QR Code",
                    modifier = Modifier.size(256.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = content,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onCopy) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Copy")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun CustomServiceDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, port: Int, command: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("8080") }
    var command by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Custom Service") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Service Name") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = port,
                    onValueChange = { if (it.all { c -> c.isDigit() }) port = it },
                    label = { Text("Port") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = command,
                    onValueChange = { command = it },
                    label = { Text("Command") },
                    placeholder = { Text("e.g. python3 -m http.server 8080") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val portInt = port.toIntOrNull()
                    if (name.isNotEmpty() && portInt != null && command.isNotEmpty()) {
                        onCreate(name, portInt, command)
                    }
                }
            ) {
                Text("Start")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
