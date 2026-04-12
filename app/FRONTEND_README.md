# 📱 NetGeo Courier Android 客户端

基于 **Kotlin + Jetpack Compose** 构建的校园网络测速 Android 应用，支持高德地图定位、网络速度测试、数据同步与网络质量评估。

---

## 📋 项目概述

NetGeo Courier 是一款面向校园用户的网络测速与评估工具。用户可通过应用完成网络速度测试，自动记录测试结果并与云端同步，获取个性化的网络优化建议。

### 核心功能
- 🚀 **网络测速**：实时测试下载/上传速度与延迟
- 📍 **位置采集**：集成高德地图 SDK 获取精确地理位置
- ☁️ **数据同步**：自动上传测速记录到后端服务器
- 📊 **网络评估**：基于历史数据生成网络质量报告
- 🔐 **用户系统**：注册、登录、Token 自动刷新
- 🗺️ **历史记录**：查看过往测速结果与趋势

---

## 🛠️ 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Kotlin | 1.9+ | 主要编程语言 |
| Jetpack Compose | 1.6+ | UI 框架 |
| Android Gradle Plugin | 8.5+ | 构建系统 |
| Retrofit2 | 2.11.0 | HTTP 客户端 |
| OkHttp3 | 4.12.0 | 网络请求引擎 |
| Gson | 2.11.0 (via converter) | JSON 序列化 |
| Coroutines | 1.8+ | 异步任务处理 |
| ViewModel | 2.8.2 | UI 状态管理 |
| Accompanist | - | Compose 扩展库 |
|高德地图 SDK | - | 定位服务 |

### 架构模式
- **MVVM**（Model-View-ViewModel）
- **Repository 模式**：统一数据源（本地 + 网络）
- **单向数据流**：State → UI → Event → ViewModel

---

## 📁 项目结构

```
app/src/main/java/com/example/netgeocourier/
├── data/
│   ├── AuthModels.kt           # 认证相关数据模型
│   ├── RecordModels.kt         # 测速记录数据模型
│   ├── EvaluationModels.kt     # 评估数据模型
│   ├── NetTestResult.kt        # 测速结果模型
│   └── ApiEnvelope.kt          # API 统一响应包装器
├── network/
│   ├── ApiClient.kt            # Retrofit 客户端构建器
│   ├── AuthApiService.kt       # 认证接口定义
│   ├── RecordApiService.kt     # 记录接口定义
│   ├── EvaluationApiService.kt # 评估接口定义
│   ├── AuthRepository.kt       # 认证数据仓库
│   ├── RecordRepository.kt     # 记录数据仓库
│   ├── EvaluationRepository.kt # 评估数据仓库
│   ├── AuthInterceptor.kt      # 认证 Token 拦截器
│   └── ApiErrorParser.kt       # API 错误解析器
├── helper/
│   ├── AuthTokenStore.kt       # Token 本地存储（SharedPreferences）
│   ├── ApiConfigStore.kt       # API 地址配置存储
│   ├── FileHelper.kt           # 文件读写工具（HTML 报告）
│   ├── LocationHelper.kt       # 高德定位封装
│   ├── SpeedTestHelper.kt      # 网络测速核心逻辑
│   └── CoordTransform.kt       # 坐标转换（GCJ-02 ↔ WGS-84）
├── screen/
│   ├── SplashActivity.kt       # 启动页
│   ├── AuthScreen.kt           # 认证页面（登录/注册）
│   ├── NetTestScreen.kt        # 测速主页面
│   ├── HistoryScreen.kt        # 历史记录页面
│   └── EvaluationScreen.kt     # 网络评估页面
├── viewmodel/
│   └── NetTestViewModel.kt     # 测速页面 ViewModel
├── ui/
│   └── theme/
│       ├── Color.kt            # 主题颜色
│       ├── Theme.kt            # Compose 主题
│       └── Type.kt             # 字体排版
├── MainActivity.kt             # 主 Activity（导航容器）
└── BuildConfig.java           # 自动生成的构建配置
```

