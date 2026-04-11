package com.example.netgeocourier.network

import com.example.netgeocourier.data.ApiEnvelope
import com.example.netgeocourier.data.RecordUploadData
import com.example.netgeocourier.data.RecordUploadRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface RecordApiService {
    @POST("api/v1/records")
    suspend fun uploadRecord(@Body request: RecordUploadRequest): Response<ApiEnvelope<RecordUploadData>>
}
