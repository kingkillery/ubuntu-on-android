package com.udroid.app.proxy

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages proxy server lifecycle across multiple sessions.
 * Uses reference counting to start/stop proxy only when needed.
 */
@Singleton
class ProxyManager @Inject constructor(
    private val proxyServer: ProxyServer
) {
    private val mutex = Mutex()
    private var referenceCount = 0

    companion object {
        private const val TAG = "ProxyManager"
    }

    /**
     * Acquire a proxy reference for a session.
     * Starts the proxy if this is the first reference.
     *
     * @return Proxy URL if successful, null if proxy failed to start
     */
    suspend fun acquire(): String? = mutex.withLock {
        referenceCount++
        Timber.tag(TAG).d("Proxy reference acquired, count=$referenceCount")

        if (referenceCount == 1) {
            // First reference, start the proxy
            proxyServer.start().fold(
                onSuccess = { proxyServer.getProxyUrl() },
                onFailure = { e ->
                    Timber.tag(TAG).e(e, "Failed to start proxy")
                    referenceCount--
                    null
                }
            )
        } else {
            proxyServer.getProxyUrl()
        }
    }

    /**
     * Release a proxy reference.
     * Stops the proxy if this was the last reference.
     */
    suspend fun release() = mutex.withLock {
        if (referenceCount > 0) {
            referenceCount--
            Timber.tag(TAG).d("Proxy reference released, count=$referenceCount")

            if (referenceCount == 0) {
                proxyServer.stop()
            }
        }
    }

    /**
     * Get current proxy URL if proxy is running.
     */
    fun getProxyUrl(): String? = proxyServer.getProxyUrl()

    /**
     * Get current reference count (for debugging).
     */
    fun getReferenceCount(): Int = referenceCount

    /**
     * Force stop the proxy regardless of reference count.
     * Use with caution - only for cleanup scenarios.
     */
    suspend fun forceStop() = mutex.withLock {
        Timber.tag(TAG).w("Force stopping proxy, current refCount=$referenceCount")
        referenceCount = 0
        proxyServer.stop()
    }
}
