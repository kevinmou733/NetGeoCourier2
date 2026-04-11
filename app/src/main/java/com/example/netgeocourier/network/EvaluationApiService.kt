package com.example.netgeocourier.network

import com.example.netgeocourier.data.ApiEnvelope
import com.example.netgeocourier.data.EvaluationData
import retrofit2.Response
import retrofit2.http.GET

interface EvaluationApiService {
    @GET("api/v1/evaluation")
    suspend fun getEvaluation(): Response<ApiEnvelope<EvaluationData>>
}
