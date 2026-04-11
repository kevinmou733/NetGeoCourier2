package com.example.netgeocourier.network

import com.example.netgeocourier.data.NetTestResult
import com.example.netgeocourier.data.RecordLocation
import com.example.netgeocourier.data.RecordMetrics
import com.example.netgeocourier.data.RecordUploadRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

class RecordRepository(
    private val apiService: RecordApiService
) {
    suspend fun uploadResult(result: NetTestResult): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.uploadRecord(
                RecordUploadRequest(
                    capturedAt = result.timestamp,
                    metrics = RecordMetrics(
                        downloadMbps = result.download,
                        uploadMbps = result.upload,
                        pingMs = result.ping
                    ),
                    location = RecordLocation(
                        latitude = result.latitude,
                        longitude = result.longitude
                    ),
                    remark = "由 Android 客户端自动同步。"
                )
            )

            if (!response.isSuccessful) {
                throw IOException(parseApiError(response, "同步测速记录失败。"))
            }

            val body = response.body() ?: throw IOException("服务器返回的测速记录同步结果为空。")
            if (!body.success) {
                throw IOException(body.message.ifBlank { "同步测速记录失败。" })
            }
            Result.success(Unit)
        } catch (throwable: Throwable) {
            Result.failure(normalizeApiException(throwable))
        }
    }
}
