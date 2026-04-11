package com.example.netgeocourier.data

data class ApiEnvelope<T>(
    val success: Boolean,
    val message: String,
    val data: T?
)

data class EvaluationData(
    val score: Int = 0,
    val level: String = "unknown",
    val suggestions: List<String> = emptyList(),
    val metrics: EvaluationMetrics = EvaluationMetrics(),
    val recordCount: Int = 0
)

data class EvaluationMetrics(
    val downloadAvg: Double? = null,
    val pingAvg: Double? = null,
    val rssiAvg: Double? = null,
    val snrAvg: Double? = null
)
