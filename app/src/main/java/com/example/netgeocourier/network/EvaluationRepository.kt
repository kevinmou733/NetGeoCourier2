package com.example.netgeocourier.network

import com.example.netgeocourier.data.EvaluationData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

class EvaluationRepository(
    private val apiService: EvaluationApiService
) {
    suspend fun getEvaluation(): Result<EvaluationData> = withContext(Dispatchers.IO) {
        runCatching {
            val response = apiService.getEvaluation()
            if (!response.isSuccessful) {
                throw IOException("Request failed: HTTP ${response.code()}")
            }

            val body = response.body() ?: throw IOException("Response body is empty.")
            if (!body.success || body.data == null) {
                throw IOException(body.message.ifBlank { "Evaluation request failed." })
            }

            body.data
        }
    }
}
