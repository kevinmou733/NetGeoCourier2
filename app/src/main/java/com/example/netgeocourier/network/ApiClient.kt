package com.example.netgeocourier.network

import android.content.Context
import com.example.netgeocourier.BuildConfig
import com.example.netgeocourier.helper.ApiConfigStore
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    fun authService(context: Context): AuthApiService {
        return buildRetrofit(baseUrl = ApiConfigStore.getBaseUrl(context), withAuth = false, context = null)
            .create(AuthApiService::class.java)
    }

    fun evaluationService(context: Context): EvaluationApiService {
        return buildRetrofit(
            baseUrl = ApiConfigStore.getBaseUrl(context),
            withAuth = true,
            context = context.applicationContext
        ).create(EvaluationApiService::class.java)
    }

    fun recordService(context: Context): RecordApiService {
        return buildRetrofit(
            baseUrl = ApiConfigStore.getBaseUrl(context),
            withAuth = true,
            context = context.applicationContext
        ).create(RecordApiService::class.java)
    }

    private fun buildRetrofit(baseUrl: String, withAuth: Boolean, context: Context?): Retrofit {
        val clientBuilder = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)

        if (withAuth && context != null) {
            clientBuilder.addInterceptor(AuthInterceptor(context))
        }

        return Retrofit.Builder()
            .baseUrl(baseUrl.ifBlank { BuildConfig.API_BASE_URL })
            .client(clientBuilder.build())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}
