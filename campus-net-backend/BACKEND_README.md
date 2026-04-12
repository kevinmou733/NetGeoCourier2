# 📡 NetGeo Courier 后端服务

基于 Express.js 构建的校园网络测速数据采集与评估 API 服务。

---

## 📋 项目概述

NetGeo Courier 后端提供用户认证、网络测速记录上传、以及基于历史数据的网络质量评估功能。采用轻量级 JSON 文件数据库存储，无需额外数据库依赖，开箱即用。

### 核心功能
- 🔐 JWT 用户认证（注册/登录/注销）
- 📊 网络测速数据上传（单条/批量）
- 📈 网络质量智能评估与建议
- 📍 地理位置信息记录（可选）

---

## 🛠️ 技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Node.js | ≥18 | 运行时环境 |
| Express | 4.19.2 | Web 框架 |
| dotenv | 16.4.7 | 环境变量管理 |
| CORS | 2.8.5 | 跨域请求处理 |

### 关键特性
- ✅ 零数据库依赖（JSON 文件存储）
- ✅ RESTful API 设计（v1 版本）
- ✅ JWT 无状态认证
- ✅ 请求 ID 追踪
- ✅ 统一错误处理与响应格式
- ✅ 数据批量操作优化

---

## 📁 项目结构

```
campus-net-backend/
└── campus-net-backend/
    ├── src/
    │   ├── app.js                    # 应用入口与中间件配置
    │   ├── server.js                 # HTTP 服务器启动
    │   ├── config/
    │   │   └── index.js              # 配置管理（端口、JWT、数据文件路径等）
    │   ├── routes/
    │   │   ├── auth.routes.js        # 认证路由（/api/v1/auth）
    │   │   ├── records.routes.js     # 测速记录路由（/api/v1/records）
    │   │   ├── evaluation.routes.js  # 评估路由（/api/v1/evaluation）
    │   │   └── health.routes.js      # 健康检查路由（/api/v1/health）
    │   ├── controllers/
    │   │   ├── auth.controller.js    # 认证业务逻辑
    │   │   ├── records.controller.js # 记录管理逻辑
    │   │   └── evaluation.controller.js # 评估逻辑
    │   ├── services/
    │   │   ├── auth.service.js       # 用户注册/登录服务
    │   │   ├── token.service.js      # JWT 令牌生成与验证
    │   │   └── evaluation.service.js # 网络评估算法服务
    │   ├── repositories/
    │   │   ├── userRepository.js     # 用户数据访问层
    │   │   ├── recordRepository.js   # 测速记录数据访问层
    │   │   └── jsonDatabase.js       # JSON 文件数据库引擎
    │   ├── middleware/
    │   │   ├── requestId.js          # 请求 ID 生成中间件
    │   │   ├── authenticate.js       # JWT 认证中间件
    │   │   └── errorHandler.js       # 全局错误处理中间件
    │   └── utils/
    │       ├── responses.js          # 统一响应格式化
    │       ├── validators.js         # 请求参数验证
    │       ├── httpErrors.js         # HTTP 错误工厂
    │       ├── asyncHandler.js       # Promise 错误捕获
    │       └── password.js           # 密码哈希工具
    ├── data/
    │   └── local-db.json             # JSON 数据库文件（自动生成）
    ├── .env                          # 环境变量配置（可选）
    ├── .env.example                  # 环境变量示例
    └── package.json
```

**架构模式**：分层架构（Routes → Controllers → Services → Repositories）

---

## 🚀 快速开始（本地开发）

### 环境要求

- Node.js ≥ 18
- npm 或 yarn

### 安装依赖

```bash
cd campus-net-backend/campus-net-backend
npm install
```

### 本地运行

#### 开发模式（带热重载）
```bash
npm run dev
```

#### 生产模式
```bash
npm start
```

**启动成功输出**：
```
Campus network backend listening on http://127.0.0.1:3000
Data file: D:\NetGeoCourier2\campus-net-backend\campus-net-backend\data\local-db.json
```

---

## ⚙️ 配置说明

### 配置文件路径

**方式1：`.env` 文件**（推荐）
在项目根目录创建 `.env` 文件：
```bash
cd campus-net-backend/campus-net-backend
touch .env
```

**方式2：环境变量**
直接在操作系统中设置环境变量。

### 配置项详解

