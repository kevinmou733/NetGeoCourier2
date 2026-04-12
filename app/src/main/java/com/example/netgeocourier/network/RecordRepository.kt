package com.example.netgeocourier.network

import com.example.netgeocourier.data.ApiEnvelope
import com.example.netgeocourier.data.NetTestResult
import com.example.netgeocourier.data.RecordBatchUploadData
import com.example.netgeocourier.data.RecordBatchUploadRequest
import com.example.netgeocourier.data.RecordLocation
import com.example.netgeocourier.data.RecordMetrics
import com.example.netgeocourier.data.RecordUploadData
import com.example.netgeocourier.data.RecordUploadRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response
import java.io.IOException

class RecordRepository(
    private val apiService: RecordApiService
) {
    suspend fun uploadResult(result: NetTestResult): Result<Unit> = withContext(Dispatchers.IO) {
        syncResults(listOf(result)).fold(
            onSuccess = { Result.success(Unit) },
            onFailure = { Result.failure(it) }
        )
    }

    suspend fun syncResults(results: List<NetTestResult>): Result<Int> = withContext(Dispatchers.IO) {
        if (results.isEmpty()) {
            return@withContext Result.success(0)
        }

        try {
            var syncedCount = 0
            results.chunked(MAX_BATCH_SIZE).forEach { chunk ->
                syncedCount += if (chunk.size == 1) {
                    uploadSingle(chunk.first())
                } else {
                    uploadBatch(chunk)
                }
            }
            Result.success(syncedCount)
        } catch (throwable: Throwable) {
            Result.failure(normalizeApiException(throwable))
        }
    }

    private suspend fun uploadSingle(result: NetTestResult): Int {
        val response = apiService.uploadRecord(result.toUploadRequest())
        val body = requireSuccessfulResponse(response)
        if (body.data?.record == null) {
            throw IOException("Server returned an empty record sync result.")
        }
        return 1
    }

    private suspend fun uploadBatch(results: List<NetTestResult>): Int {
        val response = apiService.uploadBatch(
            RecordBatchUploadRequest(records = results.map { it.toUploadRequest() })
        )
        val body = requireSuccessfulResponse(response)
        return body.data?.count ?: results.size
    }

    private fun <T> requireSuccessfulResponse(response: Response<ApiEnvelope<T>>): ApiEnvelope<T> {
        if (!response.isSuccessful) {
            throw IOException(parseApiError(response, "Failed to sync test records."))
        }

        val body = response.body() ?: throw IOException("Server returned an empty record sync result.")
        if (!body.success) {
            throw IOException(body.message.ifBlank { "Failed to sync test records." })
        }
        return body
    }

    private fun NetTestResult.toUploadRequest(): RecordUploadRequest {
        return RecordUploadRequest(
            capturedAt = timestamp,
            metrics = RecordMetrics(
                downloadMbps = download,
                uploadMbps = upload,
                pingMs = ping
            ),
            location = RecordLocation(
                latitude = latitude,
                longitude = longitude
            ),
            remark = "Synced automatically from the Android app."
        )
    }

    private companion object {
        const val MAX_BATCH_SIZE = 100
    }
}
