# 校园网综合指标评估系统 - 本地后端

这是给校赛阶段使用的本地登录注册后端，选择 `Node.js + Express`，认证使用轻量 JWT，数据先落到本地 JSON 文件，避免 Firebase、云服务和重型数据库。后续需要上传网络记录、指标点位、评估结果时，可以继续沿用 `/api/v1/...` 的接口风格扩展。

## 目录结构

```text
campus-net-backend/
  src/
    app.js                 # Express 应用装配
    server.js              # 本地服务入口
    config/                # 环境变量和路径配置
    controllers/           # HTTP 控制器
    middleware/            # 鉴权、错误处理、请求 ID
    repositories/          # 本地 JSON 数据读写
    routes/                # /api/v1 路由
    services/              # 登录注册和 JWT 业务逻辑
    utils/                 # 密码、校验、响应工具
  data/                    # 运行时自动生成 local-db.json
  scripts/                 # 本地检查和 smoke test
```

## 启动

```bash
cd campus-net-backend
copy .env.example .env
npm install
npm run dev
```

PowerShell 如果拦截 `npm.ps1`，可改用：

```powershell
npm.cmd install
npm.cmd run dev
```

服务默认监听 `http://0.0.0.0:3000`。Android 模拟器访问宿主机时使用：

```text
http://10.0.2.2:3000
```

真机和电脑在同一局域网时，把 `10.0.2.2` 换成电脑的局域网 IP。

## Auth API

### 注册

```http
POST /api/v1/auth/register
Content-Type: application/json

{
  "username": "student001",
  "password": "123456",
  "displayName": "张三",
  "studentId": "20260001"
}
```

### 登录

```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "username": "student001",
  "password": "123456"
}
```

成功返回：

```json
{
  "success": true,
  "message": "ok",
  "data": {
    "tokenType": "Bearer",
    "accessToken": "eyJhbGciOiJIUzI1NiIs...",
    "expiresIn": 604800,
    "user": {
      "id": "uuid",
      "username": "student001",
      "displayName": "张三",
      "studentId": "20260001",
      "createdAt": "2026-04-07T..."
    }
  }
}
```

### 获取当前用户

```http
GET /api/v1/auth/me
Authorization: Bearer <accessToken>
```

## 预留记录上传 API

登录后可先用这个接口验证 Android 端的鉴权和上传链路：

```http
POST /api/v1/records/batch
Authorization: Bearer <accessToken>
Content-Type: application/json

{
  "records": [
    {
      "capturedAt": "2026-04-07T12:00:00.000Z",
      "metrics": {
        "latencyMs": 28,
        "downloadMbps": 82.5,
        "uploadMbps": 31.2,
        "packetLossRate": 0.01
      },
      "location": {
        "building": "library",
        "floor": 3
      }
    }
  ]
}
```

## 本地检查

```bash
npm run check
```

服务启动后可跑 smoke test：

```bash
npm run smoke
```

## 后续扩展建议

- 数据库：校赛阶段先用 JSON；需要多人协作或并发写入时，把 `repositories/` 替换成 SQLite、PostgreSQL 或 MySQL。
- 认证：当前是 HMAC-SHA256 JWT；上线前请更换强随机 `JWT_SECRET`，并考虑刷新令牌、注销黑名单和 HTTPS。
- 业务接口：继续在 `src/routes/`、`src/controllers/`、`src/repositories/` 中按资源拆分，例如 `/api/v1/points`、`/api/v1/evaluations`。