| 变量名 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `PORT` | 数字 | `3000` | 服务监听端口 |
| `HOST` | 字符串 | `127.0.0.1` | 监听地址（`0.0.0.0` 监听所有网卡） |
| `JWT_SECRET` | 字符串 | `change-me-local-dev-secret` | **生产环境必须修改！** JWT 签名密钥 |
| `JWT_EXPIRES_IN` | 字符串 | `7d` | Token 有效期（如：`1h`、`7d`、`30d`） |
| `DATA_FILE` | 字符串 | `./data/local-db.json` | JSON 数据库文件路径 |
| `PASSWORD_MIN_LENGTH` | 数字 | `6` | 用户密码最小长度 |
| `ALLOWED_ORIGINS` | 字符串 | 空 | 允许的 CORS 源，逗号分隔（如：`http://localhost:5173,https://example.com`） |
| `NODE_ENV` | 字符串 | `development` | 运行环境（`production` 会禁用详细错误信息） |

### 示例配置（生产环境）

```env
PORT=3000
HOST=0.0.0.0
JWT_SECRET=your-super-secret-jwt-key-change-this
JWT_EXPIRES_IN=30d
DATA_FILE=/data/campus-db.json
PASSWORD_MIN_LENGTH=8
ALLOWED_ORIGINS=https://your-app-domain.com
NODE_ENV=production
```

---

## 🔌 API 接口文档

### 基础信息

- **Base URL**: `http://your-domain.com/api/v1`
- **Content-Type**: `application/json`
- **认证方式**: Bearer Token（JWT）

### 统一响应格式

所有 API 返回统一的 `ApiEnvelope` 结构：

```json
// 成功响应
{
  "success": true,
  "message": "操作成功",
  "data": {
    // 具体数据
  }
}

// 失败响应
{
  "success": false,
  "message": "错误描述",
  "error": {
    "code": "ERROR_CODE",
    "message": "详细错误信息",
    "requestId": "uuid-string"
  }
}
```

---

### 📝 认证接口

#### 1. 用户注册

```http
POST /auth/register
```

**请求示例**：
```json
{
  "username": "student001",
  "password": "mypassword123",
  "displayName": "张三",
  "studentId": "2024001"
}
```

**字段说明**：
- `username` (必填)：3-64 字符，仅支持字母、数字、`_`、`.`、`@`、`-`
- `password` (必填)：6-128 字符
- `displayName` (可选)：最多 50 字符，默认同 username
- `studentId` (可选)：最多 32 字符

**成功响应**：
```json
{
  "success": true,
  "message": "注册成功。",
  "data": {
    "tokenType": "Bearer",
    "accessToken": "eyJhbGciOiJIUzI1NiIs...",
    "expiresIn": 604800,
    "user": {
      "id": "uuid-string",
      "username": "student001",
      "displayName": "张三",
      "studentId": "2024001",
      "createdAt": "2025-06-04T10:00:00.000Z",
      "updatedAt": "2025-06-04T10:00:00.000Z"
    }
  }
}
```

#### 2. 用户登录

```http
POST /auth/login
```

**请求示例**：
```json
{
  "username": "student001",
  "password": "mypassword123"
}
```

**响应**：同注册接口，返回 `AuthPayload`。

#### 3. 获取当前用户信息（需认证）

```http
GET /auth/me
Authorization: Bearer <accessToken>
```

**响应**：
```json
{
  "success": true,
  "message": "ok",
  "data": {
    "user": {
      "id": "uuid-string",
      "username": "student001",
      "displayName": "张三",
      "studentId": "2024001",
      "createdAt": "...",
      "updatedAt": "..."
    }
  }
}
```

#### 4. 退出登录（需认证）

```http
POST /auth/logout
Authorization: Bearer <accessToken>
```

**响应**：
```json
{
  "success": true,
  "message": "Client should remove the local access token.",
  "data": {
    "loggedOut": true
  }
}
```

---

### 📊 测速记录接口（需认证）

所有记录接口都需要在请求头中添加：
```
Authorization: Bearer <accessToken>
```

#### 1. 上传单条测速记录

```http
POST /records
```

**请求示例**：
```json
{
  "capturedAt": "2025-06-04T10:30:00.000Z",
  "metrics": {
    "downloadMbps": 156.78,
    "uploadMbps": 45.32,
    "pingMs": 24
  },
  "location": {
    "latitude": 39.9042,
    "longitude": 116.4074,
    "source": "android-client"
  },
  "remark": "在教学楼测试"
}
```

