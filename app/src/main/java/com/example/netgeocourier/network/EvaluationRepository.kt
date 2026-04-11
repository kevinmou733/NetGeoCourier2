package com.example.netgeocourier.network

import com.example.netgeocourier.data.EvaluationData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

class EvaluationRepository(
    private val apiService: EvaluationApiService
) {
    suspend fun getEvaluation(): Result<EvaluationData> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getEvaluation()
            if (!response.isSuccessful) {
                throw IOException(parseApiError(response, "加载网络评估失败。"))
            }

            val body = response.body() ?: throw IOException("服务器返回为空，请稍后重试。")
            if (!body.success || body.data == null) {
                throw IOException(body.message.ifBlank { "加载网络评估失败。" })
            }

            Result.success(body.data)
        } catch (throwable: Throwable) {
            Result.failure(normalizeApiException(throwable))
        }
    }
}
