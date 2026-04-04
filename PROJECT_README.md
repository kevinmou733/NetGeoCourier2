# NetGeoCourier2 项目详解

## 📱 项目概述

NetGeoCourier2 是一个Android网络测速与定位数据采集应用。它可以测量当前网络速度、记录GPS坐标，并将数据导出为CSV表格或高德地图可视化页面，方便用户分享和存档。

### 主要特点
- 🚀 **网络测速**：测试上行/下行带宽和Ping延迟
- 📍 **GPS定位**：自动获取并转换坐标系（WGS84→GCJ-02）
- 📊 **数据导出**：支持CSV格式和HTML地图页面
- 📧 **邮件分享**：一键通过邮件发送测速数据
- 🎨 **简洁界面**：Material Design风格，操作直观

---

## 🏗️ 项目结构

```
app/
├── src/main/
│   ├── java/com/example/netgeocourier/
│   │   ├── helper/              # 工具类包
│   │   │   ├── LocationHelper.kt    # 定位权限与位置获取
│   │   │   ├── FileHelper.kt        # 文件保存与邮件发送
│   │   │   └── CoordTransform.kt    # 坐标转换算法
│   │   ├── data/
│   │   │   └── NetTestResult.kt     # 数据模型（测速结果）
│   │   └── MainActivity.kt          # 主界面（UI与逻辑）
│   ├── res/                     # 资源文件
│   │   ├── layout/              # 界面布局XML
│   │   ├── values/              # 字符串、颜色、样式
│   │   └── AndroidManifest.xml  # 应用配置清单
│   └── assets/                  # 静态资源（如有）
├── build.gradle                 # 应用级构建配置
└── proguard-rules.pro           # 代码混淆规则
```

---

## 🔧 技术栈

| 技术 | 说明 | 用途 |
|------|------|------|
| **Kotlin** | 现代JVM语言 | 主要开发语言 |
| **Android SDK** | Android 5.0+ (API 21+) | 基础框架 |
| **Google Play Services** | 定位服务 | FusedLocationProviderClient |
| **高德地图API** | 地图服务 | HTML地图生成 |
| **Kotlin协程** | 异步处理 | 定位请求 |

---

## 🚀 快速上手

### 1️⃣ 开发环境准备

1. **安装Android Studio**（最新稳定版）
   - 下载地址：https://developer.android.com/studio
   - 安装时勾选Android SDK和SDK Platform

2. **配置Android SDK**
   - 打开SDK Manager（Tools → SDK Manager）
   - 安装API 33（Android 13）或更高版本的SDK Platform
   - 安装Google Play Services（通过SDK Manager → SDK Tools）

3. **克隆/打开项目**
   ```bash
   # 如果是Git项目
   git clone <项目地址>
   ```
   - 打开Android Studio → File → Open → 选择项目根目录
   - 等待Gradle同步完成（首次可能较慢，需下载依赖）

4. **连接测试设备**
   - **真机**：启用开发者选项和USB调试，用数据线连接
   - **模拟器**：AVD Manager → Create Virtual Device → 创建并启动

### 2️⃣ 配置高德地图密钥

应用地图功能需要高德地图Web API密钥。

1. **注册高德开放平台**：https://lbs.amap.com/dev/key/app
2. **创建应用**：选择"Web服务"类型
3. **获取密钥**：复制生成的`KEY`
4. **配置密钥**：
   - 在项目根目录创建 `local.properties` 文件（如果不存在）
   - 添加以下内容：
     ```properties
     AMAP_WEB_KEY=你的高德地图Web API密钥
     ```
   - **注意**：`local.properties` 已在 `.gitignore` 中，不会被提交到Git

### 3️⃣ 运行应用

- 点击Android Studio顶部绿色Run按钮（▶️）
- 选择连接的设备或已启动的模拟器
- 等待应用安装并自动启动

---

## 📖 代码详解

### 数据模型：`NetTestResult.kt`

```kotlin
data class NetTestResult(
    val timestamp: String,  // 时间戳，格式：yyyy-MM-dd HH:mm:ss
    val latitude: Double,   // WGS84纬度
    val longitude: Double,  // WGS84经度
    val upload: Double,     // 上行速度 (Mbps)
    val download: Double,   // 下行速度 (Mbps)
    val ping: Int           // Ping延迟 (ms)
)
```
**修改建议**：
- 添加新字段（如信号强度、网络类型）时，同步修改 `FileHelper.kt` 中的CSV写入逻辑。