---

## 🚀 快速开始

### 环境要求

- **Android Studio**：2023.1.1+ (Electric Eel 或更高版本)
- **JDK**：21
- **Android SDK**：API 26 (Android 8.0) 及以上
- **设备/模拟器**：API 26+，建议 API 35

### 1. 克隆项目

```bash
git clone https://github.com/your-repo/NetGeoCourier2.git
cd NetGeoCourier2
```

### 2. 配置 API 地址

编辑 `local.properties` 文件（位于项目根目录）：

```properties
# 高德地图 Web 服务 Key（用于 HTML 报告）
AMAP_WEB_KEY=your-amap-web-key

# 高德地图 Android SDK Key
AMAP_ANDROID_KEY=your-amap-android-key

# 后端 API 地址（生产环境使用 FRP 域名）
API_BASE_URL=http://www-speedtest-i.hcb8c115c.nyat.app:11152/
```

⚠️ **重要提示**：
- 开发阶段可使用 `http://10.0.2.2:3000/`（Android 模拟器访问宿主机）
- 真机调试使用电脑局域网 IP（如 `http://192.168.1.100:3000/`）
- 生产环境必须使用 **HTTP**（TCP 隧道不支持 HTTPS 加密）

### 3. 同步 Gradle 依赖

打开项目后，Android Studio 会自动同步。如失败，手动点击：
```
File → Sync Project with Gradle Files
```

### 4. 配置高德地图密钥（可选）

如果不需要地图功能，可跳过此步。如需启用：

1. 注册高德开放平台账号
2. 创建应用，获取 **Web Service Key** 和 **Android SDK Key**
3. 在 `local.properties` 中配置（见上一步）
4. 在 `app/src/main/AndroidManifest.xml` 中配置密钥：
   ```xml
   <meta-data
       android:name="com.amap.api.v2.apikey"
       android:value="${AMAP_ANDROID_KEY}" />
   ```

### 5. 构建与运行

```bash
# 调试版本
./gradlew assembleDebug

# 释放版本
./gradlew assembleRelease
```

或使用 Android Studio 工具栏点击 **Run** 按钮。

---

## ⚙️ 配置详解

### `local.properties` 参数

| 参数 | 说明 | 示例值 |
|------|------|--------|
| `sdk.dir` | Android SDK 路径（自动生成） | `C:\Users\...\Sdk` |
| `AMAP_WEB_KEY` | 高德 Web 服务 Key（用于 HTML 报告地图） | `0fe0d875041a90a5686e62f931c5f991` |
| `AMAP_ANDROID_KEY` | 高德 Android SDK Key | `6565f23f22c878d0f4409f15df6fac71` |
| `API_BASE_URL` | 后端 API 基础 URL | `http://www-speedtest-i.hcb8c115c.nyat.app:11152/` |

### 构建类型配置

**调试构建** (`debug`)：
- `applicationIdSuffix`：无
- `minifyEnabled`：`false`
- 日志详细，支持调试器连接

**发布构建** (`release`)：
- 签名配置使用 `debug.keystore`（当前配置）
- 建议配置正式签名与代码混淆

---

## 🌐 网络通信

### API 基础信息

- **Base URL**：`BuildConfig.API_BASE_URL`（从 `local.properties` 注入）
- **协议版本**：HTTP 1.1（推荐内网穿透使用 HTTP，避免 HTTPS 证书问题）
- **数据格式**：JSON
- **超时设置**：连接/读/写 各 15 秒

### 统一响应包装器

所有 API 响应被 `ApiEnvelope<T>` 包装：

```kotlin
data class ApiEnvelope<T>(
    val success: Boolean,
    val message: String,
    val data: T?
)
```

**成功示例**：
```json
{
  "success": true,
  "message": "登录成功",
  "data": { /* 业务数据 */ }
}
```

