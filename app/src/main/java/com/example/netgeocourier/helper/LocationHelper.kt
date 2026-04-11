package com.example.netgeocourier.helper

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.amap.api.location.AMapLocation
import com.amap.api.location.AMapLocationClient
import com.amap.api.location.AMapLocationClientOption
import com.amap.api.location.AMapLocationListener
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration.Companion.milliseconds
import kotlin.coroutines.resume

private const val TAG = "LocationHelper"

object PermissionHelper {
    fun getRequiredPermissions(): Array<String> {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        return permissions.toTypedArray()
    }

    fun hasLocationPermission(context: Context): Boolean {
        val fineGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        Log.d(TAG, "Permission check - FINE: $fineGranted, COARSE: $coarseGranted")
        return fineGranted || coarseGranted
    }

    fun registerPermissionLauncher(
        activity: ComponentActivity,
        onResult: (allGranted: Boolean) -> Unit
    ) {
        activity.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
            val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            onResult(fineLocationGranted || coarseLocationGranted)
        }.launch(getRequiredPermissions())
    }
}

class LocationHelper(private val context: Context) : AMapLocationListener {

    private var locationClient: AMapLocationClient? = null
    private var locationContinuation: kotlin.coroutines.Continuation<AMapLocation?>? = null
    private var lastErrorCode: Int = -100
    private var lastErrorInfo: String = ""

    init {
        try {
            // 从 Manifest 获取 API Key 并设置
            val apiKey = context.packageManager
                .getApplicationInfo(context.packageName, 128)
                .metaData
                ?.getString("com.amap.api.v2.apikey")
            
            if (!apiKey.isNullOrEmpty()) {
                AMapLocationClient.setApiKey(apiKey)
                Log.d(TAG, "AMap API Key set: ${apiKey.take(5)}...")
            } else {
                Log.w(TAG, "AMap API Key not found in manifest")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set AMap API Key", e)
        }
    }

    /**
     * 获取最后一次定位错误码（用于调试）
     */
    fun getLastError(): Pair<Int, String> {
        return lastErrorCode to lastErrorInfo
    }

    /**
     * 获取当前定位信息（协程挂起方式），带25秒超时
     */
    suspend fun getCurrentLocation() = withTimeout(25000.milliseconds) {
        suspendCancellableCoroutine<AMapLocation?> { cont ->
            Log.d(TAG, "getCurrentLocation() called")

            // 检查权限
            if (!PermissionHelper.hasLocationPermission(context)) {
                Log.w(TAG, "No location permission")
                lastErrorCode = -1
                lastErrorInfo = "Location permission denied"
                cont.resume(null)
                return@suspendCancellableCoroutine
            }

            locationContinuation = cont

            try {
                Log.d(TAG, "Initializing AMapLocationClient...")

                // 隐私合规
                AMapLocationClient.updatePrivacyShow(context, true, true)
                AMapLocationClient.updatePrivacyAgree(context, true)
                Log.d(TAG, "Privacy compliance updated")

                locationClient = AMapLocationClient(context.applicationContext).apply {
                    setLocationListener(this@LocationHelper)
                    setLocationOption(createLocationOption())
                    startLocation()
                }
                Log.d(TAG, "AMapLocationClient started")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start location client: ${e.message}", e)
                lastErrorCode = -2
                lastErrorInfo = "Client init failed: ${e.message}"
                cont.resume(null)
            }

            cont.invokeOnCancellation {
                Log.d(TAG, "Location request cancelled or timeout")
                lastErrorCode = -3
                lastErrorInfo = "Location timeout or cancelled"
                stopLocation()
                locationContinuation = null
            }
        }
    }

    /**
     * 创建定位参数配置
     */
    private fun createLocationOption(): AMapLocationClientOption {
        return AMapLocationClientOption().apply {
            locationMode = AMapLocationClientOption.AMapLocationMode.Hight_Accuracy
            isNeedAddress = true
            isWifiActiveScan = true
            isOnceLocation = true  // 单次定位
            httpTimeOut = 20000
            // 单次定位无需设置 interval
        }
    }

    /**
     * 高德定位回调
     */
    override fun onLocationChanged(location: AMapLocation?) {
        stopLocation()

        location?.let { loc ->
            if (loc.errorCode == 0) {
                // 定位成功
                Log.d(TAG, "Location success: lat=${loc.latitude}, lon=${loc.longitude}")
                lastErrorCode = 0
                lastErrorInfo = "Success"
                locationContinuation?.resume(loc)
            } else {
                // 定位失败
                lastErrorCode = loc.errorCode
                lastErrorInfo = loc.errorInfo ?: "Unknown error"
                Log.e(TAG, "Location failed: errorCode=${loc.errorCode}, errorInfo=${loc.errorInfo}")
                locationContinuation?.resume(null)
            }
        } ?: run {
            lastErrorCode = -3
            lastErrorInfo = "Location callback returned null"
            Log.w(TAG, "Location is null")
            locationContinuation?.resume(null)
        }

        locationContinuation = null
    }

    /**
     * 停止定位
     */
    fun stopLocation() {
        try {
            locationClient?.stopLocation()
            locationClient?.unRegisterLocationListener(this)
            locationClient = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping location", e)
        }
    }
}
