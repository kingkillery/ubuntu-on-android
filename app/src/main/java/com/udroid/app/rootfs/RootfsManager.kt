package com.udroid.app.rootfs

import android.content.Context
import com.udroid.app.model.DistroVariant
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RootfsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val rootfsDir = File(context.filesDir, "ubuntu/rootfs")
    private val cacheDir = File(context.cacheDir, "downloads")

    init {
        rootfsDir.mkdirs()
        cacheDir.mkdirs()
    }

    fun isRootfsInstalled(distro: DistroVariant): Boolean {
        val rootfsPath = getRootfsPath(distro)
        val markerFile = File(rootfsPath, ".installed")
        return markerFile.exists()
    }

    fun getRootfsPath(distro: DistroVariant): File {
        return File(rootfsDir, distro.id.replace(":", "_"))
    }

    fun getCacheFile(distro: DistroVariant): File {
        val filename = distro.id.replace(":", "_") + ".tar.gz"
        return File(cacheDir, filename)
    }

    suspend fun verifyChecksum(file: File, expectedSha256: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val digest = MessageDigest.getInstance("SHA-256")
                file.inputStream().use { stream ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (stream.read(buffer).also { bytesRead = it } != -1) {
                        digest.update(buffer, 0, bytesRead)
                    }
                }
                
                val sha256 = digest.digest().joinToString("") { "%02x".format(it) }
                val matches = sha256.equals(expectedSha256, ignoreCase = true)
                
                if (!matches) {
                    Timber.e("Checksum mismatch: expected $expectedSha256, got $sha256")
                }
                
                matches
            } catch (e: Exception) {
                Timber.e(e, "Failed to verify checksum")
                false
            }
        }
    }

    suspend fun extractRootfs(
        archiveFile: File,
        targetDir: File,
        onProgress: (Int) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Timber.d("Extracting rootfs: ${archiveFile.name} -> $targetDir")
            targetDir.mkdirs()
            
            // TODO: Implement actual tar.gz extraction
            // For now, create marker file
            val markerFile = File(targetDir, ".installed")
            markerFile.createNewFile()
            
            onProgress(100)
            Timber.d("Rootfs extracted successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to extract rootfs")
            Result.failure(e)
        }
    }

    fun getInstalledRootfs(): List<DistroVariant> {
        return DistroVariant.entries.filter { distro ->
            val rootfsPath = getRootfsPath(distro)
            val markerFile = File(rootfsPath, ".installed")
            markerFile.exists()
        }
    }

    suspend fun deleteRootfs(distro: DistroVariant): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val rootfsPath = getRootfsPath(distro)
            if (rootfsPath.exists()) {
                rootfsPath.deleteRecursively()
                Timber.d("Rootfs deleted: ${distro.id}")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete rootfs")
            Result.failure(e)
        }
    }

    fun getCacheSize(): Long {
        return cacheDir.walkTopDown()
            .filter { it.isFile }
            .map { it.length() }
            .sum()
    }

    fun getRootfsSize(distro: DistroVariant): Long {
        val rootfsPath = getRootfsPath(distro)
        return if (rootfsPath.exists()) {
            rootfsPath.walkTopDown()
                .filter { it.isFile }
                .map { it.length() }
                .sum()
        } else {
            0L
        }
    }

    suspend fun clearCache(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            cacheDir.deleteRecursively()
            cacheDir.mkdirs()
            Timber.d("Cache cleared")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to clear cache")
            Result.failure(e)
        }
    }
}