---

### 坐标转换：`CoordTransform.kt`

提供WGS84→GCJ-02坐标系转换（中国地图标准）。

```kotlin
object CoordTransform {
    fun wgs84ToGcj02(lat: Double, lon: Double): Pair<Double, Double>
}
```
**使用示例**：
```kotlin
val (gcjLat, gcjLon) = CoordTransform.wgs84ToGcj02(wgsLat, wgsLon)
```
**重要说明**：
- 算法基于中国官方偏移矫正公式
- 国外坐标自动返回原值（`outOfChina`判断）
- 转换后的坐标用于高德地图标注

---

### 文件操作：`FileHelper.kt`

提供文件保存、HTML生成、邮件发送功能。

#### 获取文档目录
```kotlin
fun getDocumentsDir(context: Context): File?
```
- 路径：`/storage/emulated/0/Android/data/包名/files/Documents/`
- 自动创建目录（如果不存在）
- **无需存储权限**（Android 10+应用私有目录）

#### 保存CSV
```kotlin
fun saveCsv(context: Context, list: List<NetTestResult>): String?
```
- 文件名为 `nettest.csv`
- 自动追加模式（多次测速可追加到同一文件）
- 表头仅在文件不存在时写入一次
- CSV格式：`时间,经度(GCJ-02),纬度(GCJ-02),上行,下行,Ping`

**修改CSV格式**（第32行）：
```kotlin
out.write("时间,纬度,经度,上传,下载,延迟,网络类型\n")
```

**修改坐标精度**（第37-38行）：
```kotlin
out.write("${it.timestamp},$gcjLon,${"%.4f".format(gcjLat)},...")  // 4位小数
```

#### 生成高德地图HTML
```kotlin
fun saveAmapHtml(context: Context, list: List<NetTestResult>): String?
```
- 文件名：`netmap_时间戳.html`
- 嵌入高德地图JS API（Web版）
- 每个测速点生成一个标注Marker，显示时间和网速

**修改地图初始参数**（第84-88行）：
```kotlin
var map = new AMap.Map('container', {
    zoom: 15,                    // 缩放级别（默认13）
    center: [$centerLon, $centerLat],
    viewMode: '3D',              // 开启3D视图
    pitch: 45                    // 倾斜角度
});
```

**修改Marker样式**（第65-70行）：
```kotlin
new AMap.Marker({
    position: [$lon, $lat],
    map: map,
    title: "...",
    icon: 'https://webapi.amap.com/theme/v1.3/markers/n/mark_b.png',  // 自定义图标
    label: {content: '★', style: {fontWeight: 'bold'}}  // 添加标签
});
```

#### 发送邮件
```kotlin
fun sendEmail(context: Context, csvPath: String?, htmlPath: String?)
```
- 自动附加CSV和HTML文件
- 使用系统邮件客户端，需用户手动确认发送

---

### 定位功能：`LocationHelper.kt`

封装Google Play Services定位API，提供权限管理和位置获取。

#### PermissionHelper 对象
```kotlin
object PermissionHelper {
    fun getRequiredPermissions(): Array<String>      // 所需权限列表
    fun hasLocationPermission(context: Context): Boolean  // 检查权限
    fun registerPermissionLauncher(activity, onGranted)  // 申请权限
}
```
**权限说明**：
| 权限 | Android版本 | 用途 |
|------|-------------|------|
| `ACCESS_FINE_LOCATION` | 所有版本 | 精确定位（GPS） |
| `ACCESS_COARSE_LOCATION` | 所有版本 | 粗略定位（基站/WiFi） |
| `WRITE_EXTERNAL_STORAGE` | Android 12及以下 | 写入外部存储（旧版） |
| `READ_EXTERNAL_STORAGE` | Android 12及以下 | 读取外部存储（旧版） |
| `POST_NOTIFICATIONS` | Android 13+ | 发送通知权限 |

#### LocationHelper 类
```kotlin
class LocationHelper(private val fusedLocationClient: FusedLocationProviderClient) {
    suspend fun getCurrentLocation(context: Context): android.location.Location?
}
```
**使用方式**（需在协程中调用）：
```kotlin
val locationHelper = LocationHelper(LocationServices.getFusedLocationProviderClient(this))
val location = locationHelper.getCurrentLocation(this)  // this为Context
```
返回`null`表示：
- 未授予定位权限
- 定位服务不可用（如室内无GPS信号）
- 获取超时或失败

---

### 主界面：`MainActivity.kt`

