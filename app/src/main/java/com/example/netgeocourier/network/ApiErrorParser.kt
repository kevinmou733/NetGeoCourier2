package com.example.netgeocourier.network

import java.io.IOException
import org.json.JSONObject
import retrofit2.Response

internal fun parseApiError(response: Response<*>, fallback: String): String {
    val fallbackMessage = when (response.code()) {
        401 -> "登录状态已失效，请重新登录。"
        404 -> "请求的接口不存在。"
        else -> fallback
    }

    val rawBody = response.errorBody()?.string().orEmpty()
    if (rawBody.isBlank()) {
        return fallbackMessage
    }

    return try {
        val root = JSONObject(rawBody)
        root.optJSONObject("error")?.optString("message")
            ?.takeIf { it.isNotBlank() }
            ?: root.optString("message").takeIf { it.isNotBlank() }
            ?: fallbackMessage
    } catch (_: Exception) {
        fallbackMessage
    }
}

internal fun normalizeApiException(throwable: Throwable): Throwable {
    val message = throwable.message.orEmpty()
    val isConnectionIssue = throwable is IOException && (
        message.contains("failed to connect", ignoreCase = true) ||
            message.contains("timeout", ignoreCase = true) ||
            message.contains("timed out", ignoreCase = true) ||
            message.contains("unable to resolve host", ignoreCase = true)
        )

    return if (isConnectionIssue) {
        IOException(
            "连接服务器失败，请确认后端已经启动，并检查服务器地址是否正确。模拟器可用 http://10.0.2.2:3000/，真机请填写电脑局域网 IP 地址。",
            throwable
        )
    } else {
        throwable
    }
}
