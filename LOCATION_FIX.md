# 🌟 LocationHelper 定位失败问题 - MVVM 完整修复方案

## 📋 文档说明

本文档详细记录了 `LocationHelper` 定位失败的根本原因分析，并提供完整的 **MVVM 架构重构方案**，修复权限管理、状态同步、ViewModel 未使用等核心问题。

---

## 🔍 问题根源分析

### 1. 核心问题：权限请求与定位逻辑完全分离

**MainActivity.kt (第45-49行)**:
```kotlin
private fun requestPermissions() {
    PermissionHelper.registerPermissionLauncher(this) {
        // Permission granted  ← 空回调！什么都没做
    }
}
```

**问题表现**：
- 权限授予后没有任何回调逻辑
- `NetTestScreen` 在 `onCreate` 时立即渲染
- `LocationHelper.getCurrentLocation()` 在权限未授予时直接返回 `null` (LocationHelper.kt:63-66)
- 用户看到"定位失败"，但实际上权限请求可能还没完成

### 2. ViewModel 创建但未使用

**MainActivity.kt**:
```kotlin
lateinit var viewmodel:NetTestViewModel  // 声明了
viewmodel = ViewModelProvider(this).get(NetTestViewModel::class.java)  // 初始化了
NetTestScreen(locationHelper)  // ❌ 传入了 locationHelper，没用 viewmodel！
```

**NetTestScreen.kt**:
- 接收 `LocationHelper` 参数
- 所有状态在 Composable 内部管理
- ❌ 完全没用到 ViewModel

**NetTestViewModel.kt**:
- 已完整实现所有状态管理和业务逻辑
- 但 `doTest()`、`startAutoTest()` 等方法从未被调用

### 3. 定位参数潜在问题

LocationHelper.kt:104:
```kotlin
isOnceLocation = true  // 单次定位
httpTimeOut = 20000    // 20秒超时
```
如果高德 SDK 初始化慢或信号弱，可能超时失败。

### 4. 缺少定位服务检查

没有检查设备的 GPS/网络定位是否开启。

---

## ✅ 完整修复步骤

### 步骤 0：准备工作 - 检查依赖

确保 `app/build.gradle.kts` 包含以下依赖：

```kotlin
dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.activity.compose)
    // ... 其他依赖
}
```

如果缺少 `lifecycle-viewmodel-compose`，添加：
```kotlin
implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")
```

---

### 步骤 1：修改 MainActivity.kt

**目标**：传递 ViewModel 给 UI，正确传递权限结果

```kotlin
package com.example.netgeocourier

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.example.netgeocourier.helper.PermissionHelper
import com.example.netgeocourier.screen.NetTestScreen
import com.example.netgeocourier.ui.theme.NetGeoCourierTheme
import com.example.netgeocourier.viewmodel.NetTestViewModel

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: NetTestViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. 初始化 ViewModel
        viewModel = ViewModelProvider(this)[NetTestViewModel::class.java]

        setContent {
            NetGeoCourierTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // 2. ✅ 传递 ViewModel，不再传递 LocationHelper
                    NetTestScreen(viewModel = viewModel)
                }
            }
        }

        // 3. 请求定位权限
        requestLocationPermissions()
    }

    private fun requestLocationPermissions() {
        PermissionHelper.registerPermissionLauncher(this) { permissions ->
            // 4. 权限结果回调 → 通知 ViewModel
            val granted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                         permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true

            if (granted) {
                viewModel.onPermissionGranted()
            } else {
                viewModel.onPermissionDenied()
            }
        }
    }
}
```

**关键改动**：
- ✅ 变量名 `viewmodel` → `viewModel`（遵循 Kotlin 命名规范）
- ✅ `NetTestScreen(viewModel = viewModel)` 只传 ViewModel
- ✅ 权限回调中调用 `viewModel.onPermissionGranted()` / `onPermissionDenied()`

---

### 步骤 2：重写 NetTestViewModel.kt

**目标**：成为唯一的真相来源（Single Source of Truth）

创建或替换 `app/src/main/java/com/example/netgeocourier/viewmodel/NetTestViewModel.kt`：

