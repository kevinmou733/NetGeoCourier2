package com.example.netgeocourier.helper

import android.content.Context
import com.example.netgeocourier.data.AuthSession
import com.example.netgeocourier.data.AuthUser

object AuthTokenStore {
    private const val PREFS_NAME = "auth_prefs"
    private const val KEY_ACCESS_TOKEN = "access_token"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_USERNAME = "username"
    private const val KEY_DISPLAY_NAME = "display_name"
    private const val KEY_STUDENT_ID = "student_id"
    private const val KEY_CREATED_AT = "created_at"
    private const val KEY_UPDATED_AT = "updated_at"

    fun saveAccessToken(context: Context, token: String) {
        saveSession(context, token, null)
    }

    fun saveSession(context: Context, token: String, user: AuthUser?) {
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ACCESS_TOKEN, normalizeToken(token))
            .putString(KEY_USER_ID, user?.id)
            .putString(KEY_USERNAME, user?.username)
            .putString(KEY_DISPLAY_NAME, user?.displayName)
            .putString(KEY_STUDENT_ID, user?.studentId)
            .putString(KEY_CREATED_AT, user?.createdAt)
            .putString(KEY_UPDATED_AT, user?.updatedAt)
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

    fun getSession(context: Context): AuthSession? {
        val prefs = context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val accessToken = prefs.getString(KEY_ACCESS_TOKEN, null)
            ?.trim()
            .orEmpty()
            .takeIf { it.isNotEmpty() }
            ?.let(::normalizeToken)
            ?: return null

        val username = prefs.getString(KEY_USERNAME, null).orEmpty()
        val user = if (username.isNotBlank()) {
            AuthUser(
                id = prefs.getString(KEY_USER_ID, null).orEmpty(),
                username = username,
                displayName = prefs.getString(KEY_DISPLAY_NAME, null).orEmpty().ifBlank { username },
                studentId = prefs.getString(KEY_STUDENT_ID, null).orEmpty(),
                createdAt = prefs.getString(KEY_CREATED_AT, null).orEmpty(),
                updatedAt = prefs.getString(KEY_UPDATED_AT, null).orEmpty()
            )
        } else {
            null
        }

        return AuthSession(
            accessToken = accessToken,
            user = user
        )
    }

    fun clearAccessToken(context: Context) {
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
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