**失败示例**：
```json
{
  "success": false,
  "message": "用户名或密码错误",
  "error": {
    "code": "AUTH_INVALID_CREDENTIALS",
    "message": "用户名或密码错误",
    "requestId": "uuid"
  }
}
```

### 认证机制

1. **登录/注册** → 获取 `accessToken`
2. **后续请求** → 通过 `AuthInterceptor` 自动添加 `Authorization: Bearer <token>` 请求头
3. **Token 存储**：`AuthTokenStore` 使用 `SharedPreferences` 持久化
4. **401 处理**：拦截器检测到 401 响应时，自动清除本地 Token 并跳转登录页

---

## 🎯 核心功能模块

### 1. 用户认证

**流程**：
```
注册/登录 → 保存 Token → 获取用户信息 → 进入主页
```

**关键类**：
- `AuthApiService.kt`：定义 `login()`、`register()` 接口
- `AuthRepository.kt`：处理认证逻辑与错误转换
- `AuthScreen.kt`：UI 页面，支持登录/注册表单切换

**验证规则**：
- 用户名：3-64 字符，允许 `a-zA-Z0-9_.@-`
- 密码：6-128 字符

---

### 2. 网络测速

**测速指标**：
- **下载速度**（Download Mbps）
- **上传速度**（Upload Mbps）
- **延迟**（Ping ms）

**实现原理**：
- 使用 `SpeedTestHelper` 下载/上传测试文件
- 计算单位时间传输字节数转换为 Mbps
- 多次测量取平均值减少误差

**关键代码**：
```kotlin
// SpeedTestHelper.kt
suspend fun runSpeedTest(onProgress: (String) -> Unit): NetTestResult
```

**测速流程**：
1. 选择测试服务器（默认使用内置测试 URL）
2. 下载测试 → 计算下载速度
3. 上传测试 → 计算上传速度
4. Ping 测试 → 测量延迟
5. 组合结果并附加地理位置

---

### 3. 位置服务

**集成高德地图 SDK**：
- 单次定位：`LocationHelper.getSingleLocation()`
- 持续定位：`LocationHelper.startLocationUpdates()`
- 坐标转换：`CoordTransform`（WGS-84 ↔ GCJ-02 火星坐标）

**权限要求**：
```xml
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
```

**运行时权限请求**：`NetTestScreen` 中动态申请。

---

### 4. 数据同步

**同步策略**：
- **自动同步**：测速完成后自动上传
- **批量上传**：支持单条或批量（最多 100 条/次）
- **本地缓存**：未同步结果暂存于内存，失败可重试

**关键类**：
- `RecordRepository.kt`：提供 `uploadResult()` 和 `syncResults()` 方法
- `RecordApiService.kt`：定义 `uploadRecord()` 与 `uploadBatch()` 接口

**去重机制**：后端根据用户+时间+指标+位置生成签名，自动去重。

---

### 5. 网络评估

**评估维度**：
- 下载速度平均值
- 延迟平均值
- 信号强度（RSSI）
- 信噪比（SNR）

**评分算法**（后端实现）：
- 下载：`score = clamp(downloadAvg / 100 * 100, 0, 100)`
- 延迟：`score = clamp((200 - pingAvg) / 180 * 100, 0, 100)`
- RSSI：`score = clamp((rssi + 90) / 40 * 100, 0, 100)`
- SNR：`score = clamp((snr - 10) / 30 * 100, 0, 100)`

**评级标准**：
- 优秀：≥ 85
- 良好：70 - 84
- 一般：55 - 69
- 较差：< 55

**前端展示**：`EvaluationScreen.kt` 显示分数、评级、建议与历史趋势图表。

---

## 🗂️ 数据模型

### 核心模型

