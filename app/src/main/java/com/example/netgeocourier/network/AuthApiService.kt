package com.example.netgeocourier.network

import com.example.netgeocourier.data.ApiEnvelope
import com.example.netgeocourier.data.AuthPayload
import com.example.netgeocourier.data.LoginRequest
import com.example.netgeocourier.data.RegisterRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApiService {
    @POST("api/v1/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<ApiEnvelope<AuthPayload>>

    @POST("api/v1/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<ApiEnvelope<AuthPayload>>
}