**核心逻辑**（未展示但可推断）：
1. **权限检查**：启动时检查并申请定位权限
2. **定位获取**：调用`LocationHelper.getCurrentLocation()`
3. **网络测速**：使用SpeedTestHelper测试网速
4. **数据保存**：调用`FileHelper.saveCsv()`和`saveAmapHtml()`
5. **邮件发送**：调用`FileHelper.sendEmail()`

**修改建议**：
- 如需添加新按钮或功能，在`onCreate()`中设置点击监听器
- 如需修改测速逻辑，查看`SpeedTestHelper.kt`（本介绍不展开）

---

## 🔨 常见修改场景

### 场景1：修改应用名称和图标

**修改应用名称**：
编辑 `app/src/main/res/values/strings.xml`：
```xml
<string name="app_name">你的应用名</string>
```

**修改应用图标**：
替换 `app/src/main/res/mipmap-*/ic_launcher.png` 和 `ic_launcher_round.png`（建议尺寸：192×192, 512×512）

---

### 场景2：调整导出文件存储位置

默认使用应用私有外部存储，如需改为公共目录（如DCIM），修改 `FileHelper.kt` 第18行：

```kotlin
// 改为DCIM目录
val dir = context.getExternalFilesDir(Environment.DIRECTORY_DCIM)
```
**注意**：公共目录需要申请 `WRITE_EXTERNAL_STORAGE` 权限（Android 10以下）。

---

### 场景3：修改CSV表头和数据格式

编辑 `FileHelper.kt` 第32行（表头）和第36-39行（数据行）：

```kotlin
// 表头
out.write("时间戳,纬度,经度,上传Mbps,下载Mbps,延迟ms,网络类型\n")

// 数据行（添加网络类型字段）
out.write("${it.timestamp},$gcjLat,$gcjLon,${"%.2f".format(it.upload)},...")
```

---

### 场景4：更换地图服务（如百度地图）

1. 在HTML模板中替换JS API地址（第79行）：
```kotlin
<script src="https://api.map.baidu.com/api?v=3.0&ak=你的百度密钥"></script>
```

2. 修改地图初始化和Marker创建代码（第84-70行）：
   - 百度地图使用`BMap`对象
   - 坐标格式为`[经度, 纬度]`（与高德一致）
   - 参考百度地图JavaScript API文档

3. 坐标转换可能需要改为`BD-09`（百度坐标系），需新增转换函数。

---

### 场景5：添加数据过滤功能（如仅保存4G/5G网络）

在`FileHelper.saveCsv()`中添加条件判断：
```kotlin
list.filter { 
    // 假设NetTestResult有networkType字段（如"4G"、"5G"、"WiFi"）
    it.networkType == "4G" || it.networkType == "5G"
}.forEach { ... }
```

---

### 场景6：修改权限申请逻辑

编辑 `LocationHelper.kt` 第23-29行：
```kotlin
// 示例：仅申请精确定位，不申请存储权限（Android 10+）
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
    permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
}
```
**注意**：删除存储权限后，需确保应用不再使用公共目录存储。

---

### 场景7：自定义测速参数

如果`SpeedTestHelper.kt`中有可配置参数（如测速服务器、超时时间），直接修改对应常量：
```kotlin
companion object {
    private const val TIMEOUT_MS = 10000  // 改为30秒
    private const val TEST_SIZE_MB = 10   // 测试文件大小
}
```

---

## ⚙️ 配置文件说明

### `app/build.gradle`
```gradle
android {
    compileSdk 33  // 编译版本
    defaultConfig {
        applicationId "com.example.netgeocourier"  // 包名
        minSdk 21      // 最低支持Android 5.0
        targetSdk 33  // 目标Android 13
        versionCode 1 // 版本号（递增）
        versionName "1.0"  // 版本名称
    }
    buildTypes {
        release {
            minifyEnabled false  // 是否代码混淆
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
}

dependencies {
    implementation 'com.google.android.gms:play-services-location:21.0.1'  // 定位服务
    implementation "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3" // 协程
    // 其他依赖...
}
```
**修改建议**：
- `minSdkVersion` 不能低于21（否则定位API不可用）
- 升级依赖版本时，检查兼容性

---

