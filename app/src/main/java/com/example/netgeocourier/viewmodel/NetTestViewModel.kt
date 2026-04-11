package com.example.netgeocourier.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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
        if (_isTesting.value || _isAutoTesting.value) return

        _isTesting.value = true
        _curResult.value = null

        coroutineScope.launch {
            val context = getApplication<Application>()
            val location = locationHelper.getCurrentLocation()
            if (location == null) {
                _isTesting.value = false
                onError("Failed to get location. Please check permissions.")
                onFinish?.invoke()
                return@launch
            }

            val download = SpeedTestHelper.measureDownloadSpeed()
            val upload = SpeedTestHelper.measureUploadSpeed()
            val ping = SpeedTestHelper.measurePing()

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
            _isTesting.value = false
            onFinish?.invoke()
        }
    }

    fun startAutoTest() {
        if (_isAutoTesting.value) return
        _isAutoTesting.value = true
        val job = coroutineScope.launch {
            while (_isAutoTesting.value) {
                val deferred = CompletableDeferred<Unit>()
                doTest { deferred.complete(Unit) }
                deferred.await()
                if (_isAutoTesting.value) {
                    delay(5000)
                }
            }
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
