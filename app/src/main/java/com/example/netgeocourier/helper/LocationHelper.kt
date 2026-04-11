package com.example.netgeocourier.helper

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.amap.api.location.AMapLocation
import com.amap.api.location.AMapLocationClient
import com.amap.api.location.AMapLocationClientOption
import com.amap.api.location.AMapLocationListener
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

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
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    context, Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
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

    /**
     * 获取当前定位信息（协程挂起方式）
     */
    suspend fun getCurrentLocation() = suspendCancellableCoroutine { cont ->
        if (!PermissionHelper.hasLocationPermission(context)) {
            cont.resume(null)
            return@suspendCancellableCoroutine
        }

        locationContinuation = cont

        try {
            // 初始化定位客户端
            locationClient = AMapLocationClient(context.applicationContext).apply {
                setLocationListener(this@LocationHelper)
                setLocationOption(createLocationOption())
                startLocation()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            cont.resume(null)
        }

        cont.invokeOnCancellation {
            stopLocation()
            locationContinuation = null
        }
    }

    /**
     * 创建定位参数配置
     */
    private fun createLocationOption(): AMapLocationClientOption {
        return AMapLocationClientOption().apply {
            // 设置定位模式为高精度模式
            locationMode = AMapLocationClientOption.AMapLocationMode.Hight_Accuracy
            // 设置是否返回地址信息
            isNeedAddress = true
            // 设置是否返回地理边界信息
            //isBounds = true
            // 设置是否返回卫星信息
            //isSatelliteNavigation = false
            // 设置是否返回WIFI信息
            isWifiActiveScan = true
            // 设置是否返回POI信息
            isOnceLocation = true  // 单次定位
            // 设置定位超时时间（毫秒）
            httpTimeOut = 20000
            // 设置间隔时间（毫秒）用于连续定位
            interval = 2000
        }
    }

    /**
     * 高德定位回调
     */
    override fun onLocationChanged(location: AMapLocation?) {
        stopLocation()

        location?.let {
            if (it.errorCode == 0) {
                // 定位成功
                locationContinuation?.resume(it)
            } else {
                // 定位失败
                it.errorCode.let { errorCode ->
                    android.util.Log.e("LocationHelper", "定位失败, 错误码: $errorCode, 错误信息: ${it.errorInfo}")
                }
                locationContinuation?.resume(null)
            }
        } ?: run {
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
            e.printStackTrace()
        }
    }
}