| 模型 | 用途 | 关键字段 |
|------|------|----------|
| `NetTestResult` | 测速结果 | `timestamp`, `download`, `upload`, `ping`, `latitude`, `longitude` |
| `AuthUser` | 用户信息 | `id`, `username`, `displayName`, `studentId` |
| `AuthPayload` | 认证响应 | `accessToken`, `tokenType`, `expiresIn`, `user` |
| `RecordUploadRequest` | 上传请求 | `capturedAt`, `metrics`, `location`, `remark` |
| `RecordMetrics` | 测速指标 | `downloadMbps`, `uploadMbps`, `pingMs` |
| `RecordLocation` | 位置信息 | `latitude`, `longitude`, `source` |
| `EvaluationData` | 评估结果 | `score`, `level`, `suggestions`, `metrics`, `recordCount` |

---

## 🎨 UI 主题

### 颜色方案（`Color.kt`）

```kotlin
val Primary = Color(0xFF1976D2)      // 主色调：蓝色
val Secondary = Color(0xFF03A9F4)    // 次色调：浅蓝
val Background = Color(0xFFF5F5F5)   // 背景灰
val Surface = Color(0xFFFFFFFF)      // 表面白
val Success = Color(0xFF4CAF50)      // 成功绿
val Warning = Color(0xFFFFC107)      // 警告黄
val Error = Color(0xFFF44336)        // 错误红
```

### 排版（`Type.kt`）

使用 Material Typography  scale：
- `H1`：34sp，页面大标题
- `H2`：24sp，模块标题
- `Body1`：16sp，正文
- `Caption`：12sp，辅助说明

---

## 🔧 工具类

### `ApiConfigStore.kt`

**功能**：持久化存储 API Base URL
- 优先读取 SharedPreferences
- 回退到 `BuildConfig.API_BASE_URL`
- 自动标准化 URL（补全 `http://` 与末尾 `/`）

### `AuthTokenStore.kt`

**功能**：Token 的读写与清除
```kotlin
fun saveAccessToken(context: Context, token: String)
fun getAccessToken(context: Context): String?
fun clearToken(context: Context)
```

### `FileHelper.kt`

**功能**：
- 生成测速结果 HTML 报告（嵌入高德地图）
- 保存报告到外部存储
- 通过 `Intent.ACTION_VIEW` 分享或打开

### `SpeedTestHelper.kt`

**功能**：执行实际测速
- 下载测试：分块读取测试 URL
- 上传测试：向测试服务器 POST 随机数据
- Ping 测试：`InetAddress.isReachable(timeout)`

**测试服务器 URL**：硬编码为 `http://www.speedtest.net/speedtest-config.php`（可修改）

---

## 🐛 调试与日志

### 启用调试日志

在 `ApiClient.kt` 中添加 OkHttp 日志拦截器：
```kotlin
val logging = HttpLoggingInterceptor().apply {
    level = HttpLoggingInterceptor.Level.BODY
}
clientBuilder.addInterceptor(logging)
```

### 查看网络请求

使用 **Android Studio → View → Tool Windows → App Inspection → Network Inspector**

### 常见错误码

| 错误码 | 说明 | 处理方式 |
|--------|------|----------|
| `400` | 请求参数错误 | 检查请求体格式 |
| `401` | Token 失效 | 清除本地 Token，重新登录 |
| `404` | 接口不存在 | 检查 API Base URL |
| `500` | 服务器内部错误 | 查看后端日志 |
| `-1` | 网络连接失败 | 检查网络与 FRP 隧道 |

---

## 📦 发布构建

### 生成签名密钥

```bash
keytool -genkey -v \
  -keystore my-release-key.jks \
  -keyalg RSA -keysize 2048 \
  -validity 10000 \
  -alias my-key-alias
```

### 配置 `build.gradle.kts`

```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file("my-release-key.jks")
            storePassword = "your-store-password"
            keyAlias = "my-key-alias"
            keyPassword = "your-key-password"
        }
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}
```

### 生成 APK/AAB