```kotlin
package com.example.netgeocourier.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.netgeocourier.data.NetTestResult
import com.example.netgeocourier.helper.LocationHelper
import com.example.netgeocourier.helper.PermissionHelper
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
import java.util.Locale
import java.util.Date

class NetTestViewModel(application: Application) : AndroidViewModel(application) {

    // ========== 依赖注入 ==========
    private val locationHelper = LocationHelper(application)

    // ========== UI 状态（StateFlow） ==========
    private val _uiState = MutableStateFlow(NetTestUiState())
    val uiState: StateFlow<NetTestUiState> = _uiState.asStateFlow()

    // ========== 一次性事件（Event） ==========
    private val _event = MutableStateFlow<NetTestEvent?>(null)
    val event: StateFlow<NetTestEvent?> = _event.asStateFlow()

    init {
        // 初始化时检查权限状态
        checkPermission()
    }

    private fun checkPermission() {
        val context = getApplication<Application>()
        val hasPermission = PermissionHelper.hasLocationPermission(context)
        _uiState.value = _uiState.value.copy(hasLocationPermission = hasPermission)
    }

    // ========== 权限处理方法 ==========
    fun onPermissionGranted() {
        _uiState.value = _uiState.value.copy(hasLocationPermission = true)
    }

    fun onPermissionDenied() {
        _uiState.value = _uiState.value.copy(hasLocationPermission = false)
        _event.value = NetTestEvent.ShowPermissionDeniedDialog
    }

    fun onDismissPermissionDialog() {
        _event.value = null
    }

    fun openAppSettings() {
        val context = getApplication<Application>()
        val intent = Intent(
            android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        ).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        _event.value = null
    }

    // ========== 测试控制方法 ==========
    fun doTest(onFinish: (() -> Unit)? = null) {
        val state = _uiState.value

        // 校验：是否正在测试
        if (state.isTesting || state.isAutoTesting) return

        // 校验：是否有权限
        if (!state.hasLocationPermission) {
            _event.value = NetTestEvent.ShowPermissionDeniedDialog
            return
        }

        _uiState.value = state.copy(isTesting = true, currentResult = null)

        viewModelScope.launch {
            try {
                // 1. 获取位置
                val location = locationHelper.getCurrentLocation()
                if (location == null) {
                    _event.value = NetTestEvent.LocationFailed("无法获取位置信息")
                    _uiState.value = state.copy(isTesting = false)
                    onFinish?.invoke()
                    return@launch
                }

                // 2. 执行网速测试（并发）
                val downloadDeferred = async(Dispatchers.IO) {
                    SpeedTestHelper.measureDownloadSpeed()
                }
                val uploadDeferred = async(Dispatchers.IO) {
                    SpeedTestHelper.measureUploadSpeed()
                }
                val pingDeferred = async(Dispatchers.IO) {
                    SpeedTestHelper.measurePing()
                }

                val download = downloadDeferred.await()
                val upload = uploadDeferred.await()
                val ping = pingDeferred.await()

                // 3. 保存结果
                val timeStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    .format(Date())
                val result = NetTestResult(
                    timestamp = timeStr,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    upload = upload,
                    download = download,
                    ping = ping
                )

                val newResults = _uiState.value.testResults + result
                _uiState.value = _uiState.value.copy(
                    isTesting = false,
                    currentResult = result,
                    testResults = newResults
                )

                _event.value = NetTestEvent.TestCompleted(result)
                onFinish?.invoke()

            } catch (e: Exception) {
                e.printStackTrace()
                _event.value = NetTestEvent.TestFailed(e.message ?: "未知错误")
                _uiState.value = state.copy(isTesting = false)
                onFinish?.invoke()
            }
        }
    }

    fun startAutoTest() {
        val state = _uiState.value
        if (state.isAutoTesting || !state.hasLocationPermission) return

        _uiState.value = state.copy(isAutoTesting = true)

        val job = viewModelScope.launch {
            while (_uiState.value.isAutoTesting) {
                val deferred = CompletableDeferred<Unit>()
                doTest { deferred.complete(Unit) }
                deferred.await()
                // 只有仍在自动测试才延迟
                if (_uiState.value.isAutoTesting) {
                    delay(5000)
                }
            }
        }
        _uiState.value = _uiState.value.copy(autoJob = job)
    }

    fun stopAutoTest() {
        _uiState.value = _uiState.value.copy(isAutoTesting = false)
        _uiState.value.autoJob?.cancel()
        _uiState.value = _uiState.value.copy(autoJob = null)
    }

    // ========== 文件操作方法 ==========
    fun saveCsv() {
        val context = getApplication<Application>()
        val path = FileHelper.saveCsv(context, _uiState.value.testResults)
        _uiState.value = _uiState.value.copy(csvPath = path)
        _event.value = path?.let { NetTestEvent.CsvSaved(it) }
    }

    fun saveAmapHtml() {
        val context = getApplication<Application>()
        val path = FileHelper.saveAmapHtml(context, _uiState.value.testResults)
        _uiState.value = _uiState.value.copy(htmlPath = path)
        _event.value = path?.let { NetTestEvent.HtmlSaved(it) }
    }

    fun sendEmail() {
        val context = getApplication<Application>()
        FileHelper.sendEmail(
            context,
            _uiState.value.csvPath,
            _uiState.value.htmlPath
        )
    }

    // ========== 事件消费 ==========
    fun consumeEvent(event: NetTestEvent) {
        if (_event.value == event) {
            _event.value = null
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopAutoTest()
        locationHelper.stopLocation()
    }
}

// ========== UI 状态数据类 ==========
data class NetTestUiState(
    val isTesting: Boolean = false,
    val isAutoTesting: Boolean = false,
    val autoJob: Job? = null,
    val testResults: List<NetTestResult> = emptyList(),
    val currentResult: NetTestResult? = null,
    val csvPath: String? = null,
    val htmlPath: String? = null,
    val hasLocationPermission: Boolean = false
)

// ========== 一次性事件密封类 ==========
sealed class NetTestEvent {
    object ShowPermissionDeniedDialog : NetTestEvent()
    data class LocationFailed(val message: String) : NetTestEvent()
    data class TestCompleted(val result: NetTestResult) : NetTestEvent()
    data class TestFailed(val error: String) : NetTestEvent()
    data class CsvSaved(val path: String) : NetTestEvent()
    data class HtmlSaved(val path: String) : NetTestEvent()
}
```

