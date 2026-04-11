package com.example.netgeocourier.helper

import android.content.Context
import com.example.netgeocourier.BuildConfig

object ApiConfigStore {
    private const val PREFS_NAME = "api_config_prefs"
    private const val KEY_BASE_URL = "base_url"

    fun getBaseUrl(context: Context): String {
        val rawValue = context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_BASE_URL, null)
            .orEmpty()

        return normalizeBaseUrl(rawValue.ifBlank { BuildConfig.API_BASE_URL })
    }

    fun saveBaseUrl(context: Context, input: String): String {
        val normalized = normalizeBaseUrl(input)
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_BASE_URL, normalized)
            .apply()
        return normalized
    }

    private fun normalizeBaseUrl(input: String): String {
        val trimmed = input.trim()
        val withScheme = if (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) {
            trimmed
        } else {
            "http://$trimmed"
        }

        return if (withScheme.endsWith("/")) withScheme else "$withScheme/"
    }
}
