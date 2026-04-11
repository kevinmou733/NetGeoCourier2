package com.example.netgeocourier.network

import android.content.Context
import com.example.netgeocourier.BuildConfig
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    @Volatile
    private var evaluationApiService: EvaluationApiService? = null

    fun evaluationService(context: Context): EvaluationApiService {
        return evaluationApiService ?: synchronized(this) {
            evaluationApiService ?: buildRetrofit(context.applicationContext)
                .create(EvaluationApiService::class.java)
                .also { evaluationApiService = it }
        }
    }

    private fun buildRetrofit(context: Context): Retrofit {
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(context))
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}