---

### 步骤 3：完全重写 NetTestScreen.kt

**目标**：无状态 Composable，只负责渲染和转发用户操作

替换 `app/src/main/java/com/example/netgeocourier/screen/NetTestScreen.kt` 全部内容：

```kotlin
package com.example.netgeocourier.screen

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.netgeocourier.R
import com.example.netgeocourier.data.NetTestResult
import com.example.netgeocourier.viewmodel.NetTestViewModel
import com.example.netgeocourier.viewmodel.NetTestEvent
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetTestScreen(
    viewModel: NetTestViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // ========== 收集状态 ==========
    val uiState by viewModel.uiState.collectAsState()
    val event by viewModel.event.collectAsState()

    // ========== 副作用处理（事件响应） ==========
    LaunchedEffect(event) {
        when (event) {
            is NetTestEvent.ShowPermissionDeniedDialog -> {
                // 对话框通过 LaunchedEffect 自动显示
            }
            is NetTestEvent.LocationFailed -> {
                Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                viewModel.consumeEvent(event)
            }
            is NetTestEvent.TestCompleted -> {
                // 成功不需要 Toast，UI 已更新
                viewModel.consumeEvent(event)
            }
            is NetTestEvent.TestFailed -> {
                Toast.makeText(context, "测试失败: ${event.error}", Toast.LENGTH_SHORT).show()
                viewModel.consumeEvent(event)
            }
            is NetTestEvent.CsvSaved -> {
                Toast.makeText(context, "CSV 已保存", Toast.LENGTH_SHORT).show()
                viewModel.consumeEvent(event)
            }
            is NetTestEvent.HtmlSaved -> {
                Toast.makeText(context, "地图已生成", Toast.LENGTH_SHORT).show()
                viewModel.consumeEvent(event)
            }
            null -> {} // 无事件
        }
    }

    // ========== 权限拒绝对话框 ==========
    if (event is NetTestEvent.ShowPermissionDeniedDialog) {
        PermissionDeniedDialog(
            onDismiss = { viewModel.onDismissPermissionDialog() },
            onOpenSettings = { viewModel.openAppSettings() }
        )
    }

    // ========== UI 渲染 ==========
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.app_name),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // 操作按钮卡片
            ControlCard(
                onTestClick = { viewModel.doTest() },
                onStopClick = {
                    viewModel.stopAutoTest()
                },
                onSaveCsvClick = { viewModel.saveCsv() },
                onAutoTestClick = {
                    if (uiState.isAutoTesting) viewModel.stopAutoTest()
                    else viewModel.startAutoTest()
                },
                onSaveHtmlClick = { viewModel.saveAmapHtml() },
                onSendEmailClick = { viewModel.sendEmail() },
                uiState = uiState
            )

            Spacer(Modifier.height(20.dp))

            // 最新结果卡片
            LatestResultCard(result = uiState.currentResult)

            Spacer(Modifier.height(20.dp))

            // 折线图卡片
            if (uiState.testResults.size >= 2) {
                SpeedChartCard(results = uiState.testResults)
                Spacer(Modifier.height(20.dp))
            }

            // 历史记录卡片
            HistoryCard(results = uiState.testResults)
        }
    }
}

// ========== 子组件：控制按钮卡片 ==========
@Composable
private fun ControlCard(
    onTestClick: () -> Unit,
    onStopClick: () -> Unit,
    onSaveCsvClick: () -> Unit,
    onAutoTestClick: () -> Unit,
    onSaveHtmlClick: () -> Unit,
    onSendEmailClick: () -> Unit,
    uiState: NetTestViewModel.NetTestUiState
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 第一行：开始 / 停止
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onTestClick,
                    enabled = !uiState.isTesting && !uiState.isAutoTesting && uiState.hasLocationPermission,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("开始测试", fontSize = 14.sp)
                }

                Button(
                    onClick = onStopClick,
                    enabled = uiState.isTesting || uiState.isAutoTesting,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("停止测试", fontSize = 14.sp)
                }
            }

            Spacer(Modifier.height(12.dp))

            // 第二行：保存 CSV / 自动测试
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onSaveCsvClick,
                    enabled = uiState.testResults.isNotEmpty() && !uiState.isTesting && !uiState.isAutoTesting,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("保存CSV", fontSize = 14.sp)
                }

                Button(
                    onClick = onAutoTestClick,
                    enabled = !uiState.isTesting && uiState.hasLocationPermission,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = if (uiState.isAutoTesting) {
                        ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    } else {
                        ButtonDefaults.buttonColors()
                    }
                ) {
                    Text(
                        if (uiState.isAutoTesting) "停止自动" else "自动测试",
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // 第三行：生成地图 / 发送邮件
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onSaveHtmlClick,
                    enabled = uiState.testResults.isNotEmpty() && !uiState.isTesting && !uiState.isAutoTesting,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("生成地图", fontSize = 14.sp)
                }

                Button(
                    onClick = onSendEmailClick,
                    enabled = uiState.testResults.isNotEmpty() && !uiState.isTesting && !uiState.isAutoTesting,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("发送邮件", fontSize = 14.sp)
                }
            }
        }
    }
}

// ========== 子组件：最新结果卡片 ==========
@Composable
private fun LatestResultCard(result: NetTestResult?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "最新结果",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))

            if (result != null) {
                ResultDetailCard(result = result)
            } else {
                EmptyStateCard(message = "暂无测试数据")
            }
        }
    }
}

// ========== 子组件：历史记录卡片 ==========
@Composable
private fun HistoryCard(results: List<NetTestResult>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "历史记录",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "共 ${results.size} 次",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(8.dp))

            if (results.isEmpty()) {
                EmptyStateCard(message = "暂无历史数据")
            } else {
                results.reversed().forEachIndexed { index, result ->
                    val isFirst = index == 0
                    ResultDetailItem(result = result, isFirst = isFirst)
                }
            }
        }
    }
}

// ========== 子组件：折线图卡片 ==========
@Composable
private fun SpeedChartCard(results: List<NetTestResult>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "速率趋势图",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
            ) {
                SimpleSpeedChart(testResults = results)
            }

            Spacer(Modifier.height(8.dp))

            Text(
                "提示：点击图表上的圆点查看详细信息",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}

// ========== 通用空状态卡片 ==========
@Composable
private fun EmptyStateCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Text(
            message,
            modifier = Modifier.padding(32.dp),
            textAlign = TextAlign.Center,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ========== 权限拒绝对话框 ==========
@Composable
private fun PermissionDeniedDialog(
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "定位权限被拒绝",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
        },
        text = {
            Column {
                Text("应用需要定位权限才能获取您的位置信息。")
                Spacer(Modifier.height(8.dp))
                Text("请在设置中允许应用访问位置信息。")
            }
        },
        confirmButton = {
            TextButton(onClick = onOpenSettings) {
                Text("去设置")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

// ========== 结果详情卡片（复用原有逻辑） ==========
@Composable
fun ResultDetailCard(result: NetTestResult) {
    val (gcjLat, gcjLon) = remember(result) {
        result.latitude to result.longitude
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                result.timestamp,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SpeedItem("下行", result.download.toFloat(), Color(0xFF4285F4))
                SpeedItem("上行", result.upload.toFloat(), Color(0xFFEA4335))
                SpeedItem("Ping", result.ping.toFloat(), Color(0xFF34A853), "ms")
            }

            Text(
                "坐标: ${"%.4f".format(gcjLat)}, ${"%.4f".format(gcjLon)}",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun ResultDetailItem(result: NetTestResult, isFirst: Boolean) {
    val (gcjLat, gcjLon) = remember(result) {
        result.latitude to result.longitude
    }

    Column {
        if (!isFirst) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    result.timestamp,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "📡 ${String.format("%.2f", result.download)} Mbps ↓  ${String.format("%.2f", result.upload)} Mbps ↑",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "📍 ${"%.4f".format(gcjLat)}, ${"%.4f".format(gcjLon)}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }

            Text(
                "🏓 ${result.ping}ms",
                fontSize = 11.sp,
                color = Color(0xFF34A853)
            )
        }
    }
}

@Composable
fun SpeedItem(label: String, value: Float, color: Color, unit: String = "Mbps") {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 11.sp, color = color)
        Text(
            "${String.format("%.2f", value)}",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(unit, fontSize = 9.sp, color = color.copy(alpha = 0.7f))
    }
}

// ========== 折线图（保持不变） ==========
@Composable
fun SimpleSpeedChart(testResults: List<NetTestResult>) {
    if (testResults.isEmpty()) return

    var maxSpeed = 10f
    for (result in testResults) {
        val download = result.download.toFloat()
        val upload = result.upload.toFloat()
        if (download > maxSpeed) maxSpeed = download
        if (upload > maxSpeed) maxSpeed = upload
    }
    maxSpeed = if (maxSpeed <= 10) 10f else ((maxSpeed + 9) / 10).toInt() * 10f

    var showDialog by remember { mutableStateOf(false) }
    var selectedResult by remember { mutableStateOf<NetTestResult?>(null) }
    var selectedIndex by remember { mutableStateOf(-1) }

    Box(modifier = Modifier.fillMaxSize()) {
        SpeedChartCanvas(
            testResults = testResults,
            maxSpeed = maxSpeed,
            onPointClick = { index, result ->
                selectedIndex = index
                selectedResult = result
                showDialog = true
            }
        )
    }

    if (showDialog && selectedResult != null) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = {
                Text(
                    "第 ${selectedIndex + 1} 次测试",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text("时间：${selectedResult!!.timestamp}", fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        SpeedItem("下行", selectedResult!!.download.toFloat(), Color(0xFF4285F4))
                        SpeedItem("上行", selectedResult!!.upload.toFloat(), Color(0xFFEA4335))
                        SpeedItem("Ping", selectedResult!!.ping.toFloat(), Color(0xFF34A853), "ms")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("关闭")
                }
            }
        )
    }
}

@Composable
fun SpeedChartCanvas(
    testResults: List<NetTestResult>,
    maxSpeed: Float,
    onPointClick: ((Int, NetTestResult) -> Unit)? = null
) {
    val downloadPointPositions = remember { mutableStateListOf<Pair<Int, Offset>>() }
    val uploadPointPositions = remember { mutableStateListOf<Pair<Int, Offset>>() }

    Canvas(modifier = Modifier
        .fillMaxSize()
        .pointerInput(Unit) {
            detectTapGestures { offset ->
                var tappedIndex = -1

                for ((index, pos) in downloadPointPositions) {
                    val dx = offset.x - pos.x
                    val dy = offset.y - pos.y
                    val distance = kotlin.math.sqrt(dx * dx + dy * dy)
                    if (distance < 30f) {
                        tappedIndex = index
                        break
                    }
                }

                if (tappedIndex == -1) {
                    for ((index, pos) in uploadPointPositions) {
                        val dx = offset.x - pos.x
                        val dy = offset.y - pos.y
                        val distance = kotlin.math.sqrt(dx * dx + dy * dy)
                        if (distance < 30f) {
                            tappedIndex = index
                            break
                        }
                    }
                }

                if (tappedIndex != -1 && tappedIndex < testResults.size) {
                    onPointClick?.invoke(tappedIndex, testResults[tappedIndex])
                }
            }
        }
    ) {
        downloadPointPositions.clear()
        uploadPointPositions.clear()

        val width = size.width
        val height = size.height
        val paddingLeft = 45f
        val paddingRight = 15f
        val paddingTop = 20f
        val paddingBottom = 35f
        val chartWidth = width - paddingLeft - paddingRight
        val chartHeight = height - paddingTop - paddingBottom

        if (testResults.size < 2) return@Canvas

        val xStep = chartWidth / (testResults.size - 1)
        val ySteps = 5

        for (i in 0..ySteps) {
            val y = paddingTop + chartHeight * (1 - i.toFloat() / ySteps)
            drawLine(
                color = Color.LightGray.copy(alpha = 0.3f),
                start = Offset(paddingLeft, y),
                end = Offset(width - paddingRight, y),
                strokeWidth = 1f
            )
        }

        val textPaint = Paint().apply {
            color = android.graphics.Color.GRAY
            textSize = 26f
            textAlign = Paint.Align.RIGHT
        }

        for (i in 0..ySteps) {
            val value = (maxSpeed * i / ySteps).toInt()
            val y = paddingTop + chartHeight * (1 - i.toFloat() / ySteps)
            drawContext.canvas.nativeCanvas.drawText(
                value.toString(),
                paddingLeft - 8f,
                y + 8f,
                textPaint
            )
        }

        val labelPaint = Paint().apply {
            color = android.graphics.Color.GRAY
            textSize = 22f
            textAlign = Paint.Align.CENTER
        }

        for (i in testResults.indices) {
            val x = paddingLeft + i * xStep
            val timeStr = testResults[i].timestamp
            val label = if (timeStr.length >= 16) timeStr.substring(11, 16) else timeStr
            drawContext.canvas.nativeCanvas.drawText(
                label,
                x,
                height - paddingBottom + 18f,
                labelPaint
            )
        }

        val downloadPoints = Array(testResults.size) { i ->
            val x = paddingLeft + i * xStep
            val y = paddingTop + chartHeight * (1 - testResults[i].download.toFloat() / maxSpeed)
            Offset(x, y)
        }

        val uploadPoints = Array(testResults.size) { i ->
            val x = paddingLeft + i * xStep
            val y = paddingTop + chartHeight * (1 - testResults[i].upload.toFloat() / maxSpeed)
            Offset(x, y)
        }

        for (i in downloadPoints.indices) {
            downloadPointPositions.add(Pair(i, downloadPoints[i]))
        }
        for (i in uploadPoints.indices) {
            uploadPointPositions.add(Pair(i, uploadPoints[i]))
        }

        val downloadPath = Path()
        for (i in downloadPoints.indices) {
            if (i == 0) downloadPath.moveTo(downloadPoints[i].x, downloadPoints[i].y)
            else downloadPath.lineTo(downloadPoints[i].x, downloadPoints[i].y)
        }
        drawPath(path = downloadPath, color = Color(0xFF4285F4), style = Stroke(width = 2.5f, cap = StrokeCap.Round))

        for (point in downloadPoints) {
            drawCircle(color = Color(0xFF4285F4), radius = 7f, center = point)
            drawCircle(color = Color.White, radius = 3f, center = point)
        }

        val uploadPath = Path()
        for (i in uploadPoints.indices) {
            if (i == 0) uploadPath.moveTo(uploadPoints[i].x, uploadPoints[i].y)
            else uploadPath.lineTo(uploadPoints[i].x, uploadPoints[i].y)
        }
        drawPath(path = uploadPath, color = Color(0xFFEA4335), style = Stroke(width = 2.5f, cap = StrokeCap.Round))

        for (point in uploadPoints) {
            drawCircle(color = Color(0xFFEA4335), radius = 7f, center = point)
            drawCircle(color = Color.White, radius = 3f, center = point)
        }

        drawLine(color = Color.Black, start = Offset(paddingLeft, paddingTop), end = Offset(paddingLeft, height - paddingBottom), strokeWidth = 1.5f)
        drawLine(color = Color.Black, start = Offset(paddingLeft, height - paddingBottom), end = Offset(width - paddingRight, height - paddingBottom), strokeWidth = 1.5f)
    }
}
```

