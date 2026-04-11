
## 目录
1. [问题概述](#问题概述)
2. [修改目标](#修改目标)
3. [文件依赖关系](#文件依赖关系)
4. [详细修改步骤](#详细修改步骤)
5. [完整代码对比](#完整代码对比)
6. [验证方法](#验证方法)
7. [常见问题](#常见问题)

---

## 问题概述

### 当前架构问题

```
MainActivity
    ├── 创建 LocationHelper (实例 A)
    └── 传递给 NetTestScreen
            ├── 直接调用 LocationHelper.getCurrentLocation() ❌
            ├── 直接调用 SpeedTestHelper ❌
            ├── 直接调用 FileHelper ❌
            └── 维护独立状态 (isTesting, testResults...) ❌
                    ↓
                NetTestViewModel (独立实例)
                    ├── 创建 LocationHelper (实例 B) ← 重复！
                    ├── 封装 doTest/startAutoTest/stopAutoTest
                    ├── 维护 StateFlow 状态
                    └── 封装 FileHelper 调用
```

### 三大冲突

#### 冲突 1：业务逻辑重复
- **NetTestScreen** 实现了 `doTest/startAutoTest/stopAutoTest`
- **NetTestViewModel** 也实现了相同的逻辑
- 两套逻辑无法同步，导致状态混乱

#### 冲突 2：状态管理不一致
- Screen 使用 `mutableStateOf` (Compose 状态，旋转丢失)
- ViewModel 使用 `StateFlow` (ViewModel 状态，旋转保留)
- 两者独立，数据不同步

#### 冲突 3：LocationHelper 重复创建 + 职责越权
- MainActivity 创建 LocationHelper 实例 A
- ViewModel 内部创建 LocationHelper 实例 B
- Screen 直接调用 Helper，违反 MVVM (View 不应直接调用 Model/Helper)

---

## 修改目标

### 目标架构

```
MainActivity
    ├── 不创建 LocationHelper ✅
    └── 不传递任何 Helper 给 Screen ✅
            ↓
        NetTestScreen (纯 UI)
            ├── 接收 NetTestViewModel 参数
            ├── 从 ViewModel collectAsState 获取数据
            └── 按钮点击调用 viewModel.doTests() 等方法
                    ↓
                NetTestViewModel (唯一业务逻辑)
                    ├── 创建 LocationHelper (唯一实例)
                    ├── 实现 doTest/startAutoTest/stopAutoTest
                    ├── 暴露 StateFlow 状态
                    └── 封装 FileHelper 调用
```

### 修改原则
1. **Screen 零业务逻辑** - 只负责 UI 渲染和事件转发
2. **ViewModel 全权负责** - 所有数据、状态、业务逻辑
3. **Helper 只被 ViewModel 调用** - 不直接暴露给 Screen
4. **单一数据源** - 状态只有一份（ViewModel 的 StateFlow）

---

## 文件依赖关系

```
MainActivity.kt
    ↓ (依赖)
NetTestScreen.kt (修改：删除 locationHelper 参数)
    ↓ (依赖)
NetTestViewModel.kt (修改：无，已正确实现)
    ↓ (依赖)
LocationHelper.kt (修改：无，保持不变)
SpeedTestHelper.kt (修改：无，保持不变)
FileHelper.kt (修改：无，保持不变)
NetTestResult.kt (修改：无，保持不变)

app/build.gradle.kts (修改：添加 lifecycle-viewmodel-compose 依赖)
```

---

## 详细修改步骤

### 前置条件：添加 Compose ViewModel 依赖

**⚠️ 必须先做这一步，否则 `viewModel()` 函数会红色报错！**

**文件：** `app/build.gradle.kts`

**位置：** 在 `dependencies { ... }` 块中添加

```kotlin
dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    // ✅ 添加这一行（在 179 行后或 176 行后）
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.2")

    // ✅ 已存在的保存行（保持不动）
    implementation("androidx.compose.runtime:runtime-saveable")

    implementation(libs.amap.location)
    // ... 其他依赖保持不变
}
```

**依赖说明：**
- `lifecycle-viewmodel-compose` 提供 `viewModel()` 可组合函数
- 版本 `2.8.2` 与 AndroidX 其他组件兼容
- 如果项目使用版本目录（libs.versions.toml），也可以添加别名

**添加后同步 Gradle：**
```bash
./gradlew build
```

---

### 修改 1：MainActivity.kt - 移除 LocationHelper 创建和传递

#### 1.1 删除成员变量声明

**位置：** `MainActivity.kt` 第 102 行

```diff
-    private lateinit var locationHelper: LocationHelper
```

**修改后：**
```kotlin
class MainActivity : ComponentActivity() {

    lateinit var viewmodel: NetTestViewModel
    // 删除 private lateinit var locationHelper: LocationHelper
```

#### 1.2 删除 onCreate 中的初始化

**位置：** `MainActivity.kt` 第 108 行

```diff
-        locationHelper = LocationHelper(this)
```

**修改后：**
```kotlin
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewmodel = ViewModelProvider(this)[NetTestViewModel::class.java]
        // 删除 locationHelper = LocationHelper(this)
```

#### 1.3 修改 NetTestScreen 调用，不传递 locationHelper

**位置：** `MainActivity.kt` 第 118-122 行

```diff
                        AppPage.TEST -> NetTestScreen(
-                            locationHelper = locationHelper,
+                            // 不传递 locationHelper，Screen 内部从 ViewModel 获取
                            onOpenEvaluation = { viewmodel.currentPage = AppPage.EVALUATION }
                        )
```

**修改后：**
```kotlin
AppPage.TEST -> NetTestScreen(
    // locationHelper 已移除
    onOpenEvaluation = { viewmodel.currentPage = AppPage.EVALUATION }
)
```

**注意：** 此时会编译错误，因为 NetTestScreen 的参数列表还没改，下一步修复。

---

### 修改 2：NetTestScreen.kt - 完全重构，移除所有业务逻辑

#### 2.1 添加 ViewModel 相关导入

**位置：** `NetTestScreen.kt` 第 70 行后（已有 NetTestViewModel 导入）

**确保第 70 行存在：**
```kotlin
import com.example.netgeocourier.viewmodel.NetTestViewModel
```

**在第 70 行后添加（如果没有）：**
```kotlin
import androidx.lifecycle.viewmodel.compose.viewModel
```

**完整导入区（第 1-70 行）应包含：**
```kotlin
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
// ... 其他 Compose 导入
import com.example.netgeocourier.data.NetTestResult
import com.example.netgeocourier.helper.FileHelper
import com.example.netgeocourier.helper.SpeedTestHelper
import com.example.netgeocourier.viewmodel.NetTestViewModel  // ✅ 已有
import androidx.lifecycle.viewmodel.compose.viewModel  // ✅ 新增
```

#### 2.2 修改函数签名，移除 locationHelper 参数

**位置：** `NetTestScreen.kt` 第 72-76 行

```diff
 @Composable
 fun NetTestScreen(
-    locationHelper: LocationHelper,
+    viewModel: NetTestViewModel,
     onOpenEvaluation: () -> Unit
 ) {
```

**修改后：**
```kotlin
@Composable
fun NetTestScreen(
    viewModel: NetTestViewModel,
    onOpenEvaluation: () -> Unit
) {
```

#### 2.3 替换状态定义（第 77-85 行）

**原代码：**
```kotlin
val context = LocalContext.current
var isTesting by remember { mutableStateOf(false) }
var isAutoTesting by remember { mutableStateOf(false) }
var autoJob by remember { mutableStateOf<Job?>(null) }
var testResults by remember { mutableStateOf(listOf<NetTestResult>()) }
var currentResult by remember { mutableStateOf<NetTestResult?>(null) }
var csvPath by remember { mutableStateOf<String?>(null) }
var htmlPath by remember { mutableStateOf<String?>(null) }
val coroutineScope = rememberCoroutineScope()
val scrollState = rememberScrollState()
```

**修改为：**
```kotlin
val context = LocalContext.current
val viewModel: NetTestViewModel = viewModel()  // ✅ 获取 ViewModel
val isTesting by viewModel.isTesting.collectAsState()
val isAutoTesting by viewModel.isAutoTesting.collectAsState()
val testResults by viewModel.testResults.collectAsState()
val currentResult by viewModel.curResult.collectAsState()
val csvPath by viewModel.csvPath.collectAsState()
val htmlPath by viewModel.htmlPath.collectAsState()
val coroutineScope = rememberCoroutineScope()
val scrollState = rememberScrollState()
```

**关键变化：**
- 添加 `val viewModel: NetTestViewModel = viewModel()`
- 删除 `var autoJob` - ViewModel 内部管理，Screen 无需知道
- 所有 `var` 改为 `val` + `by delegate`
- 从 `viewModel` 的 `StateFlow` 收集状态

#### 2.4 删除 doTest 函数（第 87-118 行）

**完全删除整段代码：**
```kotlin
fun doTest(onFinish: (() -> Unit)? = null) {
    isTesting = true
    currentResult = null
    coroutineScope.launch {
        val location = locationHelper.getCurrentLocation()
        if (location == null) {
            Toast.makeText(context, "Failed to get location.", Toast.LENGTH_SHORT).show()
            isTesting = false
            onFinish?.invoke()
            return@launch
        }

        val download = SpeedTestHelper.measureDownloadSpeed()
        val upload = SpeedTestHelper.measureUploadSpeed()
        val ping = SpeedTestHelper.measurePing()

        val timeText = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val result = NetTestResult(
            timestamp = timeText,
            latitude = location.latitude,
            longitude = location.longitude,
            upload = upload,
            download = download,
            ping = ping
        )

        currentResult = result
        testResults = testResults + result
        isTesting = false
        onFinish?.invoke()
    }
}
```

#### 2.5 删除 startAutoTest 函数（第 120-135 行）

**完全删除整段代码：**
```kotlin
fun startAutoTest() {
    if (isAutoTesting) {
        return
    }
    isAutoTesting = true
    autoJob = coroutineScope.launch {
        while (isAutoTesting) {
            val deferred = CompletableDeferred<Unit>()
            doTest { deferred.complete(Unit) }
            deferred.await()
            if (isAutoTesting) {
                delay(5000)
            }
        }
    }
}
```

#### 2.6 删除 stopAutoTest 函数（第 137-142 行）

**完全删除整段代码：**
```kotlin
fun stopAutoTest() {
    isAutoTesting = false
    autoJob?.cancel()
    autoJob = null
    isTesting = false
}
```

#### 2.7 修改按钮点击事件（6 处）

**位置 1：第 188 行（Start Test 按钮）**
```kotlin
// 原代码：
Button(
    onClick = { doTest() },
    enabled = !isTesting && !isAutoTesting,
    ...
)

// 修改为：
Button(
    onClick = { viewModel.doTest() },
    enabled = !isTesting && !isAutoTesting,
    ...
)
```

**位置 2：第 197 行（Stop 按钮）**
```kotlin
// 原代码：
Button(
    onClick = { stopAutoTest() },
    enabled = isTesting || isAutoTesting,
    ...
)

// 修改为：
Button(
    onClick = { viewModel.stopAutoTest() },
    enabled = isTesting || isAutoTesting,
    ...
)
```

**位置 3：第 226-228 行（Auto Test 按钮）**
```kotlin
// 原代码：
Button(
    onClick = {
        if (isAutoTesting) stopAutoTest() else startAutoTest()
    },
    enabled = !isTesting,
    ...
)

// 修改为：
Button(
    onClick = {
        if (isAutoTesting) viewModel.stopAutoTest() else viewModel.startAutoTest()
    },
    enabled = !isTesting,
    ...
)
```

**位置 4：第 216 行（Save CSV 按钮）**
```kotlin
// 原代码：
Button(
    onClick = { csvPath = FileHelper.saveCsv(context, testResults) },
    enabled = testResults.isNotEmpty() && !isTesting && !isAutoTesting,
    ...
)

// 修改为：
Button(
    onClick = { viewModel.saveCsv(context) },
    enabled = testResults.isNotEmpty() && !isTesting && !isAutoTesting,
    ...
)
```

**位置 5：第 246 行（Generate Map 按钮）**
```kotlin
// 原代码：
Button(
    onClick = { htmlPath = FileHelper.saveAmapHtml(context, testResults) },
    enabled = testResults.isNotEmpty() && !isTesting && !isAutoTesting,
    ...
)

// 修改为：
Button(
    onClick = { viewModel.saveAmapHtml(context) },
    enabled = testResults.isNotEmpty() && !isTesting && !isAutoTesting,
    ...
)
```

**位置 6：第 255 行（Send Email 按钮）**
```kotlin
// 原代码：
Button(
    onClick = { FileHelper.sendEmail(context, csvPath, htmlPath) },
    enabled = testResults.isNotEmpty() && !isTesting && !isAutoTesting,
    ...
)

// 修改为：
Button(
    onClick = { viewModel.sendEmail(context) },
    enabled = testResults.isNotEmpty() && !isTesting && !isAutoTesting,
    ...
)
```

#### 2.8 删除不再使用的导入（可选清理）

删除以下导入（如果不再直接使用）：
```kotlin
import android.widget.Toast  // ❌ 已移除 Toast 调用
import java.text.SimpleDateFormat  // ❌
import java.util.Date  // ❌
import kotlinx.coroutines.CompletableDeferred  // ❌
import com.example.netgeocourier.helper.CoordTransform  // ❌ 如果未使用
```

**注意：** 即使保留这些导入也不会报错，删除是为了代码整洁。

---

### 修改 3：NetTestViewModel.kt - 确认无需修改

**NetTestViewModel.kt 已正确实现**，只需确认依赖完整。

#### 3.1 检查 imports（第 1-27 行）

确保包含：
```kotlin
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
```

这些已经存在，无需修改。

#### 3.2 确认函数签名

ViewModel 已暴露的方法（无需修改）：
- `fun doTest(onFinish: (() -> Unit)? = null)` ✅
- `fun startAutoTest()` ✅
- `fun stopAutoTest()` ✅
- `fun saveCsv(context: android.content.Context)` ✅
- `fun saveAmapHtml(context: android.content.Context)` ✅
- `fun sendEmail(context: android.content.Context)` ✅
- `override fun onCleared()` ✅

#### 3.3 确认状态暴露

ViewModel 已正确暴露 StateFlow：
```kotlin
val isTesting: StateFlow<Boolean> = _isTesting.asStateFlow()
val isAutoTesting: StateFlow<Boolean> = _isAutoTesting.asStateFlow()
val testResults: StateFlow<List<NetTestResult>> = _testResults.asStateFlow()
val curResult: StateFlow<NetTestResult?> = _curResult.asStateFlow()
val csvPath: StateFlow<String?> = _csvPath.asStateFlow()
val htmlPath: StateFlow<String?> = _htmlPath.asStateFlow()
```

**无需修改。**

---

### 修改 4：NetTestScreen.kt 添加正确导入（重要！）

如果 `viewModel()` 函数仍报红色，确保第 70 行后有：

```kotlin
import androidx.lifecycle.viewmodel.compose.viewModel
```

如果 `collectAsState()` 报红色，确保第 37 行后有：
```kotlin
import androidx.compose.runtime.collectAsState
```

**完整导入列表（顶部）：**
```kotlin
package com.example.netgeocourier.screen

import android.graphics.Paint
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState  // ✅ 新增（如果需要）
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.netgeocourier.data.NetTestResult
import com.example.netgeocourier.helper.FileHelper
import com.example.netgeocourier.helper.SpeedTestHelper
import com.example.netgeocourier.viewmodel.NetTestViewModel
import androidx.lifecycle.viewmodel.compose.viewModel  // ✅ 新增（关键！）
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.sqrt
```

---

## 完整代码对比

### MainActivity.kt 修改前后

#### 修改前
```kotlin
class MainActivity : ComponentActivity() {

    lateinit var viewmodel: NetTestViewModel
    private lateinit var locationHelper: LocationHelper  // ❌

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewmodel = ViewModelProvider(this)[NetTestViewModel::class.java]
        locationHelper = LocationHelper(this)  // ❌

        setContent {
            NetGeoCourierTheme {
                Surface(...) {
                    when (viewmodel.currentPage) {
                        AppPage.TEST -> NetTestScreen(
                            locationHelper = locationHelper,  // ❌
                            onOpenEvaluation = { viewmodel.currentPage = AppPage.EVALUATION }
                        )
                        AppPage.EVALUATION -> EvaluationScreen(
                            onBack = { viewmodel.currentPage = AppPage.TEST }
                        )
                    }
                }
            }
        }
    }
}
```

#### 修改后
```kotlin
class MainActivity : ComponentActivity() {

    lateinit var viewmodel: NetTestViewModel
    // ✅ 删除 locationHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewmodel = ViewModelProvider(this)[NetTestViewModel::class.java]
        // ✅ 删除 locationHelper 初始化

        setContent {
            NetGeoCourierTheme {
                Surface(...) {
                    when (viewmodel.currentPage) {
                        AppPage.TEST -> NetTestScreen(
                            // ✅ 不传递 locationHelper
                            onOpenEvaluation = { viewmodel.currentPage = AppPage.EVALUATION }
                        )
                        AppPage.EVALUATION -> EvaluationScreen(
                            onBack = { viewmodel.currentPage = AppPage.TEST }
                        )
                    }
                }
            }
        }
    }
}
```

---

### NetTestScreen.kt 修改前后

#### 修改前（关键部分）
```kotlin
@Composable
fun NetTestScreen(
    locationHelper: LocationHelper,  // ❌ 直接依赖 Helper
    onOpenEvaluation: () -> Unit
) {
    val context = LocalContext.current
    var isTesting by remember { mutableStateOf(false) }  // ❌ 独立状态
    var isAutoTesting by remember { mutableStateOf(false) }
    var autoJob by remember { mutableStateOf<Job?>(null) }
    var testResults by remember { mutableStateOf(listOf<NetTestResult>()) }
    var currentResult by remember { mutableStateOf<NetTestResult?>(null) }
    var csvPath by remember { mutableStateOf<String?>(null) }
    var htmlPath by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    fun doTest(onFinish: (() -> Unit)? = null) {  // ❌ 业务逻辑
        isTesting = true
        currentResult = null
        coroutineScope.launch {
            val location = locationHelper.getCurrentLocation()  // ❌ 直接调用
            if (location == null) {
                Toast.makeText(context, "Failed to get location.", Toast.LENGTH_SHORT).show()
                isTesting = false
                onFinish?.invoke()
                return@launch
            }
            val download = SpeedTestHelper.measureDownloadSpeed()  // ❌
            // ...
        }
    }

    fun startAutoTest() { /* ... */ }  // ❌
    fun stopAutoTest() { /* ... */ }   // ❌

    Button(onClick = { doTest() }) { ... }  // ❌
}
```

#### 修改后（关键部分）
```kotlin
@Composable
fun NetTestScreen(
    viewModel: NetTestViewModel,  // ✅ 只依赖 ViewModel
    onOpenEvaluation: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: NetTestViewModel = viewModel()  // ✅ 获取 ViewModel 实例
    val isTesting by viewModel.isTesting.collectAsState()  // ✅ 从 ViewModel 收集
    val isAutoTesting by viewModel.isAutoTesting.collectAsState()
    val testResults by viewModel.testResults.collectAsState()
    val currentResult by viewModel.curResult.collectAsState()
    val csvPath by viewModel.csvPath.collectAsState()
    val htmlPath by viewModel.htmlPath.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // ✅ 删除所有业务逻辑函数（doTest/startAutoTest/stopAutoTest）

    Button(
        onClick = { viewModel.doTest() },  // ✅ 转发到 ViewModel
        enabled = !isTesting && !isAutoTesting
    ) { ... }
}
```

---

### NetTestViewModel.kt（无需修改）

```kotlin
class NetTestViewModel(application: Application) : AndroidViewModel(application) {
    private val locationHelper = LocationHelper(application)  // ✅ 唯一实例

    private val _isTesting = MutableStateFlow(false)
    val isTesting: StateFlow<Boolean> = _isTesting.asStateFlow()
    // ... 其他状态

    fun doTest(onFinish: (() -> Unit)? = null) {  // ✅ 业务逻辑在这里
        if (_isTesting.value || _isAutoTesting.value) return
        _isTesting.value = true
        _curResult.value = null
        coroutineScope.launch {
            val location = locationHelper.getCurrentLocation()  // ✅ 内部调用 Helper
            // ...
        }
    }

    fun startAutoTest() { /* ... */ }  // ✅
    fun stopAutoTest() { /* ... */ }   // ✅

    fun saveCsv(context: android.content.Context) {
        _csvPath.value = FileHelper.saveCsv(context, _testResults.value)
    }

    fun saveAmapHtml(context: android.content.Context) {
        _htmlPath.value = FileHelper.saveAmapHtml(context, _testResults.value)
    }

    fun sendEmail(context: android.content.Context) {
        FileHelper.sendEmail(context, _csvPath.value, _htmlPath.value)
    }
}
```

---

## 修改检查清单

- [ ] **app/build.gradle.kts**
  - [ ] 添加 `implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.2")`
  - [ ] 同步 Gradle

- [ ] **MainActivity.kt**
  - [ ] 删除 `locationHelper` 成员变量声明
  - [ ] 删除 `locationHelper = LocationHelper(this)` 初始化
  - [ ] 修改 NetTestScreen 调用，移除 `locationHelper = locationHelper` 参数

- [ ] **NetTestScreen.kt**
  - [ ] 添加 `import androidx.lifecycle.viewmodel.compose.viewModel`
  - [ ] 添加 `import androidx.compose.runtime.collectAsState`（如需要）
  - [ ] 修改函数签名，将 `locationHelper: LocationHelper` 改为 `viewModel: NetTestViewModel`
  - [ ] 删除 `var isTesting` 等 7 个状态定义，改为从 `viewModel` 收集
  - [ ] 删除 `doTest()` 函数（32 行）
  - [ ] 删除 `startAutoTest()` 函数（16 行）
  - [ ] 删除 `stopAutoTest()` 函数（6 行）
  - [ ] 修改 6 处按钮点击事件，改为调用 `viewModel.` 方法
  - [ ] 删除不再使用的 import（可选）

- [ ] **编译验证**
  - [ ] 构建项目，确保无编译错误
  - [ ] 运行应用，测试功能正常

---

## 验证方法

### 1. 编译检查
```bash
./gradlew build
```
确认无编译错误，特别是 `viewModel` 和 `collectAsState` 不再报红。

### 2. 功能测试清单

| 测试项 | 预期结果 | 检查点 |
|--------|---------|--------|
| 点击 **Start Test** | 执行单次测试，显示结果 | `viewModel.doTest()` 被调用 ✅ |
| 点击 **Auto Test** | 每 5 秒自动测试一次 | `viewModel.startAutoTest()` ✅ |
| 点击 **Stop** | 停止自动测试 | `viewModel.stopAutoTest()` ✅ |
| 旋转屏幕 | 测试状态、历史记录不丢失 | ViewModel 状态保留 ✅ |
| 点击 **Save CSV** | 生成 CSV 文件 | `viewModel.saveCsv()` ✅ |
| 点击 **Generate Map** | 生成 HTML 地图文件 | `viewModel.saveAmapHtml()` ✅ |
| 点击 **Send Email** | 发送邮件（含附件） | `viewModel.sendEmail()` ✅ |
| 点击 **Open Network Evaluation** | 跳转评估页面 | 正常跳转 ✅ |
| 返回测试页 | 数据仍然存在 | 状态保留 ✅ |

### 3. 架构验证

确认 NetTestScreen.kt 中：
- ✅ 没有 `fun doTest()` 定义
- ✅ 没有 `fun startAutoTest()` 定义
- ✅ 没有 `fun stopAutoTest()` 定义
- ✅ 没有 `var isTesting` 等状态定义
- ✅ 没有直接调用 `SpeedTestHelper.`、`FileHelper.`、`LocationHelper.`
- ✅ 只有 `val isTesting by viewModel.isTesting.collectAsState()`
- ✅ 函数签名是 `fun NetTestScreen(viewModel: NetTestViewModel, ...)`

确认 NetTestViewModel.kt 中：
- ✅ 有 `val locationHelper = LocationHelper(application)` 唯一实例
- ✅ 所有业务逻辑完整
- ✅ 所有状态通过 StateFlow 暴露

---

## 常见问题

### Q1：为什么显示 `viewModel` 红色？
**A：** 缺少依赖 `androidx.lifecycle:lifecycle-viewmodel-compose`。必须在 `app/build.gradle.kts` 的 `dependencies` 中添加：
```kotlin
implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.2")
```
添加后点击 Sync Now 或运行 `./gradlew build`。

### Q2：为什么 `collectAsState` 红色？
**A：** 缺少导入。在 NetTestScreen.kt 顶部添加：
```kotlin
import androidx.compose.runtime.collectAsState
```

### Q3：为什么 `NetTestViewModel` 类型红色？
**A：** 检查导入是否正确：
```kotlin
import com.example.netgeocourier.viewmodel.NetTestViewModel
```
如果包名不对，检查 NetTestViewModel.kt 第一行的 `package` 声明。

### Q4：添加依赖后仍报错？
**A：** 尝试：
1. File → Invalidate Caches / Restart（Android Studio）
2. 命令行运行 `./gradlew clean build`
3. 检查 `libs.versions.toml` 是否定义了版本别名

### Q5：修改后 Screen 还能访问 `locationHelper` 吗？
**A：** 不能，也不需要。所有定位操作通过 `viewModel.doTest()` 间接调用。

### Q6：ViewModel 的 `coroutineScope` 和 Screen 的 `coroutineScope` 区别？
**A：** 
- Screen 的 `coroutineScope` 绑定 Composable 生命周期
- ViewModel 的 `viewModelScope` 绑定 ViewModel 生命周期（更长）
- 修改后所有协程都在 ViewModel 的 `viewModelScope` 中启动，更安全

### Q7：LocationHelper 实例化在 ViewModel 中，Application context 安全吗？
**A：** 安全。`AndroidViewModel` 提供 `application` 是 Application Context，生命周期与应用一致，不会泄漏。

---

## 修改完成后的架构优势

| 维度 | 修改前 | 修改后 |
|------|--------|--------|
| **代码行数** | NetTestScreen ~760 行 | 减少 ~70 行 |
| **职责清晰度** | Screen 混合 UI+业务 | Screen 纯 UI，ViewModel 纯业务 |
| **状态同步** | 两套状态，不同步 | 单一数据源，自动同步 |
| **旋转屏幕** | 数据丢失 | 数据保留 ✅ |
| **测试难度** | 难（UI 耦合业务） | 易（可单独测试 ViewModel） |
| **Helper 实例** | 两个 LocationHelper 实例 | 一个实例 ✅ |
| **MVVM 符合度** | 30% | 100% ✅ |

---

## 总结

通过本次修改：
1. **消除冲突**：NetTestScreen 与 NetTestViewModel 不再有重复逻辑
2. **统一状态**：所有状态由 ViewModel 管理，Screen 只读
3. **正确依赖**：Screen → ViewModel → Helper，单向依赖
4. **符合 MVVM**：View 层不包含业务逻辑
5. **节省资源**：LocationHelper 只有一个实例
6. **修复编译错误**：添加 `lifecycle-viewmodel-compose` 依赖

**修改核心口诀：Screen 只负责「显示」和「转发点击」，其他全部交给 ViewModel。**

**关键步骤顺序：**
1. ✅ 先添加 Gradle 依赖（解决红色报错）
2. ✅ 修改 MainActivity（移除 LocationHelper 传递）
3. ✅ 修改 NetTestScreen（重构为纯 UI）
4. ✅ 验证编译和功能

---

## 快速修复命令

```bash
# 1. 添加依赖后构建
./gradlew build

# 2. 清理并重新构建（如果仍有错误）
./gradlew clean build

# 3. 安装到设备测试
./gradlew installDebug
```

**完成！**
