package com.udroid.app.ui.desktop

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.udroid.app.ui.theme.UdroidTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DesktopActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sessionId = intent.getStringExtra("session_id") ?: run {
            finish()
            return
        }

        setContent {
            UdroidTheme {
                DesktopScreen(
                    sessionId = sessionId,
                    onBack = { finish() }
                )
            }
        }
    }
}
