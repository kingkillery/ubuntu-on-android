package com.udroid.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.udroid.app.ui.desktop.DesktopScreen
import com.udroid.app.ui.session.SessionListScreen
import com.udroid.app.ui.services.ServicesScreen
import com.udroid.app.ui.setup.SetupWizardScreen
import com.udroid.app.ui.terminal.TerminalScreen
import com.udroid.app.ui.agent.AgentTaskScreen
import com.udroid.app.ui.theme.UdroidTheme
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    private var hasPermissions by mutableStateOf(false)
    
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        hasPermissions = allGranted
        Timber.d("Permissions granted: $allGranted")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        checkPermissions()
        
        setContent {
            UdroidTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(
                        hasPermissions = hasPermissions,
                        onRequestPermissions = { requestPermissions() }
                    )
                }
            }
        }
    }

    private fun checkPermission(): Boolean {
        return REQUIRED_PERMISSIONS.all { permission ->
            ContextCompat.checkSelfPermission(
                this,
                permission
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun checkPermissions() {
        hasPermissions = checkPermission()
    }

    private fun requestPermissions() {
        val missingPermissions = REQUIRED_PERMISSIONS.filter { permission ->
            ContextCompat.checkSelfPermission(
                this,
                permission
            ) != PackageManager.PERMISSION_GRANTED
        }
        
        if (missingPermissions.isNotEmpty()) {
            permissionLauncher.launch(missingPermissions.toTypedArray())
        }
    }

    companion object {
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.WAKE_LOCK,
            Manifest.permission.POST_NOTIFICATIONS
        )
    }
}

@Composable
fun AppNavigation(
    hasPermissions: Boolean,
    onRequestPermissions: () -> Unit
) {
    LaunchedEffect(hasPermissions) {
        if (!hasPermissions) {
            onRequestPermissions()
        }
    }

    val navController = rememberNavController()
    val startDestination = if (hasPermissions) "session_list" else "permissions"

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable("permissions") {
            PermissionScreen(
                hasPermissions = hasPermissions,
                onComplete = {
                    navController.navigate("session_list") {
                        popUpTo("permissions") { inclusive = true }
                    }
                }
            )
        }

        composable("session_list") {
            SessionListScreen(
                onCreateSession = {
                    navController.navigate("setup_wizard")
                },
                onSessionClick = { sessionId ->
                    navController.navigate("desktop/$sessionId")
                },
                onServicesClick = { sessionId ->
                    navController.navigate("services/$sessionId")
                },
                onTerminalClick = { sessionId ->
                    navController.navigate("terminal/$sessionId")
                },
                onAgentTaskClick = { sessionId ->
                    navController.navigate("agent_task/$sessionId")
                }
            )
        }

        composable("setup_wizard") {
            SetupWizardScreen(
                onComplete = { sessionId ->
                    navController.popBackStack("session_list", inclusive = false)
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = "desktop/{sessionId}",
            arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: return@composable
            DesktopScreen(
                sessionId = sessionId,
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = "services/{sessionId}",
            arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: return@composable
            ServicesScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = "terminal/{sessionId}",
            arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: return@composable
            TerminalScreen(
                sessionId = sessionId,
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = "agent_task/{sessionId}",
            arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: return@composable
            AgentTaskScreen(
                sessionId = sessionId,
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}

@Composable
fun PermissionScreen(
    hasPermissions: Boolean,
    onComplete: () -> Unit
) {
    LaunchedEffect(hasPermissions) {
        if (hasPermissions) {
            onComplete()
        }
    }

    androidx.compose.foundation.layout.Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        if (hasPermissions) {
            // Will navigate away immediately
        } else {
            androidx.compose.material3.CircularProgressIndicator()
        }
    }
}
