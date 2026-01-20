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
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream
import java.io.BufferedInputStream
import java.io.File
import java.io.InputStream

import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

data class ChecksumEntry(val filename: String, val sha256: String)

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
        // Determine extension from download URL or default to .tar.gz
        val extension = when {
            distro.downloadUrl?.endsWith(".tar.xz") == true -> ".tar.xz"
            distro.downloadUrl?.endsWith(".tar.gz") == true -> ".tar.gz"
            else -> ".tar.gz"
        }
        val filename = distro.id.replace(":", "_") + extension
        return File(cacheDir, filename)
    }

    fun getTempDir(): File {
        val tempDir = File(context.cacheDir, "tmp")
        tempDir.mkdirs()
        return tempDir
    }

    suspend fun verifyChecksum(file: File, expectedSha256: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val actualSha256 = computeSha256(file)
                val matches = actualSha256.equals(expectedSha256, ignoreCase = true)

                if (!matches) {
                    Timber.e("Checksum mismatch: expected $expectedSha256, got $actualSha256")
                }

                matches
            } catch (e: Exception) {
                Timber.e(e, "Failed to verify checksum")
                false
            }
        }
    }

    /**
     * Computes SHA-256 hash of a file.
     */
    private fun computeSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { stream ->
            val buffer = ByteArray(64 * 1024)
            var bytesRead: Int
            while (stream.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    /**
     * Fetches and parses the SHA256SUMS file from Ubuntu cloud images.
     * Returns list of filename -> sha256 entries.
     */
    private fun parseSha256Sums(contents: String): List<ChecksumEntry> {
        return contents.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .mapNotNull { line ->
                // Format: "<hash> <filename>" or "<hash> *<filename>"
                val parts = line.split(Regex("\\s+"), limit = 2)
                if (parts.size < 2) return@mapNotNull null
                val hash = parts[0]
                val name = parts[1].trim().removePrefix("*")
                ChecksumEntry(name, hash)
            }
            .toList()
    }

    /**
     * Fetches SHA256SUMS from the same directory as the download URL
     * and verifies the downloaded file against it.
     *
     * @param downloadedFile The file that was downloaded
     * @param downloadUrl The original download URL (used to derive SHA256SUMS location)
     * @return true if verification passed, false otherwise
     */
    suspend fun verifyChecksumFromServer(downloadedFile: File, downloadUrl: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Derive SHA256SUMS URL from download URL
                // e.g., https://cloud-images.ubuntu.com/jammy/current/jammy-server-cloudimg-arm64-root.tar.xz
                // becomes https://cloud-images.ubuntu.com/jammy/current/SHA256SUMS
                val baseUrl = downloadUrl.substringBeforeLast("/")
                val filename = downloadUrl.substringAfterLast("/")
                val sha256SumsUrl = "$baseUrl/SHA256SUMS"

                Timber.d("Fetching checksums from: $sha256SumsUrl")

                // Fetch SHA256SUMS file
                val sha256SumsContent = try {
                    URL(sha256SumsUrl).readText()
                } catch (e: Exception) {
                    Timber.w(e, "Could not fetch SHA256SUMS, skipping verification")
                    return@withContext true // Skip verification if we can't fetch checksums
                }

                // Parse and find our file's checksum
                val entries = parseSha256Sums(sha256SumsContent)
                val expectedChecksum = entries.find { it.filename == filename }?.sha256

                if (expectedChecksum == null) {
                    Timber.w("No checksum found for $filename in SHA256SUMS")
                    return@withContext true // Skip if file not in checksums
                }

                // Verify
                val actualChecksum = computeSha256(downloadedFile)
                val matches = actualChecksum.equals(expectedChecksum, ignoreCase = true)

                if (matches) {
                    Timber.d("Checksum verified: $filename")
                } else {
                    Timber.e("Checksum mismatch for $filename: expected $expectedChecksum, got $actualChecksum")
                }

                matches
            } catch (e: Exception) {
                Timber.e(e, "Failed to verify checksum from server")
                true // Don't block on verification errors
            }
        }
    }

    /**
     * Creates the appropriate decompressor based on file extension.
     * Supports .tar.gz (gzip) and .tar.xz (xz) formats.
     */
    private fun createDecompressor(archiveFile: File, bis: BufferedInputStream): InputStream {
        return when {
            archiveFile.name.endsWith(".tar.xz") -> XZCompressorInputStream(bis)
            archiveFile.name.endsWith(".tar.gz") -> GzipCompressorInputStream(bis)
            else -> GzipCompressorInputStream(bis) // Default to gzip
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
                    createDecompressor(archiveFile, bis).use { decompressor ->
                        TarArchiveInputStream(decompressor).use { tais ->
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