---

### 步骤 4：修复 SpeedTestHelper.kt 的方法名

修改 `app/src/main/java/com/example/netgeocourier/helper/SpeedTestHelper.kt` 第72行：

```kotlin
// 将方法名从 measurePingSafe 改为 measurePing
suspend fun measurePing(): Int = withContext(Dispatchers.IO) {
    try {
        val address = InetAddress.getByName(PING_HOST)
        val startTime = System.currentTimeMillis()
        // 超时 3 秒
        val isReachable = address.isReachable(3000)
        val duration = System.currentTimeMillis() - startTime
        if (isReachable) duration.toInt() else -1
    } catch (e: Exception) {
        -1
    }
}
```

---

### 步骤 5：优化 LocationHelper.kt（可选增强）

在 `app/src/main/java/com/example/netgeocourier/helper/LocationHelper.kt` 中添加 GPS 开关检查：

```kotlin
import android.content.Context
import android.location.LocationManager

class LocationHelper(private val context: Context) : AMapLocationListener {

    private fun isLocationEnabled(): Boolean {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
               lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    suspend fun getCurrentLocation() = suspendCancellableCoroutine { cont ->
        // 1. 检查权限
        if (!PermissionHelper.hasLocationPermission(context)) {
            cont.resume(null)
            return@suspendCancellableCoroutine
        }

        // 2. 检查定位服务是否开启
        if (!isLocationEnabled()) {
            android.util.Log.e("LocationHelper", "定位服务未开启")
            cont.resume(null)
            return@suspendCancellableCoroutine
        }

        // ... 其余原有逻辑保持不变 ...
        locationContinuation = cont

        try {
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

    // ... 其余代码保持不变 ...
}
```

