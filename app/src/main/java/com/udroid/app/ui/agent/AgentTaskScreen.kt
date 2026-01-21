package com.udroid.app.ui.agent

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentTaskScreen(
    sessionId: String,
    onBack: () -> Unit,
    viewModel: AgentTaskViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    // Auto-scroll when output changes
    LaunchedEffect(uiState.currentOutput) {
        if (uiState.currentOutput.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Agent Tasks",
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (uiState.sessionName.isNotEmpty()) {
                            Text(
                                text = uiState.sessionName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (uiState.currentOutput.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearOutput() }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Clear Output"
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Check if agent is installed
            when {
                uiState.isCheckingInstall -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                !uiState.isAgentInstalled -> {
                    AgentNotInstalledContent(
                        onInstall = { viewModel.installAgentTools() },
                        isInstalling = uiState.isRunning,
                        output = uiState.currentOutput
                    )
                }
                else -> {
                    // Mode toggle and content
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Mode toggle
                        ModeToggleSection(
                            currentMode = uiState.mode,
                            onModeChange = { viewModel.switchMode(it) }
                        )

                        // Mode-specific content
                        when (uiState.mode) {
                            AgentMode.BASIC -> {
                                BasicModeContent(
                                    uiState = uiState,
                                    listState = listState,
                                    onCategorySelect = { viewModel.selectCategory(it) },
                                    onPresetSelect = { viewModel.selectPreset(it) },
                                    onRunPreset = { viewModel.runPresetTask() },
                                    onTaskInputChange = { viewModel.updateTaskInput(it) },
                                    onRunTask = { viewModel.runTask() }
                                )
                            }
                            AgentMode.ADVANCED -> {
                                AdvancedModeContent(
                                    uiState = uiState,
                                    listState = listState,
                                    onTaskInputChange = { viewModel.updateTaskInput(it) },
                                    onSelectTool = { viewModel.selectTool(it) },
                                    onRunTask = { viewModel.runTask() },
                                    onBrowserControl = { action, payload -> 
                                        viewModel.sendBrowserControl(action, payload) 
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Error snackbar
    uiState.errorMessage?.let { error ->
        LaunchedEffect(error) {
            kotlinx.coroutines.delay(3000)
            viewModel.clearError()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModeToggleSection(
    currentMode: AgentMode,
    onModeChange: (AgentMode) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            AgentMode.entries.forEach { mode ->
                FilterChip(
                    selected = currentMode == mode,
                    onClick = { onModeChange(mode) },
                    label = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = mode.displayName,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (currentMode == mode) FontWeight.Bold else FontWeight.Normal
                            )
                            Text(
                                text = mode.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(60.dp)
                )
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BasicModeContent(
    uiState: AgentTaskUiState,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onCategorySelect: (PresetCategory) -> Unit,
    onPresetSelect: (PresetTask) -> Unit,
    onRunPreset: () -> Unit,
    onTaskInputChange: (String) -> Unit,
    onRunTask: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Category chips
        if (uiState.selectedCategory == null) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(PresetCategory.entries) { category ->
                    FilterChip(
                        selected = false,
                        onClick = { onCategorySelect(category) },
                        label = { 
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(category.displayName)
                                Text(
                                    category.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(android.graphics.Color.parseColor(category.color))
                        ),
                        modifier = Modifier.height(60.dp)
                    )
                }
            }
        } else {
            // Show back button when category is selected
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onCategorySelect(PresetCategory.SYSTEM) }) {
                    Icon(Icons.Default.ArrowBack, "Back to categories")
                }
                Text(
                    text = uiState.selectedCategory!!.displayName,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
        // Preset tasks or output
        if (uiState.selectedCategory != null && uiState.currentOutput.isEmpty()) {
            // Show preset tasks for selected category
            val presets = uiState.selectedCategory?.let { 
                PresetTasks.getByCategory(it) 
            } ?: emptyList()

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(presets) { preset ->
                    PresetTaskCard(
                        preset = preset,
                        isSelected = uiState.selectedPreset == preset,
                        onClick = { onPresetSelect(preset) }
                    )
                }
            }

            // Run button at bottom
            if (uiState.selectedPreset != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = uiState.selectedPreset!!.name,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Estimated: ${uiState.selectedPreset!!.estimatedTime}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (uiState.isRunning) {
                            CircularProgressIndicator()
                        } else {
                            Button(onClick = onRunPreset) {
                                Text("Run Task")
                            }
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(16.dp))
            }
        } else {
            // Show output area with custom task input
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF1E1E1E))
                        .padding(8.dp),
                    reverseLayout = true
                ) {
                    if (uiState.currentOutput.isNotEmpty()) {
                        item {
                            Text(
                                text = uiState.currentOutput,
                                style = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 13.sp,
                                    color = Color(0xFFD4D4D4)
                                )
                            )
                        }
                    } else {
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                if (uiState.selectedPreset != null) {
                                    CircularProgressIndicator()
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Running: ${uiState.selectedPreset!!.name}",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = Color(0xFFD4D4D4)
                                    )
                                } else {
                                    Text(
                                        text = "Select a task or describe your custom task below",
                                        style = TextStyle(
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 13.sp,
                                            color = Color(0xFF808080)
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Custom task input
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicTextField(
                        value = uiState.taskInput,
                        onValueChange = onTaskInputChange,
                        enabled = !uiState.isRunning,
                        textStyle = TextStyle(
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { onRunTask() }),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp),
                        decorationBox = { innerTextField ->
                            Box {
                                if (uiState.taskInput.isEmpty()) {
                                    Text(
                                        text = "Or describe your custom task...",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )

                    if (uiState.isRunning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(
                            onClick = onRunTask,
                            enabled = uiState.taskInput.isNotBlank()
                        ) {
                            Icon(
                                Icons.Filled.Send,
                                contentDescription = "Run Task",
                                tint = if (uiState.taskInput.isNotBlank())
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedModeContent(
    uiState: AgentTaskUiState,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onTaskInputChange: (String) -> Unit,
    onSelectTool: (AgentTool) -> Unit,
    onRunTask: () -> Unit,
    onBrowserControl: (String, String) -> Unit = { _, _ -> }
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Tool selector
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(AgentTool.entries) { tool ->
                FilterChip(
                    selected = uiState.selectedTool == tool,
                    onClick = { onSelectTool(tool) },
                    label = { Text(tool.displayName) }
                )
            }
        }

        // Content Area
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (uiState.selectedTool == AgentTool.BROWSER) {
                AgentBrowserView(
                    serverUrl = "http://localhost:3000",
                    onControlAction = onBrowserControl
                )
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Output area
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 16.dp)
                    ) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF1E1E1E))
                                .padding(8.dp),
                            reverseLayout = true
                        ) {
                            if (uiState.currentOutput.isNotEmpty()) {
                                item {
                                    Text(
                                        text = uiState.currentOutput,
                                        style = TextStyle(
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 13.sp,
                                            color = Color(0xFFD4D4D4)
                                        )
                                    )
                                }
                            } else {
                                item {
                                    Text(
                                        text = "Enter a task below and press Run",
                                        style = TextStyle(
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 13.sp,
                                            color = Color(0xFF808080)
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // Input area
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BasicTextField(
                                value = uiState.taskInput,
                                onValueChange = onTaskInputChange,
                                enabled = !uiState.isRunning,
                                textStyle = TextStyle(
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                keyboardActions = KeyboardActions(onSend = { onRunTask() }),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp),
                                decorationBox = { innerTextField ->
                                    Box {
                                        if (uiState.taskInput.isEmpty()) {
                                            Text(
                                                text = when (uiState.selectedTool) {
                                                    AgentTool.AGENT_RUN -> "Describe a task to run..."
                                                    AgentTool.GEMINI -> "Ask Gemini a question..."
                                                    AgentTool.DROID -> "Enter a droid command..."
//                                                    AgentTool.BROWSER -> "Browser control..."
                                                    else -> "Describe a task..."
                                                },
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        innerTextField()
                                    }
                                }
                            )

                            if (uiState.isRunning) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                IconButton(
                                    onClick = onRunTask,
                                    enabled = uiState.taskInput.isNotBlank()
                                ) {
                                    Icon(
                                        Icons.Filled.Send,
                                        contentDescription = "Run Task",
                                        tint = if (uiState.taskInput.isNotBlank())
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
@Composable
fun PresetTaskCard(
    preset: PresetTask,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = 2.dp,
                        color = Color(android.graphics.Color.parseColor(preset.category.color)),
                        shape = RoundedCornerShape(8.dp)
                    )
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = preset.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
                Text(
                    text = preset.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = Color(android.graphics.Color.parseColor(preset.category.color)),
                        shape = CircleShape
                    ) {
                        Box(modifier = Modifier.size(8.dp))
                    }
                    Text(
                        text = preset.category.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "• ${preset.estimatedTime}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (isSelected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = Color(android.graphics.Color.parseColor(preset.category.color)),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
@Composable
fun AgentNotInstalledContent(
    onInstall: () -> Unit,
    isInstalling: Boolean,
    output: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (isInstalling) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Installing agent tools...",
                style = MaterialTheme.typography.bodyLarge
            )
            if (output.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF1E1E1E))
                            .padding(8.dp)
                    ) {
                        item {
                            Text(
                                text = output,
                                style = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    color = Color(0xFFD4D4D4)
                                )
                            )
                        }
                    }
                }
            }
        } else {
            Icon(
                Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Agent Tools Not Installed",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Install pk-puzldai, droid, and Gemini CLI to run agent tasks",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onInstall) {
                Text("Install Agent Tools")
            }
        }
    }
}