**字段说明**：
- `capturedAt` (可选)：记录时间（ISO 8601），默认为当前时间
- `metrics` (必填)：测速指标对象
  - `downloadMbps` (必填)：下载速度 Mbps（浮点数）
  - `uploadMbps` (必填)：上传速度 Mbps（浮点数）
  - `pingMs` (必填)：延迟 ms（整数）
- `location` (可选)：位置信息对象
  - `latitude` (必填)：纬度（双精度，-90 ~ 90）
  - `longitude` (必填)：经度（双精度，-180 ~ 180）
  - `source` (可选)：位置来源，默认 `"android-client"`
- `remark` (可选)：备注，最多 200 字符

**响应**：
```json
{
  "success": true,
  "message": "ok",
  "data": {
    "record": {
      "id": "uuid-string",
      "userId": "user-uuid",
      "capturedAt": "2025-06-04T10:30:00.000Z",
      "metrics": {
        "downloadMbps": 156.78,
        "uploadMbps": 45.32,
        "pingMs": 24
      },
      "location": {
        "latitude": 39.9042,
        "longitude": 116.4074,
        "source": "android-client"
      },
      "remark": "在教学楼测试",
      "createdAt": "2025-06-04T10:30:00.123Z"
    }
  }
}
```

#### 2. 批量上传测速记录

```http
POST /records/batch
```

**请求示例**：
```json
{
  "records": [
    {
      "capturedAt": "2025-06-04T10:30:00.000Z",
      "metrics": { "downloadMbps": 156.78, "uploadMbps": 45.32, "pingMs": 24 },
      "location": { "latitude": 39.9042, "longitude": 116.4074 }
    },
    {
      "capturedAt": "2025-06-04T10:35:00.000Z",
      "metrics": { "downloadMbps": 142.10, "uploadMbps": 48.50, "pingMs": 28 }
    }
  ]
}
```

**限制**：
- 单次最多上传 100 条记录
- 重复记录（相同用户、时间、指标、位置）会被自动去重

**响应**：
```json
{
  "success": true,
  "message": "ok",
  "data": {
    "count": 2,
    "records": [ /* 上传成功的记录数组，结构与单条上传相同 */ ]
  }
}
```

#### 3. 获取我的测速记录列表

```http
GET /records?limit=50
Authorization: Bearer <accessToken>
```

**查询参数**：
- `limit` (可选)：返回数量，默认 50，最大 200

**响应**：
```json
{
  "success": true,
  "message": "ok",
  "data": {
    "records": [
      {
        "id": "uuid-1",
        "userId": "user-uuid",
        "capturedAt": "2025-06-04T10:35:00.000Z",
        "metrics": { "downloadMbps": 142.10, "uploadMbps": 48.50, "pingMs": 28 },
        "location": { "latitude": 39.9042, "longitude": 116.4074, "source": "android-client" },
        "remark": "",
        "createdAt": "2025-06-04T10:35:00.123Z"
      }
    ]
  }
}
```

---

### 📈 网络评估接口（需认证）

#### 获取当前用户网络评估报告

```http
GET /evaluation
Authorization: Bearer <accessToken>
```

**响应**：
```json
{
  "success": true,
  "message": "ok",
  "data": {
    "score": 72,
    "level": "good",
    "suggestions": [
      "当前网络整体较稳定，建议继续关注校园高峰时段的表现。"
    ],
    "metrics": {
      "downloadAvg": 148.56,
      "pingAvg": 26,
      "rssiAvg": null,
      "snrAvg": null
    },
    "recordCount": 15
  }
}
```

**字段说明**：
- `score` (0-100)：综合评分
- `level`：评级
  - `excellent`：优秀 (≥85)
  - `good`：良好 (70-84)
  - `fair`：一般 (55-69)
  - `poor`：较差 (<55)
  - `no-data`：无数据
- `suggestions`：改进建议数组
- `metrics`：历史平均值
- `recordCount`：用于评估的记录总数

**评估算法**：
- 下载速度评分：`clamp(downloadAvg / 100 * 100, 0, 100)`
- 延迟评分：`clamp((200 - pingAvg) / 180 * 100, 0, 100)`
- 综合分 = 各有效指标评分的算术平均

---

### 🏥 健康检查

```http
GET /health
```

**响应**：
```json
{
  "success": true,
  "message": "ok",
  "data": {
    "status": "ok"
  }
}
```

---

## 🚀 服务器部署完整指南

### 方案一：本地开发环境（Windows）

#### 1. 环境准备