---

## 📁 最终文件结构

```
NetGeoCourier2/
├── LOCATION_FIX.md                      # 📄 本文档
├── local.properties                     # 包含 AMAP 密钥（已存在）
├── app/
│   ├── build.gradle.kts                 # 已配置依赖
│   └── src/main/
│       ├── AndroidManifest.xml          # 已声明权限和 API Key
│       └── java/com/example/netgeocourier/
│           ├── MainActivity.kt          # ✅ 已修改
│           ├── helper/
│           │   ├── LocationHelper.kt    # ⚠️ 可选优化（建议添加GPS检查）
│           │   ├── PermissionHelper.kt  # ✅ 无需修改
│           │   ├── SpeedTestHelper.kt   # ✅ 需重命名方法
│           │   └── FileHelper.kt        # ✅ 无需修改
│           ├── screen/
│           │   └── NetTestScreen.kt     # ✅ 需完全重写
│           ├── viewmodel/
│           │   └── NetTestViewModel.kt  # ✅ 需完全重写
│           └── data/
│               └── NetTestResult.kt     # ✅ 无需修改
```

---

## 🔄 数据流向图（MVVM 架构）

```
用户点击"开始测试"
    ↓
NetTestScreen { viewModel.doTest() }
    ↓
NetTestViewModel.doTest()
    ├── 校验权限（uiState.hasLocationPermission）
    ├── 更新状态（isTesting = true）
    └── 协程执行
        ↓
    LocationHelper.getCurrentLocation()
        ↓
    定位成功 → 并发测试
        ├── SpeedTestHelper.measureDownloadSpeed()
        ├── SpeedTestHelper.measureUploadSpeed()
        └── SpeedTestHelper.measurePing()
        ↓
    创建 NetTestResult
        ↓
    _uiState.update {
        copy(
            isTesting = false,
            currentResult = result,
            testResults = testResults + result
        )
    }
        ↓
    NetTestScreen 收集 StateFlow → 自动重组 UI
        ↓
    显示最新结果、更新图表
```

