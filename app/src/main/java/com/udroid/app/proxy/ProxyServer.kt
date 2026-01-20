package com.udroid.app.proxy

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import timber.log.Timber
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Native HTTP proxy server that forwards requests through Android's network stack.
 * This solves proot's DNS/network limitations by proxying all HTTP/HTTPS traffic
 * through the host Android system which has proper network access.
 *
 * Default port: 8118 (matches common proxy conventions like Privoxy)
 */
@Singleton
class ProxyServer @Inject constructor() {

    companion object {
        const val DEFAULT_PORT = 8118
        const val DEFAULT_HOST = "127.0.0.1"
        private const val TAG = "ProxyServer"
        private const val SOCKET_TIMEOUT_MS = 30_000
        private const val CONNECT_TIMEOUT_SEC = 30L
        private const val READ_TIMEOUT_SEC = 30L
        private const val WRITE_TIMEOUT_SEC = 30L
    }

    sealed class ProxyState {
        object Stopped : ProxyState()
        data class Starting(val port: Int) : ProxyState()
        data class Running(val port: Int, val host: String) : ProxyState()
        data class Error(val message: String, val cause: Throwable? = null) : ProxyState()
    }

    private val _state = MutableStateFlow<ProxyState>(ProxyState.Stopped)
    val state: StateFlow<ProxyState> = _state.asStateFlow()

    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT_SEC, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT_SEC, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT_SEC, TimeUnit.SECONDS)
            .followRedirects(false) // Let the client handle redirects
            .followSslRedirects(false)
            .build()
    }

    /**
     * Start the proxy server on the specified port.
     * @return Result indicating success or failure with error details
     */
    fun start(port: Int = DEFAULT_PORT): Result<Int> {
        if (_state.value is ProxyState.Running) {
            Timber.tag(TAG).w("Proxy server already running")
            return Result.success(port)
        }

        return try {
            _state.value = ProxyState.Starting(port)

            serverSocket = ServerSocket(port).apply {
                soTimeout = SOCKET_TIMEOUT_MS
                reuseAddress = true
            }

            serverJob = scope.launch {
                acceptConnections()
            }

            _state.value = ProxyState.Running(port, DEFAULT_HOST)
            Timber.tag(TAG).i("Proxy server started on $DEFAULT_HOST:$port")
            Result.success(port)
        } catch (e: IOException) {
            val error = "Failed to start proxy server on port $port: ${e.message}"
            Timber.tag(TAG).e(e, error)
            _state.value = ProxyState.Error(error, e)
            Result.failure(e)
        }
    }

    /**
     * Stop the proxy server and clean up resources.
     */
    fun stop() {
        Timber.tag(TAG).i("Stopping proxy server")
        serverJob?.cancel()
        serverJob = null

        try {
            serverSocket?.close()
        } catch (e: IOException) {
            Timber.tag(TAG).w(e, "Error closing server socket")
        }
        serverSocket = null

        _state.value = ProxyState.Stopped
        Timber.tag(TAG).i("Proxy server stopped")
    }

    /**
     * Get the proxy URL for environment variable configuration.
     * @return Proxy URL in format "http://host:port" or null if not running
     */
    fun getProxyUrl(): String? {
        val currentState = _state.value
        return if (currentState is ProxyState.Running) {
            "http://${currentState.host}:${currentState.port}"
        } else {
            null
        }
    }

    private suspend fun acceptConnections() {
        val socket = serverSocket ?: return

        while (currentCoroutineContext().isActive && !socket.isClosed) {
            try {
                val clientSocket = socket.accept()
                // Handle each connection in a separate coroutine
                scope.launch {
                    handleClient(clientSocket)
                }
            } catch (e: SocketTimeoutException) {
                // Expected when no connections, continue loop
            } catch (e: SocketException) {
                if (!socket.isClosed) {
                    Timber.tag(TAG).w(e, "Socket exception in accept loop")
                }
                // Socket closed, exit loop
                break
            } catch (e: IOException) {
                Timber.tag(TAG).e(e, "IO error accepting connection")
            }
        }
    }

    private suspend fun handleClient(clientSocket: Socket) {
        withContext(Dispatchers.IO) {
            try {
                clientSocket.soTimeout = SOCKET_TIMEOUT_MS
                val reader = BufferedReader(InputStreamReader(clientSocket.getInputStream()))
                val outputStream = clientSocket.getOutputStream()

                // Read the request line
                val requestLine = reader.readLine() ?: return@withContext
                Timber.tag(TAG).d("Proxy request: $requestLine")

                val parts = requestLine.split(" ")
                if (parts.size < 3) {
                    sendErrorResponse(outputStream, 400, "Bad Request")
                    return@withContext
                }

                val method = parts[0]
                val url = parts[1]

                // Read headers
                val headers = mutableMapOf<String, String>()
                var line: String?
                while (reader.readLine().also { line = it } != null && line!!.isNotEmpty()) {
                    val colonIndex = line!!.indexOf(':')
                    if (colonIndex > 0) {
                        val key = line!!.substring(0, colonIndex).trim()
                        val value = line!!.substring(colonIndex + 1).trim()
                        headers[key] = value
                    }
                }

                when (method) {
                    "CONNECT" -> handleConnectRequest(clientSocket, url)
                    else -> handleHttpRequest(clientSocket, method, url, headers, reader)
                }
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Error handling client connection")
            } finally {
                try {
                    clientSocket.close()
                } catch (e: IOException) {
                    // Ignore close errors
                }
            }
        }
    }

    /**
     * Handle CONNECT requests for HTTPS tunneling.
     */
    private fun handleConnectRequest(clientSocket: Socket, hostPort: String) {
        try {
            val parts = hostPort.split(":")
            val host = parts[0]
            val port = if (parts.size > 1) parts[1].toIntOrNull() ?: 443 else 443

            Timber.tag(TAG).d("CONNECT tunnel to $host:$port")

            // Connect to the target server
            val targetSocket = Socket(host, port)
            targetSocket.soTimeout = SOCKET_TIMEOUT_MS

            // Send 200 Connection Established
            val response = "HTTP/1.1 200 Connection Established\r\n\r\n"
            clientSocket.getOutputStream().write(response.toByteArray())
            clientSocket.getOutputStream().flush()

            // Tunnel data bidirectionally
            val clientToTarget = scope.launch {
                tunnel(clientSocket.getInputStream(), targetSocket.getOutputStream())
            }
            val targetToClient = scope.launch {
                tunnel(targetSocket.getInputStream(), clientSocket.getOutputStream())
            }

            // Wait for either direction to complete
            runBlocking {
                try {
                    clientToTarget.join()
                    targetToClient.join()
                } finally {
                    clientToTarget.cancel()
                    targetToClient.cancel()
                    targetSocket.close()
                }
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "CONNECT tunnel error")
            try {
                sendErrorResponse(clientSocket.getOutputStream(), 502, "Bad Gateway")
            } catch (ignore: Exception) {}
        }
    }

    private suspend fun tunnel(input: java.io.InputStream, output: OutputStream) {
        withContext(Dispatchers.IO) {
            try {
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    output.flush()
                }
            } catch (e: IOException) {
                // Connection closed, normal termination
            }
        }
    }

    /**
     * Handle regular HTTP requests (GET, POST, etc.)
     */
    private fun handleHttpRequest(
        clientSocket: Socket,
        method: String,
        url: String,
        headers: Map<String, String>,
        reader: BufferedReader
    ) {
        try {
            val targetUrl = when {
                url.startsWith("http://") || url.startsWith("https://") -> url
                url.startsWith("/") -> {
                    val hostHeader = headers["Host"] ?: headers["host"]
                    if (hostHeader != null) {
                        "http://$hostHeader$url"
                    } else {
                        url
                    }
                }
                else -> {
                    val hostHeader = headers["Host"] ?: headers["host"]
                    if (hostHeader != null && !url.contains("://")) {
                        "http://$hostHeader/$url"
                    } else {
                        url
                    }
                }
            }

            if (!targetUrl.startsWith("http://") && !targetUrl.startsWith("https://")) {
                sendErrorResponse(clientSocket.getOutputStream(), 400, "Bad Request")
                return
            }

            // Build OkHttp request
            val requestBuilder = Request.Builder()
                .url(targetUrl)

            // Add headers (except hop-by-hop headers)
            val hopByHopHeaders = setOf(
                "Proxy-Connection", "Connection", "Keep-Alive",
                "Proxy-Authenticate", "Proxy-Authorization",
                "TE", "Trailers", "Transfer-Encoding", "Upgrade"
            )

            headers.forEach { (key, value) ->
                if (key !in hopByHopHeaders) {
                    requestBuilder.addHeader(key, value)
                }
            }

            // Handle request body for POST, PUT, etc.
            val contentLength = headers["Content-Length"]?.toIntOrNull() ?: 0
            val body = if (contentLength > 0 && method in listOf("POST", "PUT", "PATCH")) {
                val bodyChars = CharArray(contentLength)
                reader.read(bodyChars, 0, contentLength)
                String(bodyChars)
            } else null

            val mediaType = (headers["Content-Type"] ?: "application/octet-stream").toMediaTypeOrNull()

            when (method) {
                "GET" -> requestBuilder.get()
                "HEAD" -> requestBuilder.head()
                "DELETE" -> requestBuilder.delete()
                "POST" -> requestBuilder.post((body ?: "").toRequestBody(mediaType))
                "PUT" -> requestBuilder.put((body ?: "").toRequestBody(mediaType))
                "PATCH" -> requestBuilder.patch((body ?: "").toRequestBody(mediaType))
                else -> {
                    sendErrorResponse(clientSocket.getOutputStream(), 501, "Not Implemented")
                    return
                }
            }

            // Execute the request
            val response: Response = httpClient.newCall(requestBuilder.build()).execute()

            // Forward the response to the client
            forwardResponse(clientSocket.getOutputStream(), response)

        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error proxying HTTP request: $method $url")
            try {
                sendErrorResponse(clientSocket.getOutputStream(), 502, "Bad Gateway")
            } catch (ignore: Exception) {}
        }
    }

    private fun forwardResponse(output: OutputStream, response: Response) {
        val statusLine = "HTTP/1.1 ${response.code} ${response.message}\r\n"
        output.write(statusLine.toByteArray())

        // Forward response headers
        val hopByHopHeaders = setOf(
            "Connection", "Keep-Alive", "Proxy-Authenticate",
            "Proxy-Authorization", "TE", "Trailers",
            "Transfer-Encoding", "Upgrade"
        )

        response.headers.names().forEach { name ->
            if (name !in hopByHopHeaders) {
                response.headers(name).forEach { value ->
                    output.write("$name: $value\r\n".toByteArray())
                }
            }
        }

        output.write("\r\n".toByteArray())

        // Forward response body
        response.body?.let { body ->
            body.byteStream().copyTo(output)
            body.close()
        }

        output.flush()
    }

    private fun sendErrorResponse(output: OutputStream, code: Int, message: String) {
        val body = "<html><body><h1>$code $message</h1></body></html>"
        val response = "HTTP/1.1 $code $message\r\n" +
            "Content-Type: text/html\r\n" +
            "Content-Length: ${body.length}\r\n" +
            "Connection: close\r\n" +
            "\r\n" +
            body

        output.write(response.toByteArray())
        output.flush()
    }
}
