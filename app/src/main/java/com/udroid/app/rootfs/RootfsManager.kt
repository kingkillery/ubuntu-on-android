package com.udroid.app.rootfs

import android.content.Context
import com.udroid.app.model.DistroVariant
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import android.util.Log
import timber.log.Timber
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import java.io.BufferedInputStream
import java.io.File

import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "RootfsManager"

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
        val installed = markerFile.exists()
        Log.d(TAG, "isRootfsInstalled(${distro.id}): path=$rootfsPath, markerExists=$installed")
        return installed
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

            val totalSize = archiveFile.length()
            var extractedSize = 0L

            FileInputStream(archiveFile).use { fis ->
                BufferedInputStream(fis).use { bis ->
                    GzipCompressorInputStream(bis).use { gzis ->
                        TarArchiveInputStream(gzis).use { tais ->
                            var entry: TarArchiveEntry? = tais.nextEntry
                            while (entry != null) {
                                val outputFile = File(targetDir, entry.name)

                                if (entry.isDirectory) {
                                    outputFile.mkdirs()
                                } else {
                                    // Ensure parent directory exists
                                    outputFile.parentFile?.mkdirs()

                                    // Handle symlinks
                                    if (entry.isSymbolicLink) {
                                        val linkTarget = entry.linkName
                                        Timber.d("Creating symlink: ${entry.name} -> $linkTarget")
                                        try {
                                            outputFile.delete()
                                            Files.createSymbolicLink(
                                                outputFile.toPath(),
                                                Paths.get(linkTarget)
                                            )
                                        } catch (e: Exception) {
                                            Timber.w("Failed to create symlink ${entry.name}: ${e.message}")
                                        }
                                    } else if (entry.isLink) {
                                        // Hard link - copy the target file
                                        val linkTarget = File(targetDir, entry.linkName)
                                        if (linkTarget.exists()) {
                                            linkTarget.copyTo(outputFile, overwrite = true)
                                        }
                                    } else {
                                        // Extract regular file
                                        FileOutputStream(outputFile).use { fos ->
                                            val buffer = ByteArray(8192)
                                            var bytesRead: Int
                                            while (tais.read(buffer).also { bytesRead = it } != -1) {
                                                fos.write(buffer, 0, bytesRead)
                                            }
                                        }

                                        // Set executable permission if needed
                                        if (entry.mode and 0b001_000_000 != 0) {
                                            outputFile.setExecutable(true, false)
                                        }
                                    }
                                }

                                extractedSize += entry.size
                                val progress = ((extractedSize.toFloat() / totalSize) * 100).toInt().coerceIn(0, 99)
                                onProgress(progress)

                                entry = tais.nextEntry
                            }
                        }
                    }
                }
            }

            // Create marker file to indicate successful extraction
            val markerFile = File(targetDir, ".installed")
            markerFile.createNewFile()

            onProgress(100)
            Timber.d("Rootfs extracted successfully to $targetDir")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to extract rootfs")
            // Clean up partial extraction
            targetDir.deleteRecursively()
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

    suspend fun installBundledRootfs(
        distro: DistroVariant,
        onProgress: (Int) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        Log.d(TAG, "=== installBundledRootfs called for ${distro.id} ===")
        if (!distro.bundled || distro.assetPath == null) {
            Log.e(TAG, "Distro ${distro.id} is not bundled or has no assetPath")
            return@withContext Result.failure(IllegalArgumentException("Distro ${distro.id} is not bundled"))
        }

        try {
            Log.d(TAG, "Installing bundled rootfs: ${distro.id} from ${distro.assetPath}")
            Timber.d("Installing bundled rootfs: ${distro.id} from ${distro.assetPath}")
            val targetDir = getRootfsPath(distro)
            targetDir.mkdirs()

            onProgress(5)

            context.assets.open(distro.assetPath).use { assetStream ->
                BufferedInputStream(assetStream).use { bis ->
                    GzipCompressorInputStream(bis).use { gzis ->
                        TarArchiveInputStream(gzis).use { tais ->
                            var entry: TarArchiveEntry? = tais.nextEntry
                            var entryCount = 0
                            while (entry != null) {
                                val outputFile = File(targetDir, entry.name)

                                if (entry.isDirectory) {
                                    outputFile.mkdirs()
                                } else {
                                    outputFile.parentFile?.mkdirs()

                                    if (entry.isSymbolicLink) {
                                        val linkTarget = entry.linkName
                                        Timber.d("Creating symlink: ${entry.name} -> $linkTarget")
                                        try {
                                            outputFile.delete()
                                            Files.createSymbolicLink(
                                                outputFile.toPath(),
                                                Paths.get(linkTarget)
                                            )
                                        } catch (e: Exception) {
                                            Timber.w("Failed to create symlink ${entry.name}: ${e.message}")
                                        }
                                    } else if (entry.isLink) {
                                        // Hard link - copy the target file
                                        val linkTarget = File(targetDir, entry.linkName)
                                        if (linkTarget.exists()) {
                                            linkTarget.copyTo(outputFile, overwrite = true)
                                        }
                                    } else {
                                        FileOutputStream(outputFile).use { fos ->
                                            val buffer = ByteArray(8192)
                                            var bytesRead: Int
                                            while (tais.read(buffer).also { bytesRead = it } != -1) {
                                                fos.write(buffer, 0, bytesRead)
                                            }
                                        }

                                        if (entry.mode and 0b001_000_000 != 0) {
                                            outputFile.setExecutable(true, false)
                                        }
                                    }
                                }

                                entryCount++
                                // Estimate progress (Alpine has ~600 files)
                                val progress = (5 + (entryCount * 90 / 700)).coerceIn(5, 95)
                                onProgress(progress)

                                entry = tais.nextEntry
                            }
                        }
                    }
                }
            }

            val markerFile = File(targetDir, ".installed")
            markerFile.createNewFile()

            onProgress(100)
            Timber.d("Bundled rootfs installed successfully: ${distro.id}")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to install bundled rootfs")
            val targetDir = getRootfsPath(distro)
            targetDir.deleteRecursively()
            Result.failure(e)
        }
    }
}
