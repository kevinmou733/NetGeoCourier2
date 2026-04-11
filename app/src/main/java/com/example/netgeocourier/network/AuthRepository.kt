package com.example.netgeocourier.network

import com.example.netgeocourier.data.AuthPayload
import com.example.netgeocourier.data.LoginRequest
import com.example.netgeocourier.data.RegisterRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

class AuthRepository(
    private val apiService: AuthApiService
) {
    suspend fun login(username: String, password: String): Result<AuthPayload> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.login(
                LoginRequest(
                    username = username.trim(),
                    password = password
                )
            )
            Result.success(
                readAuthResponse(
                    response.isSuccessful,
                    response.code(),
                    response.body(),
                    parseApiError(response, "登录失败，请稍后重试。")
                )
            )
        } catch (throwable: Throwable) {
            Result.failure(normalizeApiException(throwable))
        }
    }

    suspend fun register(
        username: String,
        password: String,
        displayName: String,
        studentId: String
    ): Result<AuthPayload> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.register(
                RegisterRequest(
                    username = username.trim(),
                    password = password,
                    displayName = displayName.trim(),
                    studentId = studentId.trim()
                )
            )
            Result.success(
                readAuthResponse(
                    response.isSuccessful,
                    response.code(),
                    response.body(),
                    parseApiError(response, "注册失败，请稍后重试。")
                )
            )
        } catch (throwable: Throwable) {
            Result.failure(normalizeApiException(throwable))
        }
    }

    private fun readAuthResponse(
        isSuccessful: Boolean,
        statusCode: Int,
        body: com.example.netgeocourier.data.ApiEnvelope<AuthPayload>?,
        errorMessage: String
    ): AuthPayload {
        if (!isSuccessful) {
            throw IOException(errorMessage)
        }

        if (body == null) {
            throw IOException("服务器返回为空，请稍后重试。")
        }

        if (!body.success || body.data == null) {
            val message = body.message.ifBlank {
                if (statusCode in 200..299) "认证失败，请稍后重试。" else errorMessage
            }
            throw IOException(message)
        }

        return body.data
    }
}