---

## 🎯 关键改进对比

| 问题 | 旧方案 | 新方案（MVVM） |
|------|--------|---------------|
| 权限管理 | 散落在 Activity，回调为空 | 集中在 ViewModel，状态驱动 |
| 状态存储 | Composable 内部 + ViewModel（两套，不同步） | ViewModel 单一数据源（StateFlow） |
| 定位失败提示 | Toast 硬编码 | Event 驱动，可扩展 |
| 测试逻辑 | 在 Composable 中 | 在 ViewModel 中（可测试） |
| UI 更新 | 手动修改变量 | 自动收集 StateFlow |
| 生命周期 | 容易泄漏 | viewModelScope 自动管理 |
| 代码复用 | 难以复用 | 逻辑与 UI 分离，易复用 |

---

## 🚀 迁移检查清单

- [ ] **MainActivity.kt** - 传递 ViewModel，修复权限回调
- [ ] **NetTestViewModel.kt** - 完全重写（状态 + 事件）
- [ ] **NetTestScreen.kt** - 完全重写（无状态 Composable）
- [ ] **SpeedTestHelper.kt** - 重命名 `measurePingSafe()` → `measurePing()`
- [ ] [可选] **LocationHelper.kt** - 添加 GPS 开关检查
- [ ] **测试** - 运行应用，验证权限流程
- [ ] **测试** - 点击"开始测试"，确认定位成功

