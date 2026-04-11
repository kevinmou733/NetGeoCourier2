package com.example.netgeocourier.viewmodel

import android.app.Application
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.netgeocourier.R
import com.example.netgeocourier.data.NetTestResult
import com.example.netgeocourier.helper.AuthTokenStore
import com.example.netgeocourier.helper.LocationHelper
import com.example.netgeocourier.helper.SpeedTestHelper
import com.example.netgeocourier.helper.FileHelper

import com.example.netgeocourier.network.ApiClient
import com.example.netgeocourier.network.RecordRepository


import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

private const val TAG = "NetTestViewModel"

// 页面枚举
enum class AppPage {
    AUTH, TEST, EVALUATION
}

class NetTestViewModel(application: Application) : AndroidViewModel(application) {
    var currentPage by mutableStateOf(AppPage.TEST)
    val locationHelper = LocationHelper(application)
    private val speedTestHelper = SpeedTestHelper(application)

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

    // 追踪已保存到CSV的记录数量
    private var savedRecordCount = 0

    private val coroutineScope = CoroutineScope(Dispatchers.Main + viewModelScope.coroutineContext)
    private val recordRepository = RecordRepository(
        ApiClient.recordService(application.applicationContext)
    )

    init {
        // 从CSV加载历史数据
        try {
            val context = getApplication<Application>()
            val csvFile = FileHelper.getCsvFile(context)
            if (csvFile != null && csvFile.exists()) {
                val records = csvFile.readLines()
                    .drop(1)  // 跳过表头
                    .filter { it.isNotBlank() }
                    .map { line ->
                        val parts = line.split(",")
                        if (parts.size >= 6) {
                            NetTestResult(
                                timestamp = parts[0],
                                longitude = parts[1].toDoubleOrNull() ?: 0.0,
                                latitude = parts[2].toDoubleOrNull() ?: 0.0,
                                upload = parts[3].toDoubleOrNull() ?: 0.0,
                                download = parts[4].toDoubleOrNull() ?: 0.0,
                                ping = parts[5].toIntOrNull() ?: 0
                            )
                        } else null
                    }.filterNotNull()
                _testResults.value = records
                savedRecordCount = records.size
                Log.d(TAG, "Loaded ${records.size} historical records from CSV, savedRecordCount=$savedRecordCount")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load CSV history: ${e.message}", e)
        }
    }

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
                val download = speedTestHelper.measureDownloadSpeed()
                Log.d(TAG, "Download: ${download} Mbps")

                Log.d(TAG, "Measuring upload speed...")
                val upload = speedTestHelper.measureUploadSpeed()
                Log.d(TAG, "Upload: ${upload} Mbps")

                Log.d(TAG, "Measuring ping...")
                val ping = speedTestHelper.measurePing()
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
                syncRecordIfNeeded(result)

                // 自动追加到CSV（确保数据不丢失）
                try {
                    val context = getApplication<Application>()
                    FileHelper.appendCsvRecord(context, result)
                    savedRecordCount = _testResults.value.size  // 同步已保存计数
                    Log.d(TAG, "Auto-saved record to CSV, savedRecordCount=$savedRecordCount")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to auto-save CSV: ${e.message}", e)
                }

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
                            deferred.complete(Unit)
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

    fun openDataDirectory(context: android.content.Context) {
        val csvFile = FileHelper.getCsvFile(context)
        if (csvFile == null || !csvFile.exists()) {
            Toast.makeText(context, "No data file found", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                csvFile.parentFile!!  // 获取父目录
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "resource/folder")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Open data folder"))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open data directory: ${e.message}", e)
            Toast.makeText(context, "Cannot open folder: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun openCsvFile(context: android.content.Context) {
        val csvFile = FileHelper.getCsvFile(context)
        if (csvFile == null || !csvFile.exists()) {
            Toast.makeText(context, "No CSV file found", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                csvFile
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "text/csv")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open CSV file: ${e.message}", e)
            Toast.makeText(context, "Cannot open CSV: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun saveAmapHtml(context: android.content.Context) {
        FileHelper.generateAndOpenMap(context, testResults.value)
    }

    fun sendEmail(context: android.content.Context) {
        val csvFile = FileHelper.getCsvFile(context)
        val csvPath = csvFile?.takeIf { it.exists() }?.absolutePath

        if (csvPath == null) {
            Toast.makeText(context, "No CSV data to send", Toast.LENGTH_SHORT).show()
            return
        }

        FileHelper.sendEmailCsvOnly(context, csvPath)
    }

    fun deleteAllHistory(context: android.content.Context) {
        try {
            val csvFile = FileHelper.getCsvFile(context)
            if (csvFile != null && csvFile.exists()) {
                csvFile.delete()
                Toast.makeText(context, "All history deleted", Toast.LENGTH_SHORT).show()
            }

            _testResults.value = emptyList()
            _curResult.value = null
            savedRecordCount = 0
            _csvPath.value = null
            _htmlPath.value = null

            Log.d(TAG, "All history deleted")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete history: ${e.message}", e)
            Toast.makeText(context, "Delete failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun syncRecordIfNeeded(result: NetTestResult) {
        val context = getApplication<Application>()
        if (AuthTokenStore.getAccessToken(context).isNullOrBlank()) {
            return
        }

        coroutineScope.launch {
            recordRepository.uploadResult(result)
                .onFailure { throwable ->
                    Log.w(TAG, "Failed to sync record: ${throwable.message}", throwable)
                }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopAutoTest()
    }
}
