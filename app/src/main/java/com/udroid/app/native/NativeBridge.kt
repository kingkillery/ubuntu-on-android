package com.udroid.app.native

import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NativeBridge @Inject constructor() {
    
    init {
        try {
            System.loadLibrary("udroid-native")
            Timber.d("Native library loaded successfully")
        } catch (e: UnsatisfiedLinkError) {
            Timber.w("Native library not available - using stub implementation")
        }
    }

    suspend fun launchProot(
        rootfsPath: String,
        sessionDir: String,
        bindMounts: List<String>,
        envVars: Map<String, String>,
        command: String
    ): com.udroid.app.model.ProcessResult {
        Timber.d("Launching PRoot: rootfs=$rootfsPath, session=$sessionDir")
        
        // Stub implementation - will be replaced with JNI call
        return com.udroid.app.model.ProcessResult(
            exitCode = 0,
            stdout = "PRoot launched (stub)",
            stderr = ""
        )
    }

    suspend fun waitForProot(pid: Long, timeoutMs: Long): Int {
        Timber.d("Waiting for PRoot PID $pid (timeout: $timeoutMs ms)")
        return 0
    }

    suspend fun killProot(pid: Long, signal: Int): Boolean {
        Timber.d("Killing PRoot PID $pid with signal $signal")
        return true
    }

    suspend fun isProotRunning(pid: Long): Boolean {
        return false
    }

    suspend fun getProotStdout(pid: Long): String {
        return ""
    }

    suspend fun getProotStderr(pid: Long): String {
        return ""
    }
}
