package com.example.netgeocourier.helper

import android.content.Context
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration.Companion.milliseconds
import java.io.BufferedReader
import java.io.FileReader
import java.io.IOException
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Proxy as JavaNetProxy
import java.net.URL

class SpeedTestHelper(private val context: Context) {
    companion object {
        private const val DOWNLOAD_URL = "https://speed.cloudflare.com/__down?bytes=25000000"
        private const val UPLOAD_URL = "https://speed.cloudflare.com/__up"
        private const val PING_HOST = "1.1.1.1"
        private const val TIMEOUT_MS = 25000
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
            // 方法1: 通过系统属性读取（最可靠）
            val proxyHost = System.getProperty("http.proxyHost")
            val proxyPort = System.getProperty("http.proxyPort")?.toIntOrNull()

            if (!proxyHost.isNullOrEmpty() && proxyPort != null) {
                Log.d(TAG, "System proxy (system properties): $proxyHost:$proxyPort")
                return JavaNetProxy(JavaNetProxy.Type.HTTP, InetSocketAddress(proxyHost, proxyPort))
            }

            // 方法2: 读取 WiFi 代理配置（Android 系统存储）
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
            // Android WiFi 代理配置存储在 /data/misc/wifi/ 或通过 WifiManager 获取
            // 这里尝试读取系统属性
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
        try {
            withTimeout(TIMEOUT_MS.milliseconds) {
                val connection = createConnection(DOWNLOAD_URL).apply {
                    connectTimeout = 15000
                    readTimeout = 15000
                    setRequestProperty("User-Agent", "Mozilla/5.0 (Android) Chrome/119.0.0.0")
                }

                val buffer = ByteArray(32 * 1024)
                var downloadedBytes = 0L
                val startTime = System.currentTimeMillis()

                connection.inputStream.use { input ->
                    var len: Int
                    while (input.read(buffer).also { len = it } != -1) {
                        downloadedBytes += len
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
            when (e) {
                is java.net.UnknownHostException -> Log.e(TAG, "DNS解析失败 - 检查网络或Hosts文件")
                is javax.net.ssl.SSLHandshakeException -> Log.e(TAG, "SSL握手失败 - 可能是证书问题")
                is java.net.SocketTimeoutException -> Log.e(TAG, "连接超时 - 服务器无响应或网络慢")
                is IOException -> Log.e(TAG, "IO错误: ${e.message}")
            }
            0.0
        }
    }

    suspend fun measureUploadSpeed(): Double = withContext(Dispatchers.IO) {
        try {
            withTimeout(TIMEOUT_MS.milliseconds) {
                val data = ByteArray(2 * 1024 * 1024)
                val connection = createConnection(UPLOAD_URL).apply {
                    doOutput = true
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/octet-stream")
                    connectTimeout = 15000
                    readTimeout = 15000
                }

                val start = System.currentTimeMillis()
                connection.outputStream.use { it.write(data) }
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
            when (e) {
                is java.net.UnknownHostException -> Log.e(TAG, "DNS解析失败 - 检查网络或Hosts文件")
                is javax.net.ssl.SSLHandshakeException -> Log.e(TAG, "SSL握手失败 - 可能是证书问题")
                is java.net.SocketTimeoutException -> Log.e(TAG, "连接超时 - 服务器无响应或网络慢")
                is IOException -> Log.e(TAG, "IO错误: ${e.message}")
            }
            0.0
        }
    }

    suspend fun measurePing(): Int = withContext(Dispatchers.IO) {
        try {
            withTimeout(5000.milliseconds) {
                val address = java.net.InetAddress.getByName(PING_HOST)
                val startTime = System.currentTimeMillis()
                val isReachable = address.isReachable(3000)
                val duration = System.currentTimeMillis() - startTime
                if (isReachable) {
                    Log.d(TAG, "Ping to $PING_HOST: ${duration}ms")
                    duration.toInt()
                } else {
                    Log.w(TAG, "Ping to $PING_HOST failed - host not reachable")
                    -1
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ping test failed: ${e.javaClass.simpleName}: ${e.message}", e)
            when (e) {
                is java.net.UnknownHostException -> Log.e(TAG, "DNS解析失败 - 1.1.1.1 无法解析")
                is java.net.SocketTimeoutException -> Log.e(TAG, "Ping超时")
            }
            -1
        }
    }
}