```bash
# 安装 Node.js (>=18)
# 下载地址：https://nodejs.org/

# 验证安装
node --version  # 应显示 v18.x 或更高
npm --version
```

#### 2. 部署项目

```bash
# 1. 进入后端目录
cd D:\NetGeoCourier2\campus-net-backend\campus-net-backend

# 2. 安装依赖
npm install

# 3. 创建环境变量文件
copy .env.example .env

# 4. 编辑 .env 文件，修改关键配置
notepad .env
```

`.env` 示例内容：
```env
HOST=127.0.0.1
PORT=3000
JWT_SECRET=your-super-secret-key-change-this-in-production
JWT_EXPIRES_IN=7d
DATA_FILE=./data/local-db.json
PASSWORD_MIN_LENGTH=6
NODE_ENV=development
```

#### 3. 启动服务

**方式A：直接运行（调试用）**
```bash
node src/server.js
```

**方式B：使用 PM2 守护进程（推荐生产）**
```bash
# 安装 PM2
npm install -g pm2

# 启动服务
pm2 start src/server.js --name netgeo-backend

# 设置开机自启
pm2 startup
pm2 save

# 查看日志
pm2 logs netgeo-backend

# 重启/停止
pm2 restart netgeo-backend
pm2 stop netgeo-backend
```

#### 4. 防火墙配置

**Windows 防火墙**：
1. 打开 "Windows Defender 防火墙"
2. 点击 "高级设置"
3. 选择 "入站规则" → "新建规则"
4. 选择 "端口" → TCP → 特定端口：`3000`
5. 允许连接 → 命名：`NetGeoCourier Backend`

---

### 方案二：Linux 服务器部署（Ubuntu/Debian）

#### 1. 系统环境

```bash
# 更新系统
sudo apt update && sudo apt upgrade -y

# 安装 Node.js 18+
curl -fsSL https://deb.nodesource.com/setup_18.x | sudo -E bash -
sudo apt-get install -y nodejs

# 验证
node --version
npm --version

# 安装 PM2
sudo npm install -g pm2
```

#### 2. 上传项目到服务器

```bash
# 使用git
git clone https://github.com/kevinmou733/NetGeoCourier2.git
cd NetGeoCourier2/campus-net-backend/campus-net-backend
```

#### 3. 配置与启动

```bash
# 安装依赖
npm install --production

# 创建环境变量文件
cp .env.example .env
nano .env  # 编辑配置

# 使用 PM2 启动
pm2 start src/server.js --name netgeo-backend \
  --env NODE_ENV=production

# 保存并设置开机自启
pm2 save
pm2 startup systemd
```

#### 3. 配置 Nginx 反向代理（推荐）

```bash
# 安装 Nginx
sudo apt install nginx

# 创建配置文件
sudo nano /etc/nginx/sites-available/netgeo-api
```

配置内容：
```nginx
server {
    listen 80;
    server_name api.your-domain.com;

    location / {
        proxy_pass http://127.0.0.1:3000;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
        proxy_cache_bypass $http_upgrade;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
}
```

启用站点：
```bash
sudo ln -s /etc/nginx/sites-available/netgeo-api /etc/nginx/sites-enabled/
sudo nginx -t  # 测试配置
sudo systemctl restart nginx
```

#### 4. 配置 HTTPS（Let's Encrypt 免费证书）

```bash
# 安装 Certbot
sudo apt install certbot python3-certbot-nginx

# 申请证书
sudo certbot --nginx -d api.your-domain.com

# 自动续期（Certbot 已自动配置）
```

---

## 🌐 内网穿透配置（樱花 FRP）

### 场景说明

如果你在本地 Windows 电脑运行后端，但想让外网（手机/他人）访问，需要使用内网穿透工具。本教程以 **樱花 FRP** 为例。

### 准备工作

1. **注册樱花 FRP 账号**
   - 访问：https://www.natfrp.com/
   - 实名认证
   - 创建一个新节点（建议选择 "高性能" 或 "企业" 节点，延迟更低）

2. **下载 FRP 客户端**
   - 控制台 → 下载客户端
   - 选择对应系统（Windows x64）
   - 下载 `frpc_windows_amd64.exe`

3. **准备后端服务**
   - 确保后端在本地 `127.0.0.1:3000` 正常运行
   - 测试：`curl.exe http://127.0.0.1:3000/api/v1/health/`

---

### FRP 配置步骤

#### 步骤1：创建 `frpc.ini` 配置文件

在项目根目录或任意位置创建 `frpc.ini`：

