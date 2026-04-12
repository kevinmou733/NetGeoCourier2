package com.example.netgeocourier.data

data class RecordUploadRequest(
    val capturedAt: String,
    val metrics: RecordMetrics,
    val location: RecordLocation,
    val remark: String = ""
)

data class RecordBatchUploadRequest(
    val records: List<RecordUploadRequest>
)

data class RecordMetrics(
    val downloadMbps: Double,
    val uploadMbps: Double,
    val pingMs: Int
)

data class RecordLocation(
    val latitude: Double,
    val longitude: Double,
    val source: String = "android-client"
)

data class RecordUploadData(
    val record: UploadedRecord? = null
)

data class RecordBatchUploadData(
    val count: Int = 0
)

data class UploadedRecord(
    val id: String = ""
)
