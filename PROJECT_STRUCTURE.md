# NetGeoCourier 项目结构说明

## 项目概述

**NetGeoCourier**（网络地理快递员）是一个 Android 应用，用于在地理位置标记的情况下进行网络速度测试。应用可以测量上行/下行速率、Ping延时，并记录测试地点的经纬度坐标，支持数据导出为 CSV 文件和高德地图 HTML。

---

## 项目结构





```
NetGeoCourier2/
├── app/                          # 主应用模块
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/netgeocourier/
│   │   │   │   ├── MainActivity.kt          # 主入口Activity
│   │   │   │   ├── data/
│   │   │   │   │   └── NetTestResult.kt     # 测速结果数据模型
│   │   │   │   ├── helper/
│   │   │   │   │   ├── CoordTransform.kt    # 坐标转换工具(WGS84→GCJ-02)
│   │   │   │   │   ├── FileHelper.kt       # 文件操作工具(CSV/HTML/邮件)
│   │   │   │   │   ├── LocationHelper.kt   # 定位和权限管理
│   │   │   │   │   └── SpeedTestHelper.kt  # 速度测试工具
│   │   │   │   ├── screen/
│   │   │   │   │   └── NetTestScreen.kt     # 测速界面UI
│   │   │   │   └── ui/theme/               # Compose主题配置
│   │   │   │       ├── Color.kt            # 颜色定义
│   │   │   │       ├── Theme.kt            # 主题配置
│   │   │   │       └── Type.kt             # 字体排版配置
│   │   │   ├── res/                        # 资源文件
│   │   │   │   ├── drawable/              # 图标资源
│   │   │   │   ├── mipmap-*/              # 应用图标
│   │   │   │   ├── values/                # 字符串/主题资源
│   │   │   │   ├── values-en/             # 英文字符串
│   │   │   │   └── xml/                   # XML配置
│   │   │   └── AndroidManifest.xml        # 应用清单文件
│   │   ├── test/                           # 单元测试
│   │   └── androidTest/                    # 仪器测试
│   ├── build.gradle.kts                    # 应用级构建配置
│   ├── proguard-rules.pro                  # ProGuard混淆规则
│   └── .gitignore                          # Git忽略配置
├── gradle/                                 # Gradle相关文件
│   ├── wrapper/                           # Gradle Wrapper
│   └── libs.versions.toml                  # 版本目录
├── build.gradle.kts                        # 根级构建配置
├── settings.gradle.kts                     # 项目设置
├── gradle.properties                       # Gradle属性配置
├── local.properties                        # 本地配置(SDK路径等)
└── .gitignore                              # 全局Git忽略配置
```

---

## 核心文件说明

### 数据模型

| 文件 | 说明 |
|------|------|
| `data/NetTestResult.kt` | 测速结果数据类，包含时间戳、经纬度、上传/下载速率、Ping值 |

### 工具类 (helper/)

| 文件 | 说明 |
|------|------|
| `CoordTransform.kt` | 坐标系转换工具，将 GPS 原始坐标(WGS84)转换为国内地图使用的 GCJ-02 坐标系 |
| `FileHelper.kt` | 文件操作工具类，提供 CSV 保存、高德地图 HTML 生成、邮件发送功能 |
| `LocationHelper.kt` | 定位帮助类，包含 `PermissionHelper` 权限管理助手和 `LocationHelper` 定位服务 |
| `SpeedTestHelper.kt` | 网络测速工具，使用 Cloudflare Speed Test API 测量上传/下载速度和 Ping |

### 界面 (screen/)

| 文件 | 说明 |
|------|------|
| `NetTestScreen.kt` | 测速界面，使用 Jetpack Compose 编写，包含手动/自动测试按钮、结果展示、历史记录 |

### 主题 (ui/theme/)

| 文件 | 说明 |
|------|------|
| `Color.kt` | Material3 颜色主题定义 |
| `Theme.kt` | Compose 主题配置，支持动态颜色和深色模式 |
| `Type.kt` | 字体排版样式定义 |

### 配置文件

| 文件 | 说明 |
|------|------|
| `AndroidManifest.xml` | 应用清单，声明权限(网络、定位、存储)、FileProvider、MainActivity |
| `app/build.gradle.kts` | 应用构建配置，声明依赖库、SDK 版本、编译选项 |
| `settings.gradle.kts` | 项目设置，配置仓库和模块包含 |
| `build.gradle.kts` | 根级构建配置，声明插件 |
| `gradle/libs.versions.toml` | 依赖版本集中管理 |

---

## 权限说明

- `INTERNET` - 允许网络请求
- `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION` - 获取定位信息
- `WRITE_EXTERNAL_STORAGE` / `READ_EXTERNAL_STORAGE` - 文件读写(Android 12以下)
- `POST_NOTIFICATIONS` - 通知权限(Android 13以上)

---

## 主要功能

1. **网络测速** - 测量上行速率、下行速率、Ping延时
2. **地理位置标记** - 获取测试地点的 GPS 坐标并转换坐标系
3. **手动/自动测试** - 支持单次测试和间隔5秒的连续自动测试
4. **数据导出** - 生成 CSV 表格文件和带标记点的高德地图 HTML
5. **邮件分享** - 将测试数据通过邮件发送

---

## 技术栈

- **语言**: Kotlin
- **UI框架**: Jetpack Compose + Material3
- **架构**: 单Activity架构
- **定位**: Google Play Services Location
- **测试API**: Cloudflare Speed Test
- **地图API**: 高德地图 Web API
- **最小SDK**: Android 8.0 (API 26)
- **目标SDK**: Android 15 (API 35)