```ini
# FRP 客户端配置文件
[common]
server_addr = frp.natfrp.com  # 樱花 FRP 服务端地址
server_port = 7000             # 服务端端口
token = your-token-here        # 从控制台获取的认证令牌

# NetGeo Courier 后端服务
[netgeo-backend]
type = tcp                     # TCP 隧道（后端是 HTTP，但 TCP 隧道更简单）
local_ip = 127.0.0.1          # 本地后端地址
local_port = 3000             # 本地后端端口
remote_port = 11152          # 远程端口（从控制台分配）
```

**如何获取 `token`**：
1. 登录樱花 FRP 控制台
2. 进入 "客户端管理" → 复制 "认证令牌"

**如何分配 `remote_port`**：
1. 控制台 → "隧道管理" → "创建隧道"
2. 服务类型：`TCP`
3. 本地端口：`3000`
4. 节点选择：任意可用节点
5. 提交后获得分配的远程端口（如 `11152`）

---

#### 步骤2：启动 FRP 客户端

**方式A：直接运行**
```bash
# 进入 frp 客户端所在目录
cd D:\frp

# 运行（首次运行会生成 frpc.ini，替换为你的配置）
frpc_windows_amd64.exe -c frpc.ini
```

**方式B：后台运行（Windows）**
```bash
# 使用 NSSM 配置 Windows 服务
nssm install frpc "D:\frp\frpc_windows_amd64.exe" "-c D:\frp\frpc.ini"
nssm start frpc
```

**方式C：打包为系统服务（Linux）**
```bash
# 使用 systemd
sudo nano /etc/systemd/system/frpc.service
```

```ini
[Unit]
Description=FRP Client
After=network.target

[Service]
Type=simple
ExecStart=/usr/local/bin/frpc -c /etc/frp/frpc.ini
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

```bash
sudo systemctl enable frpc
sudo systemctl start frpc
sudo systemctl status frpc
```

---

#### 步骤3：验证隧道状态

FRP 客户端日志应显示：
```
[2025-06-04 10:30:00] [INFO] [netgeo-backend] start proxy success, remote_port:11152 -> local_ip:127.0.0.1:3000
```

**如果失败**，常见原因：
- `connection refused` → 后端未在 127.0.0.1:3000 启动
- `token error` → 令牌错误或过期
- `port already in use` → 远程端口被占用（换一个）

---

### Android 客户端配置

修改 `local.properties`：

```properties
# HTTP 协议（TCP 隧道不支持 HTTPS 加密）
API_BASE_URL=http://www-speedtest-i.hcb8c115c.nyat.app:11152/
```

**说明**：
- 域名：`www-speedtest-i.hcb8c115c.nyat.app`（从樱花 FRP 控制台获取）
- 端口：`11152`（申请的远程端口）
- 协议：**必须用 HTTP**，因为 TCP 隧道不处理 HTTPS 握手

---

### 完整测试流程

#### 1. 启动后端
```bash
cd D:\NetGeoCourier2\campus-net-backend\campus-net-backend
node src/server.js
```

控制台输出：
```
Campus network backend listening on http://127.0.0.1:3000
```

#### 2. 启动 FRP 客户端
```bash
frpc_windows_amd64.exe -c frpc.ini
```

日志显示：
```
[start] start proxy success, remote_port:11152 -> local_ip:127.0.0.1:3000
```

#### 3. 测试穿透连接（在同一台电脑）

```bash
# 测试本地（应该成功）
curl http://127.0.0.1:3000/api/v1/health/

# 测试 FRP 穿透（应该同样成功）
curl http://www-speedtest-i.hcb8c115c.nyat.app:11152/api/v1/health/
```

**期望两者都返回**：
```json
{"success":true,"message":"ok","data":{"status":"ok"}}
```

#### 4. 手机测试

确保手机：
- ✅ 连接到移动数据（不要用同一 WiFi，可能路由策略不同）
- ✅ 能访问互联网
- ✅ 已安装应用

手机浏览器访问：
```
http://www-speedtest-i.hcb8c115c.nyat.app:11152/api/v1/health/
```

应看到 JSON 响应。

#### 5. 应用内测试

打开 Android 应用：
1. 注册账号
2. 登录
3. 进行测速
4. 查看评估

所有操作应顺畅。

---

## 🐛 故障排除

### 问题1：后端无法启动

**症状**：`node src/server.js` 报错退出

**排查**：
```bash
# 检查端口是否被占用
netstat -ano | findstr :3000