```bash
# APK
./gradlew assembleRelease

# AAB (Google Play)
./gradlew bundleRelease
```

输出位置：
- APK：`app/build/outputs/apk/release/app-release.apk`
- AAB：`app/build/outputs/bundle/release/app-release.aab`

---

## 🔐 安全建议

### 1. 启用 HTTPS（生产环境）

- 后端配置 SSL 证书（Nginx 反向代理或 Node.js 直接支持）
- Android 添加网络安全配置（`network_security_config.xml`）允许 HTTPS

### 2. 加固 Token 存储

当前使用 `SharedPreferences`（明文）。建议：
- 使用 `EncryptedSharedPreferences`（AndroidX Security）
- 或集成 `BiometricPrompt` 生物认证解锁

### 3. 代码混淆

在 `proguard-rules.pro` 添加：
```
-keep class com.example.netgeocourier.data.** { *; }
-keep class com.example.netgeocourier.network.** { *; }
-keep class retrofit2.** { *; }
-keep class okhttp3.** { *; }
```

---

## 🧪 测试

### 单元测试

```bash
./gradlew testDebugUnitTest
```

测试文件位置：
- `app/src/test/java/`：ExampleUnitTest.kt

### 集成测试

```bash
./gradlew connectedDebugAndroidTest
```

测试文件位置：
- `app/src/androidTest/java/`：ExampleInstrumentedTest.kt

---

## 🚀 性能优化

### 1. 图片加载

当前未使用图片加载库。如需显示图表，建议集成 **Coil** 或 **Glide**。

### 2. 懒加载列表

历史记录列表使用 `LazyColumn` 已有懒加载，确保大数据不卡顿。

### 3. 网络缓存

OkHttp 默认无缓存。如需缓存 GET 请求（如评估结果），添加 `Cache` 拦截器。

---

## 📱 已知限制

| 限制 | 说明 | 解决方案 |
|------|------|----------|
| 测速服务器固定 | 使用第三方公共测速服务器 | 后端可配置多个测速节点 |
| 无离线模式 | 需要网络连接才能测速 | 本地暂存，联网后同步 |
| 无推送通知 | 不推送评估报告 | 集成 Firebase Cloud Messaging |
| 仅中文界面 | 仅支持简体中文 | 添加 `values-zh-rCN` 外的资源目录 |

---

## 🛠️ 故障排除

### 1. 编译错误：`toUploadRequest is a member and an extension`

**原因**：扩展函数与成员函数冲突。  
**已修复**：`RecordRepository.kt` 中已将扩展函数改为普通方法 `convertToUploadRequest()`。

### 2. 连接失败：`connectex: No connection could be made`

**可能原因**：
- 后端未启动 → 启动 `node src/server.js`
- FRP 隧道未连接 → 检查 FRP 客户端日志
- API 地址错误 → 检查 `local.properties` 的 `API_BASE_URL`
- 协议不匹配 → **必须用 HTTP**，不要用 HTTPS

### 3. 注册失败：`注册失败，请稍后重试`

**排查步骤**：
1. 查看后端控制台错误堆栈
2. 检查 `data/local-db.json` 文件是否可写
3. 确保用户名符合规则（3-64 字符，仅限字母数字_ . @ -）
4. 确保密码 6-128 字符

### 4. 高德地图不显示

**检查**：
- `AMAP_WEB_KEY` 和 `AMAP_ANDROID_KEY` 是否正确配置
- `AndroidManifest.xml` 中是否声明 Key
- 设备是否安装高德地图 APP（可选）

---

## 📞 技术支持

- **项目主页**: https://github.com/your-repo/NetGeoCourier2
- **问题反馈**: https://github.com/your-repo/NetGeoCourier2/issues
- **邮箱**: support@example.com

---

## 📄 许可证

MIT License - 详见 LICENSE 文件

---

**最后更新**: 2025-06-04  
**维护者**: @kevinmou