### `AndroidManifest.xml`
```xml
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<!-- 其他权限根据实际需要添加 -->

<application
    android:allowBackup="true"
    android:icon="@mipmap/ic_launcher"
    android:label="@string/app_name"
    android:theme="@style/Theme.NetGeoCourier">
    <activity android:name=".MainActivity">
        <intent-filter>
            <action android:name="android.intent.action.MAIN" />
            <category android:name="android.intent.category.LAUNCHER" />
        </intent-filter>
    </activity>
</application>
```
**注意**：`FileProvider`需要在`<application>`内注册（如果使用邮件附件）：
```xml
<provider
    android:name="androidx.core.content.FileProvider"
    android:authorities="${applicationId}.fileprovider"
    android:exported="false"
    android:grantUriPermissions="true">
    <meta-data
        android:name="android.support.FILE_PROVIDER_PATHS"
        android:resource="@xml/file_paths" />
</provider>
```
对应的 `res/xml/file_paths.xml`：
```xml
<?xml version="1.0" encoding="utf-8"?>
<paths>
    <external-files-path
        name="external_files"
        path="." />
</paths>
```

---

## 🐛 调试技巧

### 1. 查看Logcat日志
- Android Studio底部 → Logcat标签
- 过滤标签：`NetGeoCourier` 或 `MainActivity`
- 常用日志级别：`Log.d()`（调试）、`Log.e()`（错误）

示例输出：
```
D/NetGeoCourier: 定位成功: lat=39.123, lon=116.456
E/NetGeoCourier: 高德地图密钥未配置
```

### 2. 检查文件存储
```bash
# 通过ADB连接设备后，查看导出文件
adb shell ls /storage/emulated/0/Android/data/com.example.netgeocourier/files/Documents/
```

### 3. 模拟器网络测速问题
- 模拟器的网络速度取决于宿主机，可能不准确
- 建议真机测试

---

## 📦 构建发布

### 生成签名APK
1. Build → Generate Signed Bundle / APK → APK
2. 选择或创建密钥库（.jks文件）
3. 配置密钥信息（密码、别名、有效期建议25年）
4. 选择release构建类型
5. 完成生成APK文件（位于 `app/release/app-release.apk`）

### 签名配置（推荐使用Gradle签名）
编辑 `app/build.gradle`：
```gradle
android {
    signingConfigs {
        release {
            storeFile file("yourkeystore.jks")
            storePassword "密码"
            keyAlias "别名"
            keyPassword "密码"
        }
    }
    buildTypes {
        release {
            signingConfig signingConfigs.release
            minifyEnabled true  // 开启混淆
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
}
```
**安全提示**：不要将keystore文件和密码提交到Git。

---

## ⚠️ 注意事项

### 高德地图API限制
- 免费版每天3000次调用（仅Web服务）
- 导出HTML时加载地图算一次调用
- 密钥泄露后立即在高德控制台重置

### 定位权限与兼容性
- Android 10+ 定位权限有"仅在使用时允许"和"始终允许"两种
- 如果应用需要后台定位，需额外申请 `ACCESS_BACKGROUND_LOCATION`
- 国内部分设备无Google Play Services，定位功能不可用（需改用高德/百度定位SDK）

### 文件存储权限变更
- Android 10 (API 29) 引入分区存储（Scoped Storage）
- 使用 `getExternalFilesDir()` 无需存储权限
- 如需访问公共目录（如Pictures），需申请权限并使用MediaStore API

---

## 🎯 下一步开发建议

1. **数据持久化**：使用Room数据库存储历史记录，支持查询和删除
2. **图表展示**：集成MPAndroidChart，显示网速变化曲线
3. **多地图支持**：根据用户选择生成百度/腾讯地图页面
4. **批量导出**：选择多个测速记录生成单个地图HTML
5. **云端同步**：接入Firebase或自建服务器，多设备数据同步
6. **实时测速**：显示实时网速波形，支持持续监控
7. **离线支持**：无网络时保存数据，待联网后导出
8. **国际化**：支持中英文切换
9. **暗黑模式**：适配系统深色主题
10. **小部件**：添加桌面小部件，一键启动测速

---

## 📚 学习资源

- [Kotlin官方文档](https://kotlinlang.org/docs/home.html)
- [Android开发者指南](https://developer.android.com/guide)
- [Google Play Services定位](https://developers.google.com/location-context/fused-location-provider)
- [高德地图JavaScript API](https://lbs.amap.com/api/javascript-api/summary)
- [Android文件存储](https://developer.android.com/training/data-storage)

---

**祝开发顺利！** 🎉

如有问题，建议先查阅本项目的注释代码，再参考官方文档。项目结构简单，逻辑清晰，非常适合Android开发入门学习。