# 如果有其他进程，结束它
taskkill /F /PID <PID>

# 检查 Node.js 版本
node --version  # 需要 >=18

# 重新安装依赖
rm -rf node_modules package-lock.json
npm install
```

---

### 问题2：FRP 提示 "connect to local service failed"

**日志**：
```
连接映射目标 [127.0.0.1:3000] 失败, 请检查本地服务是否可访问
```

**解决方案**：
1. **确认后端已启动**：访问 `http://127.0.0.1:3000/api/v1/health/` 是否成功
2. **确认监听地址**：`config/index.js` 中 `host` 应为 `127.0.0.1` 或 `0.0.0.0`
3. **重启 FRP 客户端**：先停止，再启动

---

### 问题3：穿透后无法访问（手机）

**症状**：电脑 curl 成功，手机访问失败

**排查**：
1. **检查 FRP 服务端状态**：登录樱花控制台，查看隧道是否在线
2. **测试手机流量**：关闭 WiFi，用 4G/5G 访问域名
3. **检查防火墙**：
   - Windows 防火墙是否允许 `node.exe` 监听端口
   - 暂时关闭防火墙测试
4. **尝试不同节点**：樱花提供多个节点，换一个试试

---

### 问题4：注册失败 "请稍后再试"

**可能原因**：
1. **数据库文件不可写**
   ```bash
   # 检查 data 目录权限
   ls -la data/
   # 确保 node 用户有读写权限
   ```

2. **用户名已存在**
   - 换一个用户名测试

3. **后端内部错误**
   - 查看后端控制台完整错误堆栈
   - 检查 `data/local-db.json` 文件格式是否正确

4. **环境变量 JWT_SECRET 问题**
   - 确保 `JWT_SECRET` 已设置（不要使用默认值）

---

### 问题5：Android 应用显示 "连接服务器失败"

**检查清单**：
- [ ] `local.properties` 中的 `API_BASE_URL` 是否正确（HTTP 不是 HTTPS）
- [ ] 后端是否正在运行（127.0.0.1:3000）
- [ ] FRP 客户端是否在线（控制台日志）
- [ ] 手机能否访问 `http://www-speedtest-i...:11152/api/v1/health/`
- [ ] AndroidManifest.xml 是否添加网络权限：
  ```xml
  <uses-permission android:name="android.permission.INTERNET" />
  ```

---

## 🔐 安全建议

### 1. 修改默认 JWT 密钥

```bash
# 生成随机密钥
node -e "console.log(require('crypto').randomBytes(32).toString('hex'))"
```

更新 `.env`：
```env
JWT_SECRET=生成的随机字符串
```

### 2. 限制 CORS 源

```env
ALLOWED_ORIGINS=http://your-app-domain.com,https://your-app-domain.com
```

### 3. 启用 HTTPS（如果使用 HTTP 隧道）

如果你的 FRP 支持 HTTPS 隧道（如 Nginx 反向代理 + SSL），可以：
1. 在后端前加 Nginx 配置 SSL
2. Android 改用 `https://` 访问

---

## 📊 监控与日志

### PM2 日志查看

```bash
# 查看所有日志
pm2 logs netgeo-backend

# 查看错误日志
pm2 logs netgeo-backend --err

# 实时监控
pm2 monit
```

### 自定义日志文件

修改 `server.js` 添加 Winston 或 Pino：
```javascript
const pino = require('pino');
const logger = pino({ level: process.env.LOG_LEVEL || 'info' });

// 替换 console.log
logger.info('Server starting...');
```

---

## 🔄 更新与维护

### 代码更新流程

```bash
# 1. 拉取最新代码
git pull origin main

# 2. 安装新依赖
npm install

# 3. 重启服务
pm2 restart netgeo-backend

# 4. 查看状态
pm2 status
```

### 数据备份

```bash
# 备份数据库文件
cp data/local-db.json data/backup-$(date +%Y%m%d).json

# 恢复数据
cp data/backup-20250604.json data/local-db.json
pm2 restart netgeo-backend
```

---

## 📞 技术支持

- **项目主页**: https://github.com/kevinmou733/NetGeoCourier2
- **问题反馈**: https://github.com/kevinmou733/NetGeoCourier2/issues
- **FRP**: https://www.natfrp.com/
- **邮箱**: 2398094726@qq.com

---

## 📄 许可证

MIT License - 详见 LICENSE 文件

---

**最后更新**: 2026-04-12
**维护者**: @kevinmou733
