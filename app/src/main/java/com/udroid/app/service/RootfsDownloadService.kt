package com.udroid.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.udroid.app.MainActivity
import com.udroid.app.R
import com.udroid.app.model.DistroVariant
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.buffer
import okio.sink
import timber.log.Timber
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class RootfsDownloadService : Service() {

    @Inject
    lateinit var rootfsManager: com.udroid.app.rootfs.RootfsManager

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private val binder = LocalBinder()
    private lateinit var currentDownloadJob: Job

    private val _downloadState = MutableStateFlow<DownloadState?>(null)
    val downloadState: StateFlow<DownloadState?> = _downloadState.asStateFlow()

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "rootfs_download_channel"
        private const val NOTIFICATION_ID = 1002
        const val ACTION_DOWNLOAD = "com.udroid.app.DOWNALL_ROOTFS"
        const val EXTRA_DISTRO_ID = "distro_id"
        const val EXTRA_DOWNLOAD_URL = "download_url"
    }

    inner class LocalBinder : Binder() {
        fun getService(): RootfsDownloadService = this@RootfsDownloadService
    }

    sealed class DownloadState {
        data object Idle : DownloadState()
        data class Downloading(val progress: Int, val bytesDownloaded: Long, val totalBytes: Long) : DownloadState()
        data class Verifying(val distro: DistroVariant) : DownloadState()
        data class Extracting(val progress: Int) : DownloadState()
        data class Success(val distro: DistroVariant) : DownloadState()
        data class Error(val message: String) : DownloadState()
    }

    override fun onCreate() {
        super.onCreate()
        Timber.d("RootfsDownloadService created")
        createNotificationChannel()
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_DOWNLOAD -> {
                val distroId = intent.getStringExtra(EXTRA_DISTRO_ID)
                val downloadUrl = intent.getStringExtra(EXTRA_DOWNLOAD_URL)
                
                if (distroId != null && downloadUrl != null) {
                    val distro = DistroVariant.fromId(distroId)
                    if (distro != null) {
                        startDownload(distro, downloadUrl)
                    }
                }
            }
        }
        
        return START_STICKY
    }

    private fun startDownload(distro: DistroVariant, url: String) {
        Timber.d("Starting download: ${distro.id} from $url")
        
        currentDownloadJob = serviceScope.launch {
            try {
                _downloadState.value = DownloadState.Idle
                startForeground(NOTIFICATION_ID, createNotification(distro, 0))
                
                val cacheFile = rootfsManager.getCacheFile(distro)
                
                // Download
                _downloadState.value = DownloadState.Downloading(0, 0, distro.sizeBytes)
                downloadFile(url, cacheFile) { progress, downloaded, total ->
                    _downloadState.value = DownloadState.Downloading(progress, downloaded, total)
                    updateNotification(distro, progress)
                }
                
                // Verify
                _downloadState.value = DownloadState.Verifying(distro)
                updateNotification(distro, 100, "Verifying...")
                
                // TODO: Verify checksum when available
                // val verified = rootfsManager.verifyChecksum(cacheFile, distro.sha256)
                // if (!verified) {
                //     throw SecurityException("Checksum verification failed")
                // }
                
                // Extract
                _downloadState.value = DownloadState.Extracting(0)
                updateNotification(distro, 100, "Extracting...")
                
                val targetDir = rootfsManager.getRootfsPath(distro)
                rootfsManager.extractRootfs(cacheFile, targetDir) { progress ->
                    _downloadState.value = DownloadState.Extracting(progress)
                    updateNotification(distro, progress, "Extracting...")
                }
                
                // Success
                _downloadState.value = DownloadState.Success(distro)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                
                Timber.d("Download complete: ${distro.id}")
            } catch (e: Exception) {
                Timber.e(e, "Download failed: ${distro.id}")
                _downloadState.value = DownloadState.Error(e.message ?: "Download failed")
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    private suspend fun downloadFile(
        url: String,
        file: File,
        onProgress: (Int, Long, Long) -> Unit
    ) {
        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()
        
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("Download failed: ${response.code}")
            }
            
            val responseBody = response.body ?: throw Exception("Response body is null")
            val totalBytes = responseBody.contentLength()
            var downloadedBytes = 0L

            file.outputStream().sink().buffer().use { sink ->
                responseBody.source().use { source ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int

                    while (source.read(buffer).also { bytesRead = it } != -1) {
                        sink.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead

                        val progress = if (totalBytes > 0) {
                            ((downloadedBytes * 100) / totalBytes).toInt()
                        } else {
                            0
                        }

                        onProgress(progress, downloadedBytes, totalBytes)
                    }
                }
            }
        }
    }

    private fun createNotification(
        distro: DistroVariant,
        progress: Int,
        text: String = "Downloading..."
    ): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Downloading Ubuntu")
            .setContentText("${distro.displayName} - $text $progress%")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setProgress(100, progress, false)
            .build()
    }

    private fun updateNotification(
        distro: DistroVariant,
        progress: Int,
        text: String = "Downloading..."
    ) {
        val notification = createNotification(distro, progress, text)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "Rootfs Downloads",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Notifications for rootfs downloads"
        }

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::currentDownloadJob.isInitialized && currentDownloadJob.isActive) {
            currentDownloadJob.cancel()
        }
        Timber.d("RootfsDownloadService destroyed")
    }
}
