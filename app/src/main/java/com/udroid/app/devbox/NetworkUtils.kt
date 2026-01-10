package com.udroid.app.devbox

import android.content.Context
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkUtils @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun getDeviceIpAddress(): String? {
        return try {
            // Try to get WiFi IP first
            val wifiIp = getWifiIpAddress()
            if (wifiIp != null) {
                Timber.d("Got WiFi IP: $wifiIp")
                return wifiIp
            }

            // Fallback to iterating network interfaces
            NetworkInterface.getNetworkInterfaces()?.toList()?.forEach { networkInterface ->
                if (networkInterface.isUp && !networkInterface.isLoopback) {
                    networkInterface.inetAddresses?.toList()?.forEach { address ->
                        if (address is Inet4Address && !address.isLoopbackAddress) {
                            val ip = address.hostAddress
                            if (ip != null && !ip.startsWith("127.")) {
                                Timber.d("Got network interface IP: $ip")
                                return ip
                            }
                        }
                    }
                }
            }
            Timber.w("Could not determine device IP address")
            null
        } catch (e: Exception) {
            Timber.e(e, "Error getting device IP address")
            null
        }
    }

    private fun getWifiIpAddress(): String? {
        return try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val wifiInfo = wifiManager?.connectionInfo
            val ipInt = wifiInfo?.ipAddress ?: return null

            if (ipInt == 0) return null

            val ip = String.format(
                "%d.%d.%d.%d",
                ipInt and 0xff,
                (ipInt shr 8) and 0xff,
                (ipInt shr 16) and 0xff,
                (ipInt shr 24) and 0xff
            )

            if (ip == "0.0.0.0") null else ip
        } catch (e: Exception) {
            Timber.e(e, "Error getting WiFi IP")
            null
        }
    }

    fun isConnectedToNetwork(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
               (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))
    }

    fun isPortAvailable(port: Int): Boolean {
        return try {
            ServerSocket(port).use { true }
        } catch (e: Exception) {
            false
        }
    }

    fun generateQrBitmap(content: String, size: Int = 512): Bitmap {
        val hints = mapOf(
            EncodeHintType.MARGIN to 1,
            EncodeHintType.CHARACTER_SET to "UTF-8"
        )

        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size, hints)

        val width = bitMatrix.width
        val height = bitMatrix.height
        val pixels = IntArray(width * height)

        for (y in 0 until height) {
            for (x in 0 until width) {
                pixels[y * width + x] = if (bitMatrix[x, y]) {
                    android.graphics.Color.BLACK
                } else {
                    android.graphics.Color.WHITE
                }
            }
        }

        return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.RGB_565)
    }

    fun buildConnectInfo(template: ServiceTemplate, port: Int, bindMode: BindMode): ConnectInfo {
        val ipAddress = getDeviceIpAddress() ?: "localhost"

        return when (template.id) {
            "ssh" -> {
                val command = "ssh -p $port udroid@$ipAddress"
                ConnectInfo(
                    displayText = command,
                    copyText = command,
                    qrContent = command,
                    lanAddress = ipAddress,
                    port = port
                )
            }
            "jupyter" -> {
                val url = "http://$ipAddress:$port"
                ConnectInfo(
                    displayText = url,
                    copyText = url,
                    qrContent = url,
                    lanAddress = ipAddress,
                    port = port
                )
            }
            else -> {
                // Default to HTTP URL format for other services
                val url = "http://$ipAddress:$port"
                ConnectInfo(
                    displayText = url,
                    copyText = url,
                    qrContent = url,
                    lanAddress = ipAddress,
                    port = port
                )
            }
        }
    }
}
