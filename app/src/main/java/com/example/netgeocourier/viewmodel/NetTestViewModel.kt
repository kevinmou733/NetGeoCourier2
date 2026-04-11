package com.example.netgeocourier.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.netgeocourier.R
import com.example.netgeocourier.data.NetTestResult
import com.example.netgeocourier.helper.LocationHelper
import com.example.netgeocourier.helper.SpeedTestHelper
import com.example.netgeocourier.helper.FileHelper

import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.*

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

private const val TAG = "NetTestViewModel"

// 页面枚举
enum class AppPage {
    TEST, EVALUATION
}

class NetTestViewModel(application: Application) : AndroidViewModel(application) {
    var currentPage by mutableStateOf(AppPage.TEST)
    //private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)
    val locationHelper = LocationHelper(application)

    private val _isTesting = MutableStateFlow(false)
    val isTesting: StateFlow<Boolean> = _isTesting.asStateFlow()

    private val _isAutoTesting = MutableStateFlow(false)
    val isAutoTesting: StateFlow<Boolean> = _isAutoTesting.asStateFlow()

    private val _autoJob = MutableStateFlow<Job?>(null)
    val autoJob: StateFlow<Job?> = _autoJob.asStateFlow()

    private val _testResults = MutableStateFlow(listOf<NetTestResult>())
    val testResults: StateFlow<List<NetTestResult>> = _testResults.asStateFlow()

    private val _curResult = MutableStateFlow<NetTestResult?>(null)
    val curResult: StateFlow<NetTestResult?> = _curResult.asStateFlow()

    private val _csvPath = MutableStateFlow<String?>(null)
    val csvPath: StateFlow<String?> = _csvPath.asStateFlow()

    private val _htmlPath = MutableStateFlow<String?>(null)
    val htmlPath: StateFlow<String?> = _htmlPath.asStateFlow()

    private val coroutineScope = CoroutineScope(Dispatchers.Main + viewModelScope.coroutineContext)

    fun doTest(onFinish: (() -> Unit)? = null, onError: (String) -> Unit = {}) {
        if (_isTesting.value) return

        _isTesting.value = true
        _curResult.value = null

        coroutineScope.launch {
            Log.d(TAG, "doTest started")
            try {
                val context = getApplication<Application>()
                val location = try {
                    Log.d(TAG, "Getting location...")
                    locationHelper.getCurrentLocation()
                } catch (e: Exception) {
                    Log.e(TAG, "Location exception: ${e.message}", e)
                    null
                }

                if (location == null) {
                    Log.w(TAG, "Location is null")
                    val (errorCode, errorInfo) = locationHelper.getLastError()
                    val errorMessage = when (errorCode) {
                        0 -> "Unknown location error"
                        -1 -> "Location permission denied. Please grant location permission."
                        -2 -> "Location client initialization failed: $errorInfo"
                        -3 -> "Location callback returned null"
                        12 -> "Missing required location permission. Check AndroidManifest.xml."
                        4 -> "Network error. Check internet connection."
                        5 -> "GPS is disabled. Please enable location services."
                        6 -> "Device network is disabled."
                        15 -> "AMap API Key is invalid. Check your API key configuration."
                        else -> "Location failed (error $errorCode): $errorInfo"
                    }
                    Log.e(TAG, errorMessage)
                    onError(errorMessage)
                    onFinish?.invoke()
                    return@launch
                }

                Log.d(TAG, "Location obtained: ${location.latitude}, ${location.longitude}")

                Log.d(TAG, "Measuring download speed...")
                val download = SpeedTestHelper.measureDownloadSpeed()
                Log.d(TAG, "Download: ${download} Mbps")

                Log.d(TAG, "Measuring upload speed...")
                val upload = SpeedTestHelper.measureUploadSpeed()
                Log.d(TAG, "Upload: ${upload} Mbps")

                Log.d(TAG, "Measuring ping...")
                val ping = SpeedTestHelper.measurePing()
                Log.d(TAG, "Ping: ${ping} ms")

                val timeStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                val result = NetTestResult(
                    timestamp = timeStr,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    upload = upload,
                    download = download,
                    ping = ping
                )

                _curResult.value = result
                _testResults.value = _testResults.value + result
                Log.d(TAG, "Test result added. Total: ${_testResults.value.size}")
            } catch (e: Exception) {
                Log.e(TAG, "doTest exception: ${e.message}", e)
                onError("Test failed: ${e.message}")
            } finally {
                _isTesting.value = false
                Log.d(TAG, "doTest finished, _isTesting=false")
                onFinish?.invoke()
            }
        }
    }

    fun startAutoTest() {
        if (_isAutoTesting.value) return
        _isAutoTesting.value = true
        Log.d(TAG, "startAutoTest: auto testing started")
        val job = coroutineScope.launch {
            var count = 0
            while (_isAutoTesting.value) {
                count++
                Log.d(TAG, "Auto test iteration $count")
                val deferred = CompletableDeferred<Unit>()
                // 确保 onFinish 不会抛出异常
                doTest(
                    onFinish = {
                        Log.d(TAG, "doTest onFinish called")
                        try {
                            deferred.complete(Unit)
                        } catch (e: Exception) {
                            Log.e(TAG, "deferred complete failed: ${e.message}", e)
                        }
                    },
                    onError = { err ->
                        Log.e(TAG, "doTest error: $err")
                        try {
                            deferred.complete(Unit) // 即使错误也继续
                        } catch (e: Exception) {
                            Log.e(TAG, "deferred complete failed: ${e.message}", e)
                        }
                    }
                )
                Log.d(TAG, "Waiting for deferred...")
                try {
                    deferred.await()
                    Log.d(TAG, "Deferred completed")
                } catch (e: Exception) {
                    Log.e(TAG, "deferred.await exception: ${e.message}", e)
                }
                if (_isAutoTesting.value) {
                    Log.d(TAG, "Waiting 5 seconds before next test...")
                    try {
                        delay(5000)
                    } catch (e: Exception) {
                        Log.e(TAG, "delay interrupted: ${e.message}", e)
                    }
                }
            }
            Log.d(TAG, "Auto test loop exited")
        }
        _autoJob.value = job
    }

    fun stopAutoTest() {
        _isAutoTesting.value = false
        _autoJob.value?.cancel()
        _autoJob.value = null
    }

    fun saveCsv(context: android.content.Context) {
        _csvPath.value = FileHelper.saveCsv(context, _testResults.value)
    }

    fun saveAmapHtml(context: android.content.Context) {
        _htmlPath.value = FileHelper.saveAmapHtml(context, _testResults.value)
    }

    fun sendEmail(context: android.content.Context) {
        FileHelper.sendEmail(context, _csvPath.value, _htmlPath.value)
    }

    override fun onCleared() {
        super.onCleared()
        stopAutoTest()
        //coroutineScope.cancel()
    }
}
