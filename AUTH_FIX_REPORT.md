# NetGeoCourier 认证修复与使用报告

## 一、问题定位

本次排查确认了两个核心问题：

1. 后端其实已经提供了注册、登录接口，但 Android 客户端没有给普通用户提供入口。
2. App 仍然沿用“开发调试模式”的思路，要求手动保存 token，所以一进入相关页面会看到很多面向开发者的提示，而不是正常用户能直接使用的流程。
3. 即使已经登录，原来的 App 也不会把测速结果上传到后端，导致评估页拿不到当前用户的记录，功能链路不完整。

## 二、根因分析

### 1. 后端侧

- 已存在 `POST /api/v1/auth/register`
- 已存在 `POST /api/v1/auth/login`
- 已存在受保护的 `GET /api/v1/evaluation`
- 已存在受保护的 `POST /api/v1/records`

也就是说，后端“能力”基本已经有了，但返回结构和前端接入体验还不够友好：

- 错误信息只放在 `error.message` 中，通用客户端不容易直接复用。
- 登录/注册成功消息过于笼统。
- 无记录时的提示更像接口说明，而不是面向最终用户的反馈。

### 2. Android 客户端侧

- 没有登录/注册页面。
- 没有账户会话展示。
- 没有退出登录入口。
- 评估页依赖本地 token，但 token 需要开发者手动注入。
- 测速结果只保存在本地，没有自动同步到后端记录接口。

## 三、已完成的修复

### 后端修复

1. 给错误响应增加了顶层 `message` 字段，方便客户端统一读取错误信息。
2. 将登录/注册成功提示改为明确消息：
   - `Registration successful.`
   - `Login successful.`
3. 优化了无记录时的评估提示，使其与新的 App 流程一致。

### Android 修复

1. 新增独立的登录/注册页面 `AuthScreen`。
2. 支持用户直接在 App 内注册账号，不再依赖开发者手动创建用户再发 token。
3. 登录成功后自动保存：
   - access token
   - 用户 id
   - username
   - display name
   - student id
4. 主测速页新增账户状态卡片：
   - 未登录时可直接进入登录/注册
   - 已登录时展示当前账号并支持退出登录
5. 评估页新增会话状态展示与登录入口，不再显示开发者式 token 操作提示。
6. 测速完成后，如果用户已登录，App 会自动把测速记录同步到后端 `/api/v1/records`。
7. 优化了客户端错误解析逻辑，后端返回的错误原因可以更直接展示给用户。

## 四、现在的使用方式

### 启动后端

在目录：

`E:\projectstu_zly\NetGeoCourier2\campus-net-backend\campus-net-backend`

运行：

```powershell
npm.cmd install
npm.cmd run dev
```

### App 使用流程

1. 打开 App。
2. 在主页点击 `Login / Register`。
3. 新用户可直接注册；老用户可直接登录。
4. 登录成功后返回测速页。
5. 每次测速完成后，记录会自动同步到后端。
6. 再进入评估页时，系统会基于当前登录用户的已同步记录生成个人评估。

## 五、改动文件

### 后端

- `E:\projectstu_zly\NetGeoCourier2\campus-net-backend\campus-net-backend\src\controllers\auth.controller.js`
- `E:\projectstu_zly\NetGeoCourier2\campus-net-backend\campus-net-backend\src\services\evaluation.service.js`
- `E:\projectstu_zly\NetGeoCourier2\campus-net-backend\campus-net-backend\src\utils\responses.js`

### Android

- `E:\projectstu_zly\NetGeoCourier2\app\src\main\java\com\example\netgeocourier\MainActivity.kt`
- `E:\projectstu_zly\NetGeoCourier2\app\src\main\java\com\example\netgeocourier\helper\AuthTokenStore.kt`
- `E:\projectstu_zly\NetGeoCourier2\app\src\main\java\com\example\netgeocourier\screen\AuthScreen.kt`
- `E:\projectstu_zly\NetGeoCourier2\app\src\main\java\com\example\netgeocourier\screen\EvaluationScreen.kt`
- `E:\projectstu_zly\NetGeoCourier2\app\src\main\java\com\example\netgeocourier\screen\NetTestScreen.kt`
- `E:\projectstu_zly\NetGeoCourier2\app\src\main\java\com\example\netgeocourier\viewmodel\NetTestViewModel.kt`
- `E:\projectstu_zly\NetGeoCourier2\app\src\main\java\com\example\netgeocourier\data\AuthModels.kt`
- `E:\projectstu_zly\NetGeoCourier2\app\src\main\java\com\example\netgeocourier\data\RecordModels.kt`
- `E:\projectstu_zly\NetGeoCourier2\app\src\main\java\com\example\netgeocourier\network\ApiClient.kt`
- `E:\projectstu_zly\NetGeoCourier2\app\src\main\java\com\example\netgeocourier\network\ApiErrorParser.kt`
- `E:\projectstu_zly\NetGeoCourier2\app\src\main\java\com\example\netgeocourier\network\AuthApiService.kt`
- `E:\projectstu_zly\NetGeoCourier2\app\src\main\java\com\example\netgeocourier\network\AuthRepository.kt`
- `E:\projectstu_zly\NetGeoCourier2\app\src\main\java\com\example\netgeocourier\network\RecordApiService.kt`
- `E:\projectstu_zly\NetGeoCourier2\app\src\main\java\com\example\netgeocourier\network\RecordRepository.kt`
- `E:\projectstu_zly\NetGeoCourier2\app\src\main\java\com\example\netgeocourier\network\EvaluationRepository.kt`

## 六、验证结果

已完成验证：

- 后端依赖安装：通过
- 后端语法检查：通过
- 后端 smoke test：通过

受本机环境限制暂未完成：

- Android Kotlin 编译校验未能执行，因为当前机器没有配置 `JAVA_HOME`，且 `java` 命令不可用。

## 七、补充说明

- 这次新增的 App 认证 UI 文案为了避免当前工程中的编码问题，先使用了英文文本。
- 如果你需要，我下一步可以继续把新增登录/注册流程完整本地化成中文，并顺手清理现有字符串资源里的编码问题。
