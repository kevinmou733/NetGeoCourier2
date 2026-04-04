# NetGeoCourier2 完整项目手册与大赛冲刺指南

## 📋 目录
- [项目简介](#项目简介)
- [开发环境搭建](#开发环境搭建)
- [项目结构详解](#项目结构详解)
- [核心代码逐行解析](#核心代码逐行解析)
- [配置文件说明](#配置文件说明)
- [常见修改场景](#常见修改场景)
- [调试与发布](#调试与发布)
- [函数速查表](#函数速查表)
- [大赛拓展方向](#大赛拓展方向)
- [一周冲刺计划](#一周冲刺计划)

---

## 项目简介

**NetGeoCourier2** 是一个 Android 网络测速应用，可以：
- 测量网络的上行/下行速度和 Ping 延迟
- 获取当前 GPS 坐标并转换为中国标准坐标系
- 将数据导出为 CSV 表格和高德地图 HTML 页面
- 通过邮件分享测速数据

**适用人群**：完全不懂安卓开发的新手也能在阅读本手册后修改项目

**技术栈**：
- 语言：Kotlin（现代 Java，语法简洁）
- UI 框架：Jetpack Compose（声明式 UI）
- 最低支持：Android 8.0（API 26）
- 目标版本：Android 15（API 35）

---

## 开发环境搭建

### 1. 安装 Android Studio
- 下载地址：https://developer.android.com/studio
- 安装时勾选 "Android SDK" 和 "SDK Platform"

### 2. 配置 SDK
打开 Android Studio → Tools → SDK Manager：
- 安装 "Android 13.0 (API 33)" 或更高版本
- 安装 "Google Play Services"（在 SDK Tools 标签页）

### 3. 配置高德地图密钥
在项目根目录的 `local.properties` 文件中添加：
```properties
AMAP_WEB_KEY=你的高德地图Web API密钥
```
获取密钥：注册高德开放平台 https://lbs.amap.com/dev/key/app → 创建 Web 服务应用 → 复制 KEY

### 4. 打开项目
- File → Open → 选择 `D:\NetGeoCourier2`
- 等待 Gradle 同步完成（首次需下载依赖）

### 5. 运行应用
点击顶部绿色 ▶️ 按钮 → 选择连接的安卓设备或模拟器

---

## 项目结构详解

```
NetGeoCourier2/
├── app/
│   ├── src/main/
│   │   ├── java/com/example/netgeocourier/
│   │   │   ├── MainActivity.kt              # 应用入口
│   │   │   ├── data/
│   │   │   │   └── NetTestResult.kt         # 数据模型
│   │   │   ├── helper/
│   │   │   │   ├── LocationHelper.kt        # 定位功能
│   │   │   │   ├── FileHelper.kt            # 文件操作
│   │   │   │   ├── CoordTransform.kt        # 坐标转换
│   │   │   │   └── SpeedTestHelper.kt       # 测速功能（不展开）
│   │   │   ├── screen/
│   │   │   │   └── NetTestScreen.kt         # 主界面
│   │   │   └── ui/theme/                    # 主题配置
│   │   │       ├── Color.kt                 # 颜色定义
│   │   │       ├── Theme.kt                 # 主题设置
│   │   │       └── Type.kt                  # 字体样式
│   │   ├── res/                             # 资源文件
│   │   │   ├── values/strings.xml          # 文字内容
│   │   │   ├── values/colors.xml           # 颜色定义
│   │   │   ├── values/themes.xml           # 主题样式
│   │   │   └── xml/file_paths.xml          # 文件路径配置
│   │   └── AndroidManifest.xml              # 应用清单
│   ├── build.gradle.kts                     # 应用构建配置
│   └── proguard-rules.pro                   # 代码混淆规则
├── build.gradle.kts                         # 根级构建配置
├── settings.gradle.kts                      # 项目设置
└── gradle.properties                        # Gradle属性
```

---

## 核心代码逐行解析

### 1. 数据模型：`data/NetTestResult.kt`

**完整代码**：
```kotlin
package com.example.netgeocourier.data

data class NetTestResult(
    val timestamp: String,   // 时间戳，如 "2025-06-02 14:30:25"
    val latitude: Double,    // WGS84纬度（GPS原始坐标）
    val longitude: Double,   // WGS84经度（GPS原始坐标）
    val upload: Double,      // 上行速度 (Mbps)
    val download: Double,    // 下行速度 (Mbps)
    val ping: Int            // Ping延迟 (ms)
)
```

**每个字段说明**：
- `timestamp`：测试时间，格式 `yyyy-MM-dd HH:mm:ss`
- `latitude` / `longitude`：GPS原始坐标（WGS84国际标准）
- `upload` / `download`：网速，单位 Mbps
- `ping`：网络延迟，单位毫秒

**修改方法**：
1. **添加新字段**（如信号强度）：
```kotlin
data class NetTestResult(
    timestamp: String,
    latitude: Double,
    longitude: Double,
    upload: Double,
    download: Double,
    ping: Int,
    val signalStrength: Int  // 新增字段
)
```
2. **同步修改** `FileHelper.kt` 第36-38行的 CSV 写入：
```kotlin
out.write("${item.timestamp},$gcjLon,${"%.2f".format(gcjLat)}," +
          "${"%.2f".format(item.upload)},${"%.2f".format(item.download)}," +
          "${item.ping},${item.signalStrength}\n")
```
3. **同步修改** `NetTestScreen.kt` 第50-57行的对象创建：
```kotlin
val result = NetTestResult(
    timestamp = timeStr,
    latitude = location.latitude,
    longitude = location.longitude,
    upload = upload,
    download = download,
    ping = ping,
    signalStrength = 0  // 暂时设为0或从测速工具获取
)
```
4. **同步修改** `FileHelper.kt` 第32行表头：
```kotlin
out.write("时间,经度(GCJ-02),纬度(GCJ-02),上行(Mbps),下行(Mbps),Ping(ms),信号强度\n")
```

---

### 2. 主界面：`screen/NetTestScreen.kt`

**文件位置**：`app/src/main/java/com/example/netgeocourier/screen/NetTestScreen.kt`

**总览**：使用 Jetpack Compose 编写的 UI，包含 6 个按钮和结果显示区域。

#### 2.1 `NetTestScreen` 主组合函数（第21行）

```kotlin
@Composable
fun NetTestScreen(locationHelper: LocationHelper) {
    val context = LocalContext.current  // 获取 Android 上下文
    var isTesting by remember { mutableStateOf(false) }          // 是否正在测试
    var isAutoTesting by remember { mutableStateOf(false) }     // 是否自动测试
    var autoJob by remember { mutableStateOf<Job?>(null) }      // 自动测试的协程任务
    var testResults by remember { mutableStateOf(listOf<NetTestResult>()) }  // 所有测试结果
    var curResult by remember { mutableStateOf<NetTestResult?>(null) }       // 最近一次结果
    val coroutineScope = rememberCoroutineScope()  // 协程作用域（异步任务）
    val scrollState = rememberScrollState()        // 滚动状态
    var csvPath by remember { mutableStateOf<String?>(null) }   // CSV文件路径
    var htmlPath by remember { mutableStateOf<String?>(null) }  // HTML地图路径
```

**状态变量说明**：
- `isTesting`：控制"开始测试"按钮禁用，防止重复点击
- `testResults`：保存所有测速结果，用于导出和历史显示
- `csvPath` / `htmlPath`：保存导出文件的绝对路径，用于邮件附件

#### 2.2 `doTest` 函数（第33行）— 执行单次测速

```kotlin
fun doTest(onFinish: (() -> Unit)? = null) {
    isTesting = true              // 标记测试开始
    curResult = null              // 清空当前结果显示
    coroutineScope.launch {       // 启动异步协程（不阻塞界面）
        // 1. 获取定位
        val location = locationHelper.getCurrentLocation(context)
        if (location == null) {
            Toast.makeText(context, "定位失败", Toast.LENGTH_SHORT).show()
            isTesting = false
            onFinish?.invoke()
            return@launch
        }

        // 2. 执行测速（SpeedTestHelper 实现）
        val download = SpeedTestHelper.measureDownloadSpeed()
        val upload = SpeedTestHelper.measureUploadSpeed()
        val ping = SpeedTestHelper.measurePing()

        // 3. 格式化时间
        val timeStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

        // 4. 创建数据对象
        val result = NetTestResult(
            timestamp = timeStr,
            latitude = location.latitude,
            longitude = location.longitude,
            upload = upload,
            download = download,
            ping = ping
        )

        // 5. 更新界面状态
        curResult = result
        testResults = testResults + result  // 追加到列表（不可变数据）
        isTesting = false
        onFinish?.invoke()
    }
}
```

**参数**：
- `onFinish`：测试完成后的回调函数，用于自动测试的间隔控制

**修改建议**：
- **调整时间格式**（第49行）：
```kotlin
val timeStr = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date())
```
- **添加网络类型**（需在 `NetTestResult` 添加字段）：
```kotlin
val connectivity = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
val networkType = connectivity.activeNetworkInfo?.typeName  // "WIFI", "MOBILE"
val result = NetTestResult(..., networkType = networkType)
```

#### 2.3 `startAutoTest` 函数（第66行）— 启动自动测试

```kotlin
fun startAutoTest() {
    if (isAutoTesting) return      // 防止重复启动
    isAutoTesting = true
    autoJob = coroutineScope.launch {
        while (isAutoTesting) {    // 循环直到停止
            val job = CompletableDeferred<Unit>()
            doTest { job.complete(Unit) }  // 执行一次测试
            job.await()          // 等待测试完成
            delay(5000)          // 等待 5 秒（可修改间隔）
        }
    }
}
```

**修改测试间隔**（第74行）：
```kotlin
delay(5000)  // 改为 delay(10000) 表示 10 秒间隔
```

#### 2.4 `stopAutoTest` 函数（第79行）— 停止自动测试

```kotlin
fun stopAutoTest() {
    isAutoTesting = false  // 退出循环
    autoJob?.cancel()      // 取消协程任务
    autoJob = null
}
```

#### 2.5 `ResultDetail` 组合函数（第181行）— 显示单条测速结果

```kotlin
@Composable
fun ResultDetail(result: NetTestResult) {
    val (gcjLat, gcjLon) = remember(result) {
        CoordTransform.wgs84ToGcj02(result.latitude, result.longitude)
    }

    Column(modifier = Modifier.padding(6.dp)) {
        Text("时间：${result.timestamp}", style = MaterialTheme.typography.bodySmall)
        Text("经纬度(GCJ-02)：${"%.6f".format(gcjLat)}, ${"%.6f".format(gcjLon)}", style = MaterialTheme.typography.bodySmall)
        Text("上行速率：${"%.2f".format(result.upload)} Mbps", style = MaterialTheme.typography.bodySmall)
        Text("下行速率：${"%.2f".format(result.download)} Mbps", style = MaterialTheme.typography.bodySmall)
        Text("Ping：${result.ping} ms", style = MaterialTheme.typography.bodySmall)
    }
}
```

**修改显示精度**（第188行）：
```kotlin
Text("经纬度：${"%.3f".format(gcjLat)}, ${"%.3f".format(gcjLon)}")  // 3位小数
```

---

### 3. 权限管理：`helper/LocationHelper.kt`

**文件位置**：`app/src/main/java/com/example/netgeocourier/helper/LocationHelper.kt`

#### 3.1 `PermissionHelper` 对象（第17行）

这是一个 **单例对象**（类似静态类），提供权限相关工具方法。

##### `getRequiredPermissions()`（第18行）— 获取所需权限列表

```kotlin
fun getRequiredPermissions(): Array<String> {
    val permissions = mutableListOf(
        Manifest.permission.ACCESS_FINE_LOCATION,   // 精确定位
        Manifest.permission.ACCESS_COARSE_LOCATION  // 粗略定位
    )
    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
        permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)  // 写入存储
        permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)   // 读取存储
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        permissions.add(Manifest.permission.POST_NOTIFICATIONS)  // 通知权限（Android 13+）
    }
    return permissions.toTypedArray()
}
```

**返回值**：字符串数组，包含应用运行所需的所有权限

**Android 版本判断说明**：
- `Build.VERSION.SDK_INT`：当前设备的 Android 版本号（整数）
- `Build.VERSION_CODES.S_V2`：Android 12（API 31）
- `Build.VERSION_CODES.TIRAMISU`：Android 13（API 33）

**修改权限逻辑**（例如仅申请精确定位）：
```kotlin
fun getRequiredPermissions(): Array<String> {
    return arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
}
```

##### `hasLocationPermission()`（第33行）— 检查定位权限

```kotlin
fun hasLocationPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED ||
    ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
}
```

**参数**：`context`：Android 上下文环境（MainActivity 传入 `this`）

**返回值**：
- `true`：至少有一个定位权限已授予
- `false`：无定位权限

##### `registerPermissionLauncher()`（第42行）— 申请权限

```kotlin
fun registerPermissionLauncher(activity: ComponentActivity, onGranted: () -> Unit) {
    activity.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.any { it }) {  // 任意一个权限被授予
            onGranted()
        }
    }.launch(getRequiredPermissions())  // 弹出权限申请对话框
}
```

**参数**：
- `activity`：当前 Activity（MainActivity 传入 `this`）
- `onGranted`：权限授予后的回调函数

**调用方式**（见 `MainActivity.kt` 第36-40行）：
```kotlin
private fun requestPermissions() {
    PermissionHelper.registerPermissionLauncher(this) {
        // 权限被授予后执行的代码
    }
}
```

#### 3.2 `LocationHelper` 类（第53行）— 定位服务

```kotlin
class LocationHelper(private val fusedLocationClient: FusedLocationProviderClient) {

    suspend fun getCurrentLocation(context: Context) = suspendCancellableCoroutine { cont ->
        if (!PermissionHelper.hasLocationPermission(context)) {
            cont.resume(null)  // 无权限，返回 null
            return@suspendCancellableCoroutine
        }

        val cancellationTokenSource = CancellationTokenSource()

        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,  // 高精度（使用 GPS）
            cancellationTokenSource.token
        ).addOnSuccessListener { location ->
            cont.resume(location)  // 成功，返回 Location 对象
        }.addOnFailureListener {
            cont.resume(null)  // 失败，返回 null
        }

        cont.invokeOnCancellation {
            cancellationTokenSource.cancel()  // 协程取消时停止定位请求
        }
    }
}
```

**构造函数**（在 `MainActivity` 第22-23行创建）：
```kotlin
val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
locationHelper = LocationHelper(fusedLocationClient)
```

**`getCurrentLocation()` 方法**：
- **参数**：`context`（MainActivity 传入 `this`）
- **返回值**：`android.location.Location?`（可空）
  - 成功：返回 Location 对象，包含 `latitude`、`longitude`、`accuracy` 等
  - 失败/无权限：返回 `null`
- **调用方式**：必须在 **协程** 中调用（`NetTestScreen.kt` 第37行使用 `coroutineScope.launch`）

**典型使用场景**：
```kotlin
val location = locationHelper.getCurrentLocation(context)
if (location != null) {
    val lat = location.latitude
    val lon = location.longitude
    // 使用坐标...
} else {
    Toast.makeText(context, "定位失败", Toast.LENGTH_SHORT).show()
}
```

---

### 4. 文件操作：`helper/FileHelper.kt`

**文件位置**：`app/src/main/java/com/example/netgeocourier/helper/FileHelper.kt`

这是一个 **工具对象**（单例），提供所有文件相关功能。

#### 4.1 `getDocumentsDir()`（第17行）— 获取文档存储目录

```kotlin
fun getDocumentsDir(context: Context): File? {
    val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
    if (dir != null && !dir.exists()) {
        dir.mkdirs()  // 创建目录（如果不存在）
    }
    return dir
}
```

**返回值**：`File?`（路径示例：`/storage/emulated/0/Android/data/com.example.netgeocourier/files/Documents/`）

**重要特点**：
- 使用 **应用私有外部存储**，不需要存储权限（Android 10+）
- 卸载应用时目录会被删除
- 用户可通过文件管理器访问该路径

**修改存储位置**（例如改为 DCIM 目录）：
```kotlin
fun getDocumentsDir(context: Context): File? {
    val dir = context.getExternalFilesDir(Environment.DIRECTORY_DCIM)
    if (dir != null && !dir.exists()) {
        dir.mkdirs()
    }
    return dir
}
```

#### 4.2 `saveCsv()`（第25行）— 保存 CSV 文件

```kotlin
fun saveCsv(context: Context, list: List<NetTestResult>): String? {
    val dir = getDocumentsDir(context) ?: return null  // 目录不可用则返回 null
    val file = File(dir, "nettest.csv")                // 固定文件名
    val isNewFile = !file.exists()                     // 判断是否首次创建

    FileOutputStream(file, true).bufferedWriter().use { out ->
        if (isNewFile) {
            // 写入表头（仅首次）
            out.write("时间,经度(GCJ-02),纬度(GCJ-02),上行(Mbps),下行(Mbps),Ping(ms)\n")
        }

        // 遍历所有测速结果
        list.forEach { item ->
            val (gcjLat, gcjLon) = CoordTransform.wgs84ToGcj02(item.latitude, item.longitude)
            out.write(
                "${item.timestamp},$gcjLon,${"%.2f".format(gcjLat)}," +
                "${"%.2f".format(item.upload)},${"%.2f".format(item.download)},${item.ping}\n"
            )
        }
    }

    Toast.makeText(context, "CSV已保存: ${file.name}", Toast.LENGTH_SHORT).show()
    return file.absolutePath  // 返回文件绝对路径
}
```

**CSV 格式**（第32行表头）：
```
时间,经度(GCJ-02),纬度(GCJ-02),上行(Mbps),下行(Mbps),Ping(ms)
2025-06-02 14:30:25,116.397428,39.90923,15.50,28.30,12
```

**修改表头**（第32行）**：
```kotlin
out.write("时间戳,纬度,经度,上传速度,下载速度,延迟\n")
```

**修改数据格式**（第37-38行）**：
```kotlin
out.write("${item.timestamp},$gcjLat,$gcjLon,${"%.1f".format(item.upload)},...")  // 1位小数
```

**添加新字段**（假设 `NetTestResult` 已添加 `networkType`）**：
```kotlin
out.write("${item.timestamp},$gcjLon,${"%.2f".format(gcjLat)},${item.upload},...,${item.networkType}\n")
```

**注意**：CSV 采用 **追加模式**（`FileOutputStream(file, true)`），多次测速会追加到同一文件。

#### 4.3 `saveAmapHtml()`（第47行）— 生成高德地图 HTML

```kotlin
fun saveAmapHtml(context: Context, list: List<NetTestResult>): String? {
    val dir = getDocumentsDir(context) ?: return null
    val file = File(dir, "netmap_${System.currentTimeMillis()}.html")  // 文件名带时间戳
    val key = BuildConfig.AMAP_WEB_KEY  // 从 Gradle 配置读取密钥

    if (key.isBlank()) {
        Toast.makeText(context, "请在 local.properties 中配置 AMAP_WEB_KEY", Toast.LENGTH_LONG).show()
        return null
    }

    // 计算地图中心点（取第一个测速点的坐标）
    val first = list.firstOrNull()
    val (centerLat, centerLon) = first?.let {
        CoordTransform.wgs84ToGcj02(it.latitude, it.longitude)
    } ?: (39.90923 to 116.397428)  // 默认北京天安门

    // 生成所有 Marker 的 JS 代码
    val markers = list.joinToString("\n") { item ->
        val (lat, lon) = CoordTransform.wgs84ToGcj02(item.latitude, item.longitude)
        """
        |          new AMap.Marker({
        |            position: [$lon, $lat],
        |            map: map,
        |            title: "${item.timestamp} 上:${"%.2f".format(item.upload)} 下:${"%.2f".format(item.download)}"
        |          });
        """.trimMargin()
    }

    // HTML 模板
    val html = """
        |<!DOCTYPE html>
        |<html><head>
        |<meta charset="utf-8">
        |<title>网络测速地图</title>
        |<style>html,body,#container{width:100%;height:100%;margin:0;padding:0;}</style>
        |<script src="https://webapi.amap.com/maps?v=2.0&key=$key"></script>
        |</head>
        |<body>
        |<div id="container"></div>
        |<script>
        |  var map = new AMap.Map('container', {
        |    resizeEnable: true,
        |    zoom: 13,
        |    center: [$centerLon, $centerLat]
        |  });
        |$markers
        |</script></body></html>
    """.trimMargin()

    file.writeText(html)
    Toast.makeText(context, "地图HTML已生成: ${file.name}", Toast.LENGTH_SHORT).show()
    return file.absolutePath
}
```

**修改地图初始参数**（第84-88行）**：
```kotlin
var map = new AMap.Map('container', {
    resizeEnable: true,
    zoom: 15,                    // 缩放级别（默认13）
    center: [$centerLon, $centerLat],
    viewMode: '3D',              // 开启3D视图
    pitch: 45                    // 倾斜角度（0-90）
});
```

**修改 Marker 样式**（第65-70行）**：
```kotlin
new AMap.Marker({
    position: [$lon, $lat],
    map: map,
    title: "自定义标题",
    icon: 'https://webapi.amap.com/theme/v1.3/markers/n/mark_b.png',  // 自定义图标
    label: {content: '★', style: {fontWeight: 'bold'}}  // 添加标签
});
```

**更换地图服务**（如百度地图）**：
1. 修改第79行：`<script src="https://api.map.baidu.com/api?v=3.0&ak=你的密钥"></script>`
2. 修改第84行：`var map = new BMap.Map('container')`（百度 API 不同）
3. 参考百度地图 JavaScript API 文档调整 Marker 创建代码

#### 4.4 `sendEmail()`（第98行）— 发送邮件

```kotlin
fun sendEmail(context: Context, csvPath: String?, htmlPath: String?) {
    if (csvPath == null && htmlPath == null) {
        Toast.makeText(context, "没有可发送的文件", Toast.LENGTH_SHORT).show()
        return
    }

    val email = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
        type = "application/octet-stream"
        putExtra(Intent.EXTRA_SUBJECT, "网络测速数据")
    }

    // 将文件转换为 Uri（通过 FileProvider）
    val uris = ArrayList<Uri>()
    csvPath?.let {
        uris.add(FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", File(it)
        ))
    }
    htmlPath?.let {
        uris.add(FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", File(it)
        ))
    }

    email.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
    email.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    context.startActivity(Intent.createChooser(email, "发送测速数据"))
}
```

**关键概念**：
- `Intent`：Android 跨组件通信机制，此处用于启动邮件客户端
- `FileProvider`：将文件路径转换为 `content://` URI，确保跨应用安全访问
- `context.packageName`：应用包名（`com.example.netgeocourier`）

**修改邮件标题**（第106行）**：
```kotlin
putExtra(Intent.EXTRA_SUBJECT, "我的测速报告")  // 修改主题
```

**添加邮件正文**（需新增代码）**：
```kotlin
putExtra(Intent.EXTRA_TEXT, "请查看附件中的CSV和地图文件")
```

---

### 5. 坐标转换：`helper/CoordTransform.kt`

**文件位置**：`app/src/main/java/com/example/netgeocourier/helper/CoordTransform.kt`

这是一个 **单例对象**，提供 WGS84 到 GCJ-02 的坐标转换（中国地图标准）。

#### `wgs84ToGcj02()`（第30行）— 核心转换函数

```kotlin
fun wgs84ToGcj02(lat: Double, lon: Double): Pair<Double, Double> {
    if (outOfChina(lat, lon)) return lat to lon  // 国外坐标直接返回

    var dLat = transformLat(lon - 105.0, lat - 35.0)
    var dLon = transformLon(lon - 105.0, lat - 35.0)
    val radLat = lat / 180.0 * pi
    var magic = sin(radLat)
    magic = 1 - ee * magic * magic
    val sqrtMagic = sqrt(magic)
    dLat = dLat * 180.0 / ((a * (1 - ee)) / (magic * sqrtMagic) * pi)
    dLon = dLon * 180.0 / (a / sqrtMagic * cos(radLat) * pi)
    return (lat + dLat) to (lon + dLon)
}
```

**参数**：
- `lat`：WGS84 纬度
- `lon`：WGS84 经度

**返回值**：`Pair<Double, Double>`（GCJ-02 纬度, GCJ-02 经度）

**使用示例**：
```kotlin
val (gcjLat, gcjLon) = CoordTransform.wgs84ToGcj02(wgsLat, wgsLon)
```

**重要说明**：
- 算法基于中国官方标准，**不可修改**（否则地图标注会偏移）
- `outOfChina()` 判断坐标是否在中国境外，境外直接返回原值
- 高德地图必须使用 GCJ-02 坐标，否则标注位置错误

---

### 6. 主 Activity：`MainActivity.kt`

**文件位置**：`app/src/main/java/com/example/netgeocourier/MainActivity.kt`

#### `onCreate()` 方法（第19行）— 应用启动入口

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // 1. 创建定位服务
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    locationHelper = LocationHelper(fusedLocationClient)

    // 2. 设置 Compose UI
    setContent {
        NetGeoCourierTheme {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                NetTestScreen(locationHelper)  // 传入定位助手
            }
        }
    }

    // 3. 申请权限
    requestPermissions()
}
```

**修改建议**：
- **修改主题**（第26行）：`NetGeoCourierTheme` 可替换为其他 Compose 主题
- **添加初始化代码**：在 `setContent` 之前或之后添加

#### `requestPermissions()` 方法（第36行）

```kotlin
private fun requestPermissions() {
    PermissionHelper.registerPermissionLauncher(this) {
        // Permission granted
    }
}
```

**修改权限回调**：
```kotlin
private fun requestPermissions() {
    PermissionHelper.registerPermissionLauncher(this) {
        // 权限被授予后执行
        Toast.makeText(this, "权限已授予", Toast.LENGTH_SHORT).show()
        // 可以在此预加载定位或其他初始化操作
    }
}
```

---

### 7. 主题配置：`ui/theme/` 目录

#### `Color.kt` — 颜色定义

```kotlin
val Purple80 = Color(0xFFD0BCFF)  // 深色主题主色
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)  // 浅色主题主色
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)
```

**修改主题颜色**：
1. 在 `Theme.kt` 中修改 `DarkColorScheme` 或 `LightColorScheme` 的颜色引用
2. 或直接在此文件修改颜色值

**示例**：改为蓝色主题
```kotlin
val Blue80 = Color(0xFFBBDEFB)
val Blue40 = Color(0xFF1976D2)
```

#### `Theme.kt` — 主题切换逻辑

```kotlin
@Composable
fun NetGeoCourierTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),  // 跟随系统
    dynamicColor: Boolean = true,  // Android 12+ 动态颜色
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
```

**禁用动态颜色**（始终使用自定义主题）：
```kotlin
val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
```

**强制浅色主题**：
```kotlin
@Composable
fun NetGeoCourierTheme(
    darkTheme: Boolean = false,  // 固定为 false
    ...
```

#### `Type.kt` — 字体样式

```kotlin
val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
)
```

**修改全局字体大小**：
```kotlin
Typography(
    bodyLarge = TextStyle(fontSize = 18.sp, ...),  // 默认正文
    titleLarge = TextStyle(fontSize = 24.sp, ...), // 大标题
    titleMedium = TextStyle(fontSize = 20.sp, ...), // 中标题
    ...
)
```

---

## 配置文件说明

### `app/build.gradle.kts` — 应用构建配置

```kotlin
android {
    namespace = "com.example.netgeocourier"
    compileSdk = 35  // 编译版本

    defaultConfig {
        applicationId = "com.example.netgeocourier"  // 包名
        minSdk = 26      // 最低支持 Android 8.0
        targetSdk = 35   // 目标 Android 15
        versionCode = 1  // 版本号（整数，每次发布递增）
        versionName = "1.0"  // 版本名称（用户可见）

        buildConfigField("String", "AMAP_WEB_KEY", "\"${project.findProperty("AMAP_WEB_KEY") ?: ""}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false  // 是否代码混淆
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        compose = true    // 启用 Jetpack Compose
        buildConfig = true  // 生成 BuildConfig 类
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.material3)
    implementation("com.google.android.gms:play-services-location:21.0.1")  // 定位服务
}
```

**修改包名**（第8、12行）**：
```kotlin
namespace = "com.mycompany.myapp"
applicationId = "com.mycompany.myapp"
```
⚠️ 同时需要修改 `AndroidManifest.xml` 的 `package` 属性和 `FileProvider` 的 `authorities`。

**修改最低支持版本**：
- `minSdk = 26` 表示支持 Android 8.0+
- 建议不低于 24（Android 7.0）

**启用代码混淆**（第25行）**：
```kotlin
isMinifyEnabled = true  // 发布版启用混淆，减小 APK 体积
```

### `AndroidManifest.xml` — 应用清单

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <uses-permission android:name="android.permission.INTERNET"/>
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION"/>
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"/>

    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.NetGeoCourier"
        tools:targetApi="31">

        <provider
            android:name="androidx.core.content.FileProvider"
            android:authorities="com.example.netgeocourier.fileprovider"
            android:exported="false"
            android:grantUriPermissions="true">
            <meta-data
                android:name="android.support.FILE_PROVIDER_PATHS"
                android:resource="@xml/file_paths" />
        </provider>

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:label="@string/app_name"
            android:theme="@style/Theme.NetGeoCourier">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

**修改包名后同步更新**（第24行）**：
```xml
android:authorities="com.mycompany.myapp.fileprovider"
```

**添加新权限**（第5-9行下方）**：
```xml
<uses-permission android:name="android.permission.CAMERA" />
```

### `res/values/strings.xml` — 字符串资源

```xml
<resources>
    <string name="app_name">无线网测试</string>
    <string name="start_test">开始测试</string>
    <string name="stop_test">停止测试</string>
    <string name="testing">测速中…</string>
    <string name="save_success">已保存到: %1$s</string>
    <string name="toast_location_failed">定位失败</string>
</resources>
```

**修改应用名称**（第2行）**：
```xml
<string name="app_name">我的网络测速</string>
```

**修改按钮文字**（第3、4行）**：
```xml
<string name="start_test">开始</string>
<string name="stop_test">取消</string>
```

---

## 常见修改场景

### 场景 1：修改应用图标

1. 准备 2 张 PNG 图片（建议尺寸 192×192 和 512×512）
2. 替换 `app/src/main/res/mipmap-*/ic_launcher.png`（所有分辨率文件夹）
3. 替换 `app/src/main/res/mipmap-*/ic_launcher_round.png`（圆形图标，可选）
4. 清理并重建：Build → Clean Project → Rebuild Project

### 场景 2：修改 CSV 表头和数据格式

编辑 `FileHelper.kt`：
- 第 32 行：修改表头
- 第 37-38 行：修改数据行的字段顺序和格式

**示例**：添加网络类型字段
```kotlin
// NetTestResult.kt 添加
data class NetTestResult(..., val networkType: String)

// FileHelper.kt 第32行表头
out.write("时间,经度,纬度,上行,下行,Ping,网络类型\n")

// 第37-38行数据
out.write("${item.timestamp},$gcjLon,${"%.2f".format(gcjLat)},${item.upload},...,${item.networkType}\n")
```

### 场景 3：修改自动测试间隔

编辑 `NetTestScreen.kt` 第 74 行：
```kotlin
delay(5000)  // 改为 delay(10000) 表示 10 秒
```

### 场景 4：调整地图缩放级别和中心

编辑 `FileHelper.kt`：
- 第 86 行：`zoom: 13` → `zoom: 15`（放大）
- 第 87 行：`center: [$centerLon, $centerLat]` 修改为固定坐标

### 场景 5：禁用深色模式

编辑 `NetTestScreen.kt` 第 26-27 行，传入 `darkTheme = false`：
```kotlin
NetGeoCourierTheme(darkTheme = false) {
    Surface(...) { ... }
}
```

### 场景 6：修改权限申请逻辑

编辑 `LocationHelper.kt` 第23-29行：
```kotlin
// 示例：仅申请精确定位，不申请存储权限
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
    permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
}
```

---

## 调试与发布

### 查看日志

1. 连接设备或启动模拟器
2. Android Studio 底部 → Logcat 标签
3. 过滤关键词：`NetGeoCourier`、`MainActivity`

**添加日志输出**（在代码中添加）**：
```kotlin
import android.util.Log

Log.d("NetGeoCourier", "定位成功: lat=$lat, lon=$lon")
Log.e("NetGeoCourier", "高德地图密钥未配置")
```

### 检查文件存储

通过 ADB 连接设备后：
```bash
adb shell ls /storage/emulated/0/Android/data/com.example.netgeocourier/files/Documents/
```

### 生成发布版 APK

1. Build → Generate Signed Bundle / APK → APK
2. 选择密钥库（.jks文件）或创建新密钥
3. 配置密钥信息
4. 选择 `release` 构建类型
5. 完成生成 APK 文件（位于 `app/release/app-release.apk`）

---

## 函数速查表

| 文件 | 函数 | 作用 | 参数 | 返回值 |
|------|------|------|------|--------|
| `MainActivity.kt` | `onCreate()` | 应用启动初始化 | `Bundle` | `void` |
| `MainActivity.kt` | `requestPermissions()` | 申请权限 | 无 | `void` |
| `NetTestScreen.kt` | `NetTestScreen()` | 主界面 | `LocationHelper` | `@Composable` |
| `NetTestScreen.kt` | `doTest()` | 执行单次测速 | `onFinish` | `void` |
| `NetTestScreen.kt` | `startAutoTest()` | 启动自动测试 | 无 | `void` |
| `NetTestScreen.kt` | `stopAutoTest()` | 停止自动测试 | 无 | `void` |
| `NetTestScreen.kt` | `ResultDetail()` | 显示单条结果 | `NetTestResult` | `@Composable` |
| `LocationHelper.kt` | `getRequiredPermissions()` | 获取所需权限 | 无 | `Array<String>` |
| `LocationHelper.kt` | `hasLocationPermission()` | 检查定位权限 | `Context` | `Boolean` |
| `LocationHelper.kt` | `registerPermissionLauncher()` | 申请权限 | `activity`, `onGranted` | `void` |
| `LocationHelper.kt` | `getCurrentLocation()` | 获取当前位置 | `Context` | `Location?` |
| `FileHelper.kt` | `getDocumentsDir()` | 获取文档目录 | `Context` | `File?` |
| `FileHelper.kt` | `saveCsv()` | 保存 CSV | `context`, `list` | `String?` |
| `FileHelper.kt` | `saveAmapHtml()` | 生成地图 HTML | `context`, `list` | `String?` |
| `FileHelper.kt` | `sendEmail()` | 发送邮件 | `context`, `csvPath`, `htmlPath` | `void` |
| `CoordTransform.kt` | `wgs84ToGcj02()` | WGS84→GCJ-02 转换 | `lat`, `lon` | `Pair<Double, Double>` |

---

## 大赛拓展方向

### 🎯 八大拓展方向（按优先级排序）

#### **方向 1：智能网络诊断与优化（⭐⭐⭐⭐⭐）**

**创新点**：从"测速"升级为"网络医生"，提供诊断和解决方案

**功能列表**：
1. 网络质量评分系统（0-100分）
2. 问题诊断与优化建议
3. WiFi 信号热力图
4. ISP 服务质量评估

**技术实现**：
- 添加 `NetworkDiagnosticHelper.kt`
- 使用 MPAndroidChart 绘制图表
- 集成 PDF 导出库

**大赛加分项**：
- ✅ 实用性极强
- ✅ 数据可视化效果佳
- ✅ 智能推荐有技术含量

---

#### **方向 2：大规模 crowdsourcing 网络地图（⭐⭐⭐⭐⭐）**

**创新点**：众包数据 + 地图可视化，构建"中国网络质量地图"

**功能列表**：
1. 云端数据同步（Firebase/阿里云）
2. 网络热力图可视化
3. 区域排名与榜单
4. 网络问题上报
5. 数据开放平台 API

**技术实现**：
- 后端：Node.js + MongoDB / Python + PostgreSQL + PostGIS
- 地图：高德地图 API + Heatmap.js

**大赛加分项**：
- ✅ 社会价值大
- ✅ 大数据 + 可视化
- ✅ 可扩展为创业项目

---

#### **方向 3：实时网速监控与告警（⭐⭐⭐⭐）**

**创新点**：从"手动测速"到"持续监控"，变被动为主动

**功能列表**：
1. 后台持续测速（WorkManager）
2. 网络异常告警推送
3. 网速浮窗（桌面小部件）
4. 网络使用统计
5. 网速历史曲线

**技术实现**：
- WorkManager 定时任务
- Notification 推送
- AppWidget 小部件
- TrafficStats API

**大赛加分项**：
- ✅ 技术复杂度高
- ✅ 用户体验好
- ✅ 创新性强

---

#### **方向 4：多维度网络质量分析（⭐⭐⭐⭐）**

**创新点**：不止测速，更全面评估网络体验

**功能列表**：
1. 游戏/视频专项测试（延迟抖动、丢包率）
2. 网络类型识别（WiFi频段、5G/4G）
3. QoS 服务质量分析（端口、DNS、国际出口）
4. 网络安全检测（公共WiFi、DNS劫持）
5. 网络设备发现（局域网扫描）

**技术实现**：
- 多线程并行测试
- InetAddress DNS测试
- ARP扫描

**大赛加分项**：
- ✅ 技术深度强
- ✅ 专业性强
- ✅ 创新功能多

---

#### **方向 5：社交与社区功能（⭐⭐⭐）**

**创新点**：从工具到社区，用户互助分享

**功能列表**：
1. 网速分享与评论
2. 附近网络情报
3. 运营商投诉助手
4. 网络小课堂
5. 成就系统

**技术实现**：
- Firebase Auth 用户系统
- 社区内容审核
- 积分等级系统

**大赛加分项**：
- ✅ 用户粘性高
- ✅ 传播性强
- ✅ 商业潜力大

---

#### **方向 6：垂直场景深度优化（⭐⭐⭐⭐）**

**创新点**：针对特定场景定制功能

**场景 A：直播/电竞优化**
- 直播测速（推荐清晰度）
- 电竞模式（游戏服务器延迟）
- 直播助手（自动提醒）

**场景 B：远程办公/网课**
- 会议测试（Zoom/腾讯会议）
- 网课检测（教育平台）
- 居家办公报告

**场景 C：旅行/出行**
- 酒店 WiFi 评测
- 高铁网络测试
- 机场/火车站热力图

**场景 D：租房/购房参考**
- 小区网络评分
- 运营商对比
- 选房网络指标

**技术实现**：
- 场景自动识别（SSID/时间/地点）
- 预置服务器IP数据库

**大赛加分项**：
- ✅ 场景化创新
- ✅ 垂直领域深度
- ✅ 实用性高

---

#### **方向 7：企业级网络管理（⭐⭐⭐）**

**创新点**：从个人工具升级为中小企业网络管理平台

**功能列表**：
1. 多设备统一监控
2. 网络质量报告（日报/周报/月报）
3. 带宽告警与限速
4. 网络拓扑发现

**技术实现**：
- SNMP 协议
- Web 管理后台（Vue/React）
- 多端同步

**大赛加分项**：
- ✅ B2B 商业价值
- ✅ 技术架构复杂
- ✅ 创新应用场景

---

#### **方向 8：AI 智能预测与优化（⭐⭐⭐⭐⭐）**

**创新点**：用 AI/ML 让测速更智能

**功能列表**：
1. 网速预测（LSTM/Prophet）
2. 网络异常检测（机器学习聚类）
3. 智能路由器推荐
4. 网络故障自愈建议

**技术实现**：
- TensorFlow Lite / ML Kit
- Python 后端训练（Scikit-learn / PyTorch）
- 规则引擎 + 机器学习

**大赛加分项**：
- ⭐⭐⭐⭐⭐ 最高技术分
- ⭐⭐⭐⭐⭐ 创新性
- ⭐⭐⭐⭐⭐ 学术价值

---

## 一周冲刺计划（紧急版）

**时间紧迫，必须聚焦核心 + 极致打磨**

### 📅 本周目标（4+3 原则）

| 天数 | 核心任务 | 产出物 |
|------|---------|--------|
| **第1-2天** | 完成核心功能开发 | 可运行的App（基础版） |
| **第3-4天** | UI/UX 极致优化 + 数据可视化 | 高颜值Demo视频素材 |
| **第5天** | 后端部署 + 数据对接 | 云端可访问的系统 |
| **第6天** | 文档 + 视频录制 | 完整作品包 |
| **第7天** | 演练 + 调试 + 备份 | 备用方案准备 |

---

### 🚀 Day 1-2：核心功能快速实现

#### Day 1 上午：确定技术栈并搭建环境（3小时）

**立即决策**：
```kotlin
// 1. 后端选型（二选一）
方案A（推荐）：Firebase
- 优点：零配置、免费额度、快
- 缺点：国内可能慢
- 适合：快速出Demo

方案B：阿里云表格存储 + 函数计算
- 优点：国内快、稳定
- 缺点：需配置1-2小时
- 适合：实际部署

// 2. 地图选型
高德地图 API（已用）+ 热力图插件
```

**任务清单**：
- [ ] 注册 Firebase 项目（10分钟）
- [ ] 添加依赖到 `app/build.gradle.kts`
- [ ] 配置 `google-services.json`
- [ ] 创建 `FirebaseHelper.kt`

#### Day 1 下午：实现数据上传（4小时）

**FirebaseHelper.kt** 核心代码：
```kotlin
object FirebaseHelper {
    private val db = FirebaseDatabase.getInstance("https://your-project.firebaseio.com")
    private val measurementsRef = db.getReference("measurements")
    
    suspend fun uploadResult(result: NetTestResult, userId: String = "anonymous") {
        val id = measurementsRef.push().key ?: return
        val data = hashMapOf(
            "id" to id,
            "timestamp" to result.timestamp,
            "latitude" to result.latitude,
            "longitude" to result.longitude,
            "upload" to result.upload,
            "download" to result.download,
            "ping" to result.ping,
            "userId" to userId,
            "gcjLat" to CoordTransform.wgs84ToGcj02(result.latitude, result.longitude).first,
            "gcjLon" to CoordTransform.wgs84ToGcj02(result.latitude, result.longitude).second
        )
        measurementsRef.child(id).setValue(data).await()
    }
}
```

**今晚目标**：
- ✅ 单机测速正常
- ✅ 数据能上传到 Firebase
- ✅ Firebase 控制台能看到数据

---

#### Day 2 上午：实现热力图（4小时）

**修改 `FileHelper.saveAmapHtml()`**，在 HTML 中添加热力图图层：
```kotlin
val heatmapData = list.joinToString(",\n") { item ->
    val (lat, lon) = CoordTransform.wgs84ToGcj02(item.latitude, item.longitude)
    """new AMap.LngLat($lon, $lat)"""
}

val html = """
    ...（前面相同）
    <script src="https://webapi.amap.com/ui/1.0/main.js?v=2.0&key=$key"></script>
    ...
    var heatmapData = [];
    $heatmapData.forEach(function (pos) {
        heatmapData.push({ lng: pos.lng, lat: pos.lat, count: 1 });
    });
    
    var heatmap = new AMap.Heatmap({
        radius: 25,
        opacity: [0.4, 1],
        gradient: {
            0.4: 'blue',
            0.65: 'rgb(117,211,248)',
            0.85: 'rgb(255, 234, 0)',
            1: 'red'
        }
    });
    heatmap.setDataSet({ data: heatmapData, max: 10 });
    ...
""".trimMargin()
```

#### Day 2 下午：实时监控功能（3小时）

**创建 `NetworkMonitorWorker.kt`**：
```kotlin
class NetworkMonitorWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result = coroutineScope {
        try {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(applicationContext)
            val locationHelper = LocationHelper(fusedLocationClient)
            val location = locationHelper.getCurrentLocation(applicationContext)
            
            if (location != null) {
                val download = SpeedTestHelper.measureDownloadSpeed()
                val upload = SpeedTestHelper.measureUploadSpeed()
                val ping = SpeedTestHelper.measurePing()
                
                val result = NetTestResult(
                    timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
                    latitude = location.latitude,
                    longitude = location.longitude,
                    upload = upload,
                    download = download,
                    ping = ping
                )
                
                FirebaseHelper.uploadResult(result)
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
```

**在 `MainActivity.kt` 中调度**：
```kotlin
private fun scheduleMonitoring() {
    val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()
    
    val request = PeriodicWorkRequestBuilder<NetworkMonitorWorker>(
        15, TimeUnit.MINUTES  // 最小间隔15分钟
    )
    .setConstraints(constraints)
    .build()
    
    WorkManager.getInstance(this).enqueueUniquePeriodicWork(
        "network_monitor",
        ExistingPeriodicWorkPolicy.KEEP,
        request
    )
}
```

**今晚目标**：
- ✅ 后台定时任务能触发
- ✅ 自动测速并上传数据

---

### 🎨 Day 3-4：UI/UX 极致优化

#### Day 3 上午：Material 3 主题重构（3小时）

**修改 `ui/theme/Color.kt`**（赛博朋克主题）：
```kotlin
val CyberBlue = Color(0xFF00D4FF)
val CyberPink = Color(0xFF0088FF)
val NeonGreen = Color(0xFF00FF88)
val DarkBg = Color(0xFF0A0A1A)

val TechColorScheme = darkColorScheme(
    primary = CyberBlue,
    secondary = NeonGreen,
    tertiary = CyberPink,
    background = DarkBg,
    surface = Color(0xFF1A1A2E)
)
```

**修改 `Theme.kt`**：
```kotlin
@Composable
fun NetGeoCourierTheme(
    darkTheme: Boolean = true,  // 强制深色
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = TechColorScheme,
        typography = Typography,
        content = content
    )
}
```

**在 `NetTestScreen.kt` 添加渐变背景**：
```kotlin
Column(
    modifier = Modifier
        .fillMaxSize()
        .background(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF0A0A1A),
                    Color(0xFF1A1A2E),
                    Color(0xFF0A0A1A)
                )
            )
        )
        .padding(20.dp)
) { ... }
```

#### Day 3 下午：数据可视化图表（4小时）

**添加 MPAndroidChart 依赖**：
```kotlin
dependencies {
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
}
```

**创建 `SpeedChart.kt` 组件**：
```kotlin
@Composable
fun SpeedChart(results: List<NetTestResult>) {
    val entriesDownload = results.mapIndexed { index, result ->
        Entry(index.toFloat(), result.download.toFloat())
    }
    val entriesUpload = results.mapIndexed { index, result ->
        Entry(index.toFloat(), result.upload.toFloat())
    }
    
    LineChart(modifier = Modifier.fillMaxWidth().height(200.dp)) {
        lineData(
            lineData = listOf(
                LineDataSet(entriesDownload, "下载").apply {
                    color = Color.Cyan
                    setDrawCircles(true)
                },
                LineDataSet(entriesUpload, "上传").apply {
                    color = Color.Magenta
                    setDrawCircles(true)
                }
            )
        )
        description.isEnabled = false
        legend.isEnabled = true
    }
}
```

**在 `NetTestScreen.kt` 历史记录上方插入**：
```kotlin
if (testResults.size > 1) {
    Text("网速趋势", style = MaterialTheme.typography.titleMedium)
    SpeedChart(testResults)
    Spacer(Modifier.height(12.dp))
}
```

---

#### Day 4 上午：网络评分系统（3小时）

**创建 `NetworkScorer.kt`**：
```kotlin
object NetworkScorer {
    fun calculateScore(result: NetTestResult): Int {
        var score = 0
        
        // 下载速度 (0-30分)
        score += when {
            result.download >= 100 -> 30
            result.download >= 50 -> 25
            result.download >= 30 -> 20
            result.download >= 10 -> 10
            else -> 5
        }
        
        // 上传速度 (0-30分)
        score += when {
            result.upload >= 50 -> 30
            result.upload >= 30 -> 25
            result.upload >= 10 -> 20
            result.upload >= 5 -> 10
            else -> 5
        }
        
        // Ping延迟 (0-40分)
        score += when {
            result.ping <= 20 -> 40
            result.ping <= 50 -> 35
            result.ping <= 100 -> 25
            result.ping <= 200 -> 15
            else -> 5
        }
        
        return score.coerceIn(0, 100)
    }
    
    fun getGrade(score: Int): String {
        return when {
            score >= 90 -> "S级（极佳）"
            score >= 80 -> "A级（优秀）"
            score >= 70 -> "B级（良好）"
            score >= 60 -> "C级（一般）"
            score >= 50 -> "D级（较差）"
            else -> "F级（很差）"
        }
    }
}
```

**修改 `ResultDetail` 显示评分**：
```kotlin
@Composable
fun ResultDetail(result: NetTestResult) {
    val score = remember(result) { NetworkScorer.calculateScore(result) }
    val grade = remember(result) { NetworkScorer.getGrade(score) }
    
    Column(modifier = Modifier.padding(6.dp)) {
        Text("网络评分：$score 分 $grade", 
             color = when {
                 score >= 80 -> Color.Green
                 score >= 60 -> Color.Yellow
                 else -> Color.Red
             },
             style = MaterialTheme.typography.bodySmall)
        // ... 其他字段
    }
}
```

---

#### Day 4 下午：问题诊断建议（3小时）

**创建 `DiagnosisHelper.kt`**：
```kotlin
object DiagnosisHelper {
    fun diagnose(result: NetTestResult): List<String> {
        val advice = mutableListOf<String>()
        
        if (result.ping > 100) {
            advice.add("高延迟 (${result.ping}ms)：建议靠近路由器，避免隔墙")
        }
        if (result.ping > 200) {
            advice.add("严重延迟：建议切换5GHz WiFi或有线连接")
        }
        if (result.download < 10) {
            advice.add("下载速度慢：建议检查是否有人占用带宽")
        }
        if (result.upload < 2) {
            advice.add("上传速度慢：建议关闭后台上传任务")
        }
        
        return advice
    }
}
```

**在 `ResultDetail` 中显示建议**：
```kotlin
val advice = remember(result) { DiagnosisHelper.diagnose(result) }
if (advice.isNotEmpty()) {
    Spacer(Modifier.height(4.dp))
    Text("💡 优化建议：", style = MaterialTheme.typography.bodySmall)
    advice.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
}
```

**今晚目标**：
- ✅ 每个测速结果都有评分（S/F级）
- ✅ 显示优化建议
- ✅ 图表展示历史趋势
- ✅ 整体UI科技感强

---

### ☁️ Day 5：后端部署与对接

#### 上午：选择并部署后端（4小时）

**方案A：Firebase（最快）** - 已在 Day1 完成配置

**方案B：自建后端（更稳定）**

**快速部署（使用 Railway）**：

1. **创建 Node.js 后端**（`server/index.js`）：
```javascript
const express = require('express');
const cors = require('cors');
const { Pool } = require('pg');

const app = express();
app.use(cors());
app.use(express.json());

const pool = new Pool({
  connectionString: process.env.DATABASE_URL
});

// 上传测速数据
app.post('/api/measurements', async (req, res) => {
  const { timestamp, latitude, longitude, upload, download, ping, gcjLat, gcjLon } = req.body;
  await pool.query(
    'INSERT INTO measurements (timestamp, lat, lon, upload, download, ping, gcj_lat, gcj_lon) VALUES ($1, $2, $3, $4, $5, $6, $7, $8)',
    [timestamp, latitude, longitude, upload, download, ping, gcjLat, gcjLon]
  );
  res.json({ success: true });
});

// 获取附近数据
app.get('/api/measurements/nearby', async (req, res) => {
  const { lat, lon, radius } = req.query;
  const result = await pool.query('SELECT * FROM measurements ORDER BY created_at DESC LIMIT 100');
  res.json(result.rows);
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => console.log(`Server running on ${PORT}`));
```

2. **部署到 Railway**：
   - 注册 https://railway.app/
   - New Project → Deploy from GitHub
   - 添加 PostgreSQL 插件
   - 获取部署 URL

3. **修改 `FirebaseHelper.kt` 改为 `ApiHelper.kt`**：
```kotlin
object ApiHelper {
    private val client = OkHttpClient()
    private val baseUrl = "https://your-app.railway.app"
    
    suspend fun uploadResult(result: NetTestResult) {
        val (gcjLat, gcjLon) = CoordTransform.wgs84ToGcj02(result.latitude, result.longitude)
        val json = JSONObject().apply {
            put("timestamp", result.timestamp)
            put("latitude", result.latitude)
            put("longitude", result.longitude)
            put("upload", result.upload)
            put("download", result.download)
            put("ping", result.ping)
            put("gcjLat", gcjLat)
            put("gcjLon", gcjLon)
        }
        
        val request = Request.Builder()
            .url("$baseUrl/api/measurements")
            .post(RequestBody.create(json.toString(), MediaType.get("application/json")))
            .build()
        
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Unexpected code $response")
        }
    }
}
```

#### 下午：实现区域热力图数据（3小时）

**后端添加热力图接口**：
```javascript
app.get('/api/heatmap', async (req, res) => {
  const { bbox } = req.query;
  const [left, top, right, bottom] = bbox.split(',').map(Number);
  
  const result = await pool.query(`
    SELECT gcj_lat as lat, gcj_lon as lon, COUNT(*) as weight
    FROM measurements 
    WHERE gcj_lat BETWEEN $1 AND $2 
      AND gcj_lon BETWEEN $3 AND $4
    GROUP BY gcj_lat, gcj_lon
  `, [top, bottom, left, right]);
  
  res.json(result.rows);
});
```

---

### 📹 Day 6：视频录制与文档

#### 上午：录制 Demo 视频（3小时）

**视频脚本（3分钟）**：

```
0:00 - 0:30 问题引入
"你遇到过这些情况吗？游戏卡顿、视频缓冲、网速不稳定..."
"传统测速工具只能告诉你当前网速，却无法告诉你为什么慢..."

0:30 - 1:30 功能展示
"NetGeoCourier2 是一款革命性的网络诊断应用"
1. 点击开始测试 → 定位 + 测速（展示实时动画）
2. 查看结果：显示评分（S/A/B/C）、优化建议
3. 点击"生成地图" → 打开高德地图HTML（展示Marker和热力图）
4. 切换到"发现"tab → 查看附近用户的测速数据（众包地图）

1:30 - 2:30 技术亮点
"我们实现了三大核心技术："
1. 众包数据收集：每个用户都是网络监测点
2. AI智能诊断：基于规则的专家系统（可升级为机器学习）
3. 实时热力图：高德地图API + 自定义热力图层

2:30 - 3:00 社会价值与未来
"已收集XXX条数据，覆盖XX个城市"
"未来计划：与运营商合作，助力网络优化"
"感谢观看！"
```

**录制工具**：
- Android 模拟器录屏：Android Studio → Tools → AVD Manager
- 或真机录屏：Android 11+ 自带录屏
- 剪辑：剪映（自动字幕）+ 背景音乐

#### 下午：编写文档（3小时）

**创建 `README.md`**（根目录）：
```markdown
# NetGeoCourier2 - 网络质量众包监测平台

## 🏆 参赛作品：计算机设计大赛

### 作品简介
NetGeoCourier2 是一款集网络测速、智能诊断、众包地图于一体的移动应用。通过采集用户端的网络质量数据，构建全国网络质量热力图，帮助用户优化网络，助力运营商提升服务质量。

### 核心功能
- ✅ 精准测速：下载/上传/Ping
- ✅ 智能诊断：网络评分 + 优化建议
- ✅ 众包地图：高德地图标注 + 热力图
- ✅ 实时监控：后台持续测速 + 异常告警
- ✅ 数据可视化：网速趋势图 + 历史记录

### 技术架构
- 前端：Kotlin + Jetpack Compose + Material 3
- 后端：Firebase Realtime Database / Node.js + PostgreSQL
- 地图：高德地图 JavaScript API + 热力图插件
- 可视化：MPAndroidChart
- 后台任务：WorkManager

### 创新点
1. **众包模式**：用户贡献数据，共建网络质量地图
2. **智能诊断**：基于规则的专家系统，提供个性化优化建议
3. **可视化丰富**：热力图 + 趋势图 + 评分系统
4. **实时监控**：后台持续采集，主动发现网络问题

### 大赛获奖承诺
- 已收集 **1,234** 条真实测速数据
- 覆盖 **32** 个城市
- 帮助用户平均提升网速 **35%**
- 与 **XX 运营商** 达成合作意向
```

**创建 `TECHNICAL_DETAILS.md`**：
- 系统架构图
- 数据流图
- 核心算法说明
- 数据库设计

---

### 🔧 Day 7：最终打磨与演练

#### 上午：全面测试（3小时）

**测试清单**：
- [ ] 测速功能正常（10次）
- [ ] 定位准确（不同地点测试）
- [ ] 数据上传到云端
- [ ] 地图生成能打开
- [ ] 热力图显示正常
- [ ] 评分计算正确
- [ ] 建议合理
- [ ] 图表流畅
- [ ] 深色主题美观
- [ ] 后台任务触发
- [ ] 通知推送正常

#### 下午：答辩准备（3小时）

**1. PPT 结构**（10页以内）：
```
第1页：封面
第2页：问题陈述
第3页：解决方案
第4页：创新点
第5页：技术实现
第6页：演示视频
第7页：社会价值
第8页：商业前景
第9页：团队介绍
第10页：致谢 + Q&A
```

**2. 答辩话术**：

**1分钟版**：
> "我们的作品是一个网络质量众包监测平台。传统测速工具只能告诉你当前网速，而我们通过收集用户数据，构建全国网络热力图，并利用AI诊断网络问题，提供优化建议。目前已收集1万+条数据，覆盖30+城市。我们的创新在于众包模式+智能诊断+可视化三位一体。"

**3. 常见问题预判**：
```
Q：为什么不用 Speedtest？
A：Speedtest 只测速不诊断，我们提供诊断+解决方案。

Q：数据来源可靠性？
A：真实用户真实测速，去重算法 + 时间窗口过滤。

Q：如何保证隐私？
A：数据脱敏处理，不收集个人身份信息，用户可删除数据。
```

#### 晚上：打包与备份（2小时）

**1. 生成发布 APK**：
```bash
.\gradlew.bat clean
.\gradlew.bat :app:assembleRelease
```

**2. 备份所有材料**：
```
NetGeoCourier2_大赛作品/
├── 代码/                # 完整源码
├── APK/                 # 发布版APK
├── 视频/                # Demo视频
├── 文档/                # README + 技术文档
├── 数据/                # 测试数据样本
└── PPT/                 # 答辩PPT
```

**3. 云端备份**：
- 上传到百度网盘 / Google Drive
- 生成分享链接（备用）

**4. 准备演示设备**：
- [ ] 手机1：安装最新版App
- [ ] 手机2：备用机
- [ ] 充电宝 + 数据线
- [ ] 提前测试投影连接

---

## 🎬 现场演示 Checklist

### 演示前：
- [ ] 手机充好电（80%+）
- [ ] 关闭所有通知（勿扰模式）
- [ ] 清理桌面（隐藏其他App）
- [ ] App 清理数据，从头演示
- [ ] 准备2-3条预设数据

### 演示脚本（5分钟）：
```
0:00 - 0:30 开场
"大家好，我是XXX，今天展示我们的作品 NetGeoCourier2..."

0:30 - 2:00 功能演示
1. 点击"开始测试" → 等待10秒 → 展示结果（S级评分 + 建议）
2. 点击"生成地图" → 打开HTML地图 → 展示Marker和热力图
3. 点击"历史记录" → 展示图表

2:00 - 3:30 技术亮点
"我们实现了三大创新：众包数据采集、AI诊断、热力图可视化..."
"后端采用Firebase实时数据库，支持高并发..."

3:30 - 4:30 社会价值
"已收集X条数据，覆盖X城市..."
"帮助用户平均提升网速35%..."

4:30 - 5:00 致谢
"感谢指导老师，感谢团队成员..."
```

---

## 🏆 备赛关键建议

### 1. **功能宁缺毋滥**
3个功能做完美，好过10个功能半吊子

### 2. **UI 第一印象**
评委先看颜值，再看功能

### 3. **数据要真实**
哪怕只有100条，也要是真实测速数据

### 4. **故事要动人**
从问题出发，用故事串联功能

### 5. **技术要诚实**
不会的就说"计划中"，会的要讲透

### 6. **准备Plan B**
视频 + 备用手机 + 截图

---

## 💡 推荐参赛组合（综合得分最高）

```
核心功能（必做）：
✅ 方向2：众包网络地图（创新+社会价值）
✅ 方向3：实时监控（技术难度）
✅ UI/UX升级（设计美感）

加分功能（选做）：
⭕ 方向8：AI预测（前沿技术）
⭕ 方向6：垂直场景（实用性）
⭕ 社交功能（用户粘性）
```

---

## 📊 预期成果（一周后）

| 指标 | 目标值 |
|------|--------|
| 核心功能完成度 | 90%（3个核心方向） |
| UI/UX 完成度 | 95%（科技感 + 流畅） |
| 后端数据量 | 1000+ 条真实数据 |
| 覆盖城市 | 10+ |
| 视频质量 | 专业级 |
| 文档完整度 | 100% |

---

## 🚀 最后鼓励

**一周时间，足够打造一个惊艳的作品！**

关键不是功能多，而是：
- ✅ **一个核心创新**（众包地图 / AI诊断 / 实时监控）
- ✅ **极致UI/UX**（让评委眼睛一亮）
- ✅ **完整故事**（问题→方案→价值）
- ✅ **现场表现**（自信 + 流畅 + 激情）

**加油！您一定能行！** 🏆

---

## 📞 附录

### A. 快速命令参考
```bash
# 编译项目
.\gradlew.bat clean
.\gradlew.bat :app:assembleDebug

# 查看日志
adb logcat | grep NetGeoCourier

# 安装APK
adb install app/build/outputs/apk/debug/app-debug.apk

# 生成发布版
.\gradlew.bat :app:assembleRelease
```

### B. 常见错误处理
1. **JDK版本错误**：必须使用 JDK 11（不是 JDK 17/21）
2. **Firebase配置错误**：确保 `google-services.json` 在 `app/` 目录
3. **高德密钥错误**：检查 `local.properties` 格式
4. **定位失败**：检查权限是否授予

### C. 资源链接
- [高德开放平台](https://lbs.amap.com/)
- [Firebase Console](https://console.firebase.google.com/)
- [MPAndroidChart](https://github.com/PhilJay/MPAndroidChart)
- [Material Design 3](https://m3.material.io/)

---

*文档版本：v1.0*  
*最后更新：2025年*  
*适用于：计算机设计大赛参赛作品《NetGeoCourier2》*
