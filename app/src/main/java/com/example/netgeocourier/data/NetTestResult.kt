package com.example.netgeocourier.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class NetTestResult(
    val timestamp: String,
    val latitude: Double,
    val longitude: Double,
    val upload: Double,
    val download: Double,
    val ping: Int
) : Parcelable
