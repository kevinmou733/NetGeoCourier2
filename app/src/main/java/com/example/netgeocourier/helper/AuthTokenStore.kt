package com.example.netgeocourier.helper

import android.content.Context

object AuthTokenStore {
    private const val PREFS_NAME = "auth_prefs"
    private const val KEY_ACCESS_TOKEN = "access_token"

    fun saveAccessToken(context: Context, token: String) {
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ACCESS_TOKEN, normalizeToken(token))
            .apply()
    }

    fun getAccessToken(context: Context): String? {
        val token = context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_ACCESS_TOKEN, null)
            ?.trim()
            .orEmpty()

        return token.takeIf { it.isNotEmpty() }?.let(::normalizeToken)
    }

    fun clearAccessToken(context: Context) {
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_ACCESS_TOKEN)
            .apply()
    }

    private fun normalizeToken(rawToken: String): String {
        val trimmed = rawToken.trim()
        return if (trimmed.startsWith("Bearer ", ignoreCase = true)) {
            trimmed.substringAfter(' ').trim()
        } else {
            trimmed
        }
    }
}
