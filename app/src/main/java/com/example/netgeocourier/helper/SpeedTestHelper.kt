package com.example.netgeocourier.helper

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration.Companion.seconds
import java.io.IOException
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Proxy as JavaNetProxy
import java.net.Socket
import java.net.URL

class SpeedTestHelper(private val context: Context) {
    companion object {
        private const val DOWNLOAD_URL = "https://speed.cloudflare.com/__down?bytes=25000000"
        private const val UPLOAD_URL = "https://speed.cloudflare.com/__up"
        private const val PING_HOST = "1.1.1.1"
        private const val PING_PORT = 53  // 使用DNS端口而不是ICMP
        private const val TIMEOUT_MS = 30000  // 增加到30秒
        private const val TAG = "SpeedTestHelper"
    }

    private fun createConnection(urlString: String): HttpURLConnection {
        val url = URL(urlString)
        val proxy = getSystemProxy()
        val connection = if (proxy != null) {
            Log.d(TAG, "Using system proxy: ${proxy.type()} ${proxy.address()}")
            url.openConnection(proxy) as HttpURLConnection
        } else {
            Log.d(TAG, "No system proxy, using direct connection")
            url.openConnection() as HttpURLConnection
        }
        return connection
    }

    private fun getSystemProxy(): JavaNetProxy? {
        return try {
            val proxyHost = System.getProperty("http.proxyHost")
            val proxyPort = System.getProperty("http.proxyPort")?.toIntOrNull()

            if (!proxyHost.isNullOrEmpty() && proxyPort != null) {
                Log.d(TAG, "System proxy (system properties): $proxyHost:$proxyPort")
                return JavaNetProxy(JavaNetProxy.Type.HTTP, InetSocketAddress(proxyHost, proxyPort))
            }

            val wifiProxy = getWifiProxy()
            if (wifiProxy != null) {
                Log.d(TAG, "System proxy (WiFi config): ${wifiProxy.first}:${wifiProxy.second}")
                return JavaNetProxy(JavaNetProxy.Type.HTTP, InetSocketAddress(wifiProxy.first, wifiProxy.second))
            }

            Log.d(TAG, "No system proxy configured")
            null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get system proxy: ${e.message}", e)
            null
        }
    }

    private fun getWifiProxy(): Pair<String, Int>? {
        return try {
            val proxyHost = System.getProperty("https.proxyHost") ?: System.getProperty("http.proxyHost")
            val proxyPortStr = System.getProperty("https.proxyPort") ?: System.getProperty("http.proxyPort")
            val proxyPort = proxyPortStr?.toIntOrNull()

            if (!proxyHost.isNullOrEmpty() && proxyPort != null) {
                Pair(proxyHost, proxyPort)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun measureDownloadSpeed(): Double = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            withTimeout(TIMEOUT_MS.seconds) {
                Log.d(TAG, "Starting download test...")
                val connection = createConnection(DOWNLOAD_URL).apply {
                    connectTimeout = TIMEOUT_MS
                    readTimeout = TIMEOUT_MS
                    setRequestProperty("User-Agent", "Mozilla/5.0 (Android) Chrome/119.0.0.0")

                    setRequestProperty("Accept-Encoding", "identity")
                }

                connection.connect()
                val responseCode = connection.responseCode
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    Log.w(TAG, "Download test returned non-OK response: $responseCode")
                    return@withTimeout 0.0
                }

                val buffer = ByteArray(32 * 1024)
                var downloadedBytes = 0L
                val startTime = System.currentTimeMillis()

                connection.inputStream.use { input ->
                    var len: Int
                    // 添加读取超时保护，使用循环读取
                    while (input.read(buffer).also { len = it } != -1) {
                        downloadedBytes += len
                        // 防止无限循环，最多读取25MB
                        if (downloadedBytes >= 25_000_000) break
                    }
                }

                val endTime = System.currentTimeMillis()
                val duration = (endTime - startTime) / 1000.0

                Log.d(TAG, "Downloaded $downloadedBytes bytes in ${"%.2f".format(duration)}s")
                if (duration > 0.0 && downloadedBytes > 0) {
                    val speed = (downloadedBytes * 8) / (duration * 1_000_000)
                    Log.d(TAG, "Download speed: ${"%.2f".format(speed)} Mbps")
                    speed
                } else {
                    Log.w(TAG, "Download failed: duration=$duration, downloadedBytes=$downloadedBytes")
                    0.0
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Download speed test failed: ${e.javaClass.simpleName}: ${e.message}", e)
            0.0
        } finally {
            connection?.disconnect()
        }
    }

    suspend fun measureUploadSpeed(): Double = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            withTimeout(TIMEOUT_MS.seconds) {
                Log.d(TAG, "Starting upload test...")
                val data = ByteArray(2 * 1024 * 1024)  // 2MB 上传数据
                val connection = createConnection(UPLOAD_URL).apply {
                    doOutput = true
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/octet-stream")
                    setRequestProperty("Accept-Encoding", "identity")
                    connectTimeout = TIMEOUT_MS
                    readTimeout = TIMEOUT_MS
                }

                connection.connect()
                val start = System.currentTimeMillis()

                connection.outputStream.use { outputStream ->
                    outputStream.write(data)
                    outputStream.flush()
                }


                val code = connection.responseCode
                val end = System.currentTimeMillis()

                val seconds = (end - start) / 1000.0
                Log.d(TAG, "Upload response code: $code, time: ${"%.2f".format(seconds)}s")
                if (seconds > 0 && code in 200..299) {
                    val speed = (data.size * 8) / (seconds * 1_000_000)
                    Log.d(TAG, "Upload speed: ${"%.2f".format(speed)} Mbps")
                    speed
                } else {
                    Log.w(TAG, "Upload failed: code=$code, seconds=$seconds")
                    0.0
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Upload speed test failed: ${e.javaClass.simpleName}: ${e.message}", e)
            0.0
        } finally {
            connection?.disconnect()
        }
    }

    suspend fun measurePing(): Int = withContext(Dispatchers.IO) {
        try {
            withTimeout(8000L) {
                Log.d(TAG, "Starting ping test to $PING_HOST:$PING_PORT...")

                val startTime = System.currentTimeMillis()
                Socket().use { socket ->
                    val socketAddress = InetSocketAddress(PING_HOST, PING_PORT)
                    socket.connect(socketAddress, 5000)  // 5秒连接超时
                    val endTime = System.currentTimeMillis()
                    val pingTime = (endTime - startTime).toInt()
                    Log.d(TAG, "Ping to $PING_HOST:$PING_PORT: ${pingTime}ms")
                    pingTime
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ping test failed: ${e.javaClass.simpleName}: ${e.message}", e)

            999
        }
    }
}