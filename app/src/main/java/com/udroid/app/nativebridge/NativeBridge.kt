package com.udroid.app.nativebridge

import android.content.Context
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import android.util.Log
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "NativeBridge"

@Singleton
class NativeBridge @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // Use native library directory which is executable on modern Android
    private val nativeLibDir: File by lazy {
        File(context.applicationInfo.nativeLibraryDir)
    }

    // Directory for versioned libraries (writable)
    private val libDir: File by lazy {
        File(context.filesDir, "lib").also { it.mkdirs() }
    }

    private val prootBinary: File by lazy {
        File(nativeLibDir, "libproot.so")
    }

    private val loaderBinary: File by lazy {
        File(nativeLibDir, "libproot-loader.so")
    }

    private val tallocLib: File by lazy {
        File(nativeLibDir, "libtalloc.so")
    }

    // Versioned talloc library that proot expects
    private val tallocLibVersioned: File by lazy {
        File(libDir, "libtalloc.so.2")
    }

    // Map of sessionId -> Process for tracking multiple concurrent processes
    private val processMap: MutableMap<String, Process> = mutableMapOf()

    init {
        Log.d(TAG, "NativeBridge initialized, nativeLibDir=${nativeLibDir.absolutePath}")
        Timber.d("NativeBridge initialized")
    }

    private fun getAbi(): String {
        return Build.SUPPORTED_ABIS.firstOrNull { abi ->
            abi in listOf("arm64-v8a", "armeabi-v7a", "x86_64")
        } ?: "arm64-v8a"
    }

    suspend fun ensureProotExtracted(): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Checking proot binaries in nativeLibDir: ${nativeLibDir.absolutePath}")

            // Binaries are bundled as native libs - just verify they exist
            val prootExists = prootBinary.exists()
            val loaderExists = loaderBinary.exists()
            val tallocExists = tallocLib.exists()

            Log.d(TAG, "Binary check: proot=$prootExists, loader=$loaderExists, talloc=$tallocExists")
            Log.d(TAG, "proot path: ${prootBinary.absolutePath}")
            Log.d(TAG, "proot canExecute=${prootBinary.canExecute()}, canRead=${prootBinary.canRead()}")

            if (!prootExists || !loaderExists || !tallocExists) {
                Log.e(TAG, "Missing proot binaries in native lib directory!")
                return@withContext false
            }

            // Native libs should already be executable, but verify
            if (!prootBinary.canExecute()) {
                Log.e(TAG, "proot binary is not executable!")
                return@withContext false
            }

            // Copy talloc with versioned name (proot expects libtalloc.so.2)
            if (!tallocLibVersioned.exists() || tallocLibVersioned.length() != tallocLib.length()) {
                Log.d(TAG, "Copying talloc library with versioned name...")
                tallocLib.copyTo(tallocLibVersioned, overwrite = true)
                Log.d(TAG, "Talloc copied to: ${tallocLibVersioned.absolutePath}")
            }

            Log.d(TAG, "PRoot binaries ready in native lib directory")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to verify proot: ${e.message}", e)
            Timber.e(e, "Failed to verify proot")
            false
        }
    }

    suspend fun launchProot(
        sessionId: String,
        rootfsPath: String,
        sessionDir: String,
        bindMounts: List<String>,
        envVars: Map<String, String>,
        command: String,
        timeoutSeconds: Long = 30
    ): com.udroid.app.model.ProcessResult = withContext(Dispatchers.IO) {
        Log.d(TAG, "=== launchProot called ===")
        Log.d(TAG, "sessionId=$sessionId, rootfs=$rootfsPath, command=$command, timeout=${timeoutSeconds}s")
        Timber.d("Launching PRoot: sessionId=$sessionId, rootfs=$rootfsPath, command=$command, timeout=${timeoutSeconds}s")

        Log.d(TAG, "Ensuring proot is extracted...")
        if (!ensureProotExtracted()) {
            Log.e(TAG, "Failed to extract proot binaries!")
            return@withContext com.udroid.app.model.ProcessResult(
                exitCode = -1,
                stdout = "",
                stderr = "Failed to extract proot"
            )
        }
        Log.d(TAG, "PRoot binaries ready: proot=${prootBinary.exists()}, loader=${loaderBinary.exists()}, talloc=${tallocLib.exists()}")

        try {
            Log.d(TAG, "Building process command...")
            val processBuilder = ProcessBuilder().apply {
                val cmd = mutableListOf(
                    prootBinary.absolutePath,
                    "-0",  // Fake root
                    "-r", rootfsPath,  // Root filesystem
                    "-b", "/dev",
                    "-b", "/proc",
                    "-b", "/sys",
                    "-w", "/root"
                )

                // Add bind mounts
                bindMounts.forEach { mount ->
                    cmd.add("-b")
                    cmd.add(mount)
                }

                // Add command - wrap in timeout-aware execution
                cmd.add("/bin/sh")
                cmd.add("-c")
                cmd.add(command.ifEmpty { "echo 'PRoot ready'" })

                Log.d(TAG, "Full command: ${cmd.joinToString(" ")}")
                command(cmd)

                // Set environment
                environment().apply {
                    put("PROOT_LOADER", loaderBinary.absolutePath)
                    // Include both native lib dir and our writable lib dir for versioned libs
                    put("LD_LIBRARY_PATH", "${libDir.absolutePath}:${nativeLibDir.absolutePath}")
                    put("HOME", "/root")
                    put("USER", "root")
                    put("TERM", "xterm-256color")
                    put("LANG", "C.UTF-8")
                    put("PATH", "/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin")
                    envVars.forEach { (k, v) -> put(k, v) }
                }

                directory(File(sessionDir))
                redirectErrorStream(false)
            }

            Log.d(TAG, "Starting proot process...")
            val process = processBuilder.start()

            // Store process in map for tracking
            synchronized(processMap) {
                processMap[sessionId] = process
            }
            Log.d(TAG, "PRoot process started for session $sessionId, waiting for completion (timeout: ${timeoutSeconds}s)")
            Timber.d("PRoot process started for session $sessionId")

            // Read output with timeout
            val stdoutBuilder = StringBuilder()
            val stderrBuilder = StringBuilder()

            // Start readers in background
            val stdoutReader = Thread {
                try {
                    process.inputStream.bufferedReader().forEachLine { line ->
                        stdoutBuilder.appendLine(line)
                    }
                } catch (e: Exception) {
                    Timber.w("Stdout read interrupted: ${e.message}")
                }
            }
            val stderrReader = Thread {
                try {
                    process.errorStream.bufferedReader().forEachLine { line ->
                        stderrBuilder.appendLine(line)
                    }
                } catch (e: Exception) {
                    Timber.w("Stderr read interrupted: ${e.message}")
                }
            }

            stdoutReader.start()
            stderrReader.start()

            // Wait with timeout
            Log.d(TAG, "Calling waitFor with timeout ${timeoutSeconds}s...")
            val completed = process.waitFor(timeoutSeconds, TimeUnit.SECONDS)
            Log.d(TAG, "waitFor returned: completed=$completed")
            val exitCode = if (completed) {
                val code = process.exitValue()
                Log.d(TAG, "Process completed with exit code: $code")
                code
            } else {
                Log.w(TAG, "PRoot command timed out after ${timeoutSeconds}s, killing process")
                Timber.w("PRoot command timed out after ${timeoutSeconds}s, killing process")
                process.destroyForcibly()
                -1
            }

            // Give readers a moment to finish
            stdoutReader.join(500)
            stderrReader.join(500)

            // Remove process from map after completion
            synchronized(processMap) {
                processMap.remove(sessionId)
            }

            val stdout = stdoutBuilder.toString()
            val stderr = stderrBuilder.toString()

            Log.d(TAG, "PRoot exited with code: $exitCode for session $sessionId, stdout: ${stdout.take(200)}, stderr: ${stderr.take(200)}")
            Timber.d("PRoot exited with code: $exitCode for session $sessionId, stdout: ${stdout.take(100)}")

            com.udroid.app.model.ProcessResult(
                exitCode = exitCode,
                stdout = stdout,
                stderr = stderr
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to launch PRoot")
            com.udroid.app.model.ProcessResult(
                exitCode = -1,
                stdout = "",
                stderr = e.message ?: "Unknown error"
            )
        }
    }

    suspend fun waitForProot(sessionId: String, timeoutMs: Long): Int = withContext(Dispatchers.IO) {
        Timber.d("Waiting for PRoot session $sessionId (timeout: $timeoutMs ms)")
        try {
            val process = synchronized(processMap) { processMap[sessionId] }
            process?.waitFor()?.also {
                Timber.d("PRoot session $sessionId exited with code: $it")
                synchronized(processMap) {
                    processMap.remove(sessionId)
                }
            } ?: -1
        } catch (e: Exception) {
            Timber.e(e, "Error waiting for PRoot session $sessionId")
            -1
        }
    }

    suspend fun killProot(sessionId: String, signal: Int): Boolean = withContext(Dispatchers.IO) {
        Timber.d("Killing PRoot session $sessionId with signal $signal")
        try {
            val process = synchronized(processMap) { processMap.remove(sessionId) }
            if (process != null) {
                if (signal == 9) {
                    process.destroyForcibly()
                } else {
                    process.destroy()
                }
                true
            } else {
                Timber.w("No process found for session $sessionId")
                false
            }
        } catch (e: Exception) {
            Timber.e(e, "Error killing PRoot session $sessionId")
            false
        }
    }

    suspend fun isProotRunning(sessionId: String): Boolean = withContext(Dispatchers.IO) {
        val process = synchronized(processMap) { processMap[sessionId] }
        process?.isAlive ?: false
    }

    suspend fun getProotStdout(sessionId: String): String = withContext(Dispatchers.IO) {
        try {
            val process = synchronized(processMap) { processMap[sessionId] }
            process?.inputStream?.bufferedReader()?.readLine() ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    suspend fun getProotStderr(sessionId: String): String = withContext(Dispatchers.IO) {
        try {
            val process = synchronized(processMap) { processMap[sessionId] }
            process?.errorStream?.bufferedReader()?.readLine() ?: ""
        } catch (e: Exception) {
            ""
        }
    }
}
