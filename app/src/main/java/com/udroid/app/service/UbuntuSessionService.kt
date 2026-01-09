package com.udroid.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import com.udroid.app.MainActivity
import com.udroid.app.R
import com.udroid.app.model.SessionState
import com.udroid.app.session.UbuntuSessionManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class UbuntuSessionService : Service() {

    @Inject
    lateinit var sessionManager: UbuntuSessionManager

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val binder = LocalBinder()

    private val _activeSessionId = MutableStateFlow<String?>(null)
    val activeSessionId: StateFlow<String?> = _activeSessionId.asStateFlow()

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "ubuntu_session_channel"
        private const val NOTIFICATION_ID = 1001
        const val ACTION_START_SESSION = "com.udroid.app.START_SESSION"
        const val ACTION_STOP_SESSION = "com.udroid.app.STOP_SESSION"
        const val EXTRA_SESSION_ID = "session_id"
    }

    inner class LocalBinder : Binder() {
        fun getService(): UbuntuSessionService = this@UbuntuSessionService
    }

    override fun onCreate() {
        super.onCreate()
        Timber.d("UbuntuSessionService created")
        createNotificationChannel()
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_SESSION -> {
                val sessionId = intent.getStringExtra(EXTRA_SESSION_ID)
                if (sessionId != null) {
                    startSession(sessionId)
                }
            }
            ACTION_STOP_SESSION -> {
                val sessionId = intent.getStringExtra(EXTRA_SESSION_ID)
                if (sessionId != null) {
                    stopSession(sessionId)
                }
            }
        }
        
        return START_STICKY
    }

    private fun startSession(sessionId: String) {
        serviceScope.launch {
            try {
                Timber.d("Starting session: $sessionId")
                sessionManager.startSession(sessionId)
                _activeSessionId.value = sessionId
                startForeground(NOTIFICATION_ID, createNotification(sessionId))
            } catch (e: Exception) {
                Timber.e(e, "Failed to start session: $sessionId")
            }
        }
    }

    private fun stopSession(sessionId: String) {
        serviceScope.launch {
            try {
                Timber.d("Stopping session: $sessionId")
                sessionManager.stopSession(sessionId)
                _activeSessionId.value = null
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            } catch (e: Exception) {
                Timber.e(e, "Failed to stop session: $sessionId")
            }
        }
    }

    private fun createNotification(sessionId: String): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, UbuntuSessionService::class.java).apply {
            action = ACTION_STOP_SESSION
            putExtra(EXTRA_SESSION_ID, sessionId)
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Ubuntu Session Running")
            .setContentText("Session: $sessionId")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Stop",
                stopPendingIntent
            )
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "Ubuntu Sessions",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Notifications for running Ubuntu sessions"
        }

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        Timber.d("UbuntuSessionService destroyed")
    }
}
