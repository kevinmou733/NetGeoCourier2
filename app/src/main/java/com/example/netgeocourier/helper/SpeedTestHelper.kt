package com.example.netgeocourier.helper

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.InetAddress

object SpeedTestHelper {
    private const val DOWNLOAD_URL = "https://speed.cloudflare.com/__down?bytes=25000000"
    private const val UPLOAD_URL = "https://speed.cloudflare.com/__up"
    private const val PING_HOST = "1.1.1.1"

    suspend fun measureDownloadSpeed(): Double = withContext(Dispatchers.IO) {
        try {
            val connection = URL(DOWNLOAD_URL).openConnection() as HttpURLConnection
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Android) Chrome/119.0.0.0")

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

            if (duration > 0.0 && downloadedBytes > 0) {
                (downloadedBytes * 8) / (duration * 1_000_000)
            } else 0.0
        } catch (e: Exception) {
            e.printStackTrace()
            0.0
        }
    }

    suspend fun measureUploadSpeed(): Double = withContext(Dispatchers.IO) {
        try {
            val data = ByteArray(2 * 1024 * 1024)
            val connection = URL(UPLOAD_URL).openConnection() as HttpURLConnection
            connection.doOutput = true
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/octet-stream")
            connection.connectTimeout = 15000
            connection.readTimeout = 15000

            val start = System.currentTimeMillis()
            connection.outputStream.use { it.write(data) }
            val code = connection.responseCode
            val end = System.currentTimeMillis()

            val seconds = (end - start) / 1000.0
            if (seconds > 0 && code in 200..299) {
                (data.size * 8) / (seconds * 1_000_000)
            } else 0.0
        } catch (e: Exception) {
            e.printStackTrace()
            0.0
        }
    }

// 使用 InetAddress 实现 ping 功能（无需 root，更安全）
        suspend fun measurePing(): Int = withContext(Dispatchers.IO) {
            try {
                val address = InetAddress.getByName(PING_HOST)
                val startTime = System.currentTimeMillis()
                // 超时 3 秒
                val isReachable = address.isReachable(3000)
                val duration = System.currentTimeMillis() - startTime
                if (isReachable) duration.toInt() else -1
            } catch (e: Exception) {
                -1
            }
        }
}