---

## ⚠️ 注意事项与最佳实践

### 1. 事件消费机制
```kotlin
// 在 LaunchedEffect 中处理事件后，必须清除
viewModel.consumeEvent(event)  // 防止重复处理
```

### 2. 权限拒绝后处理
用户如果选择"不再询问"，需要引导用户到系统设置页面：
- 对话框提供"去设置"按钮
- 返回应用后，在 `MainActivity.onResume()` 中重新检查权限（需在 ViewModel 添加 `checkPermission()` 方法）

### 3. 自动测试管理
- `stopAutoTest()` 设置标志位并取消 Job
- `doTest()` 开始前检查 `isAutoTesting` 状态

### 4. 内存泄漏预防
- ✅ `LocationHelper` 在 ViewModel `onCleared` 中释放
- ✅ 使用 `viewModelScope` 自动取消协程
- ✅ 事件使用 `StateFlow` 而非 `SharedFlow`，避免重复消费

---

## 📝 最终效果

✅ **权限流程**：申请 → 授予 → ViewModel 更新状态 → UI 按钮可用  
✅ **定位流程**：点击测试 → 检查权限 → 调用 LocationHelper → 成功定位 → 执行测试 → 更新 UI  
✅ **状态同步**：所有状态变更通过 ViewModel → StateFlow → Compose 自动更新  
✅ **事件驱动**：Toast、对话框通过 Event 一次性触发  
✅ **架构清晰**：MVVM 分层明确，易于维护和测试

---

**🎉 完成！现在你的应用已完全遵循 MVVM 架构，状态管理清晰，权限流程正确，定位失败问题已彻底解决。**

---

## 📚 参考资源

- [Android MVVM 架构指南](https://developer.android.com/topic/architecture)
- [Jetpack Compose 状态管理](https://developer.android.com/jetpack/compose/state)
- [Kotlin Flow 官方文档](https://kotlinlang.org/docs/flow.html)
- [高德定位 SDK 文档](https://lbs.amap.com/api/android-location-sdk/guide/utilities/accuracy-check)

---

**文档版本**：v1.0  
**最后更新**：2026-04-10  
**适用版本**：NetGeoCourier2 v1.0
