package com.example.netgeocourier.data

data class NetTestResult(
    val timestamp: String,
    val latitude: Double,
    val longitude: Double,
    val upload: Double,
    val download: Double,
    val ping: Int
)
