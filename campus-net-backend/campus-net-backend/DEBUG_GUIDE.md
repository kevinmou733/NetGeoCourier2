# Campus Net Backend - 调试指南

## 项目概述

这是一个基于 Node.js + Express 的校园网指标评估系统后端，使用 JWT 认证，数据存储在本地 JSON 文件中。

**技术栈：**
- Node.js >= 18
- Express 4.x
- JWT (jsonwebtoken)
- 本地 JSON 文件存储

## 1. 环境准备

### 1.1 检查 Node.js 版本
```bash
node --version
# 确保版本 >= 18.x
```

### 1.2 安装依赖
```bash
cd campus-net-backend
npm install
```

## 2. 配置环境变量

### 2.1 复制环境配置模板
```bash
copy .env.example .env
# 或在 PowerShell/Linux/Mac:
cp .env.example .env
```

### 2.2 编辑 .env 文件（可选）
```env
PORT=3000
HOST=0.0.0.0
NODE_ENV=development
JWT_SECRET=change-me-local-dev-secret
JWT_EXPIRES_IN=7d
DATA_FILE=./data/local-db.json
PASSWORD_MIN_LENGTH=6
ALLOWED_ORIGINS=http://localhost:5173,http://127.0.0.1:5173
```

**重要：** 生产环境必须修改 `JWT_SECRET` 为强随机字符串！

## 3. 启动服务

### 3.1 开发模式（推荐调试使用）
```bash
npm run dev
```
这会启动带热重载的 Node.js 监视模式，文件修改后自动重启。

**PowerShell 用户注意：** 如果遇到 npm.ps1 执行策略问题，使用：
```powershell
npm.cmd run dev
```

### 3.2 生产模式
```bash
npm start
```

### 3.3 验证服务启动
成功启动后，终端会显示：
```
Campus network backend listening on http://0.0.0.0:3000
Data file: D:\...\data\local-db.json
```

访问健康检查接口：
```bash
curl http://localhost:3000/api/v1/health
# 应返回: {"status":"ok"}
```

## 4. 内置检查工具

### 4.1 语法检查
```bash
npm run check
```
运行 `scripts/check-syntax.js` 检查代码语法。

### 4.2 Smoke Test（集成测试）
```bash
npm run smoke
```
自动测试注册、登录、获取用户信息流程。

**调试特定 API 端点：**
```bash
# 设置自定义 API 地址
API_BASE_URL=http://localhost:3000 npm run smoke
```

## 5. 调试技巧

### 5.1 使用 console.log
在代码中添加日志输出：
```javascript
// 例如在控制器中
console.log('[AuthController] 注册请求:', req.body);
console.log('[UserRepository] 用户数据:', user);
```

### 5.2 使用 Node.js 调试器

#### 方法 A：使用 `--inspect` 标志
```bash
node --inspect src/server.js
```
然后在 Chrome 浏览器打开 `chrome://inspect` 进行调试。

#### 方法 B：使用 `--inspect-brk`（在入口处中断）
```bash
node --inspect-brk src/server.js
```

#### 方法 C：在 VS Code 中调试
1. 打开项目文件夹
2. 创建 `.vscode/launch.json`：
```json
{
  "version": "0.2.0",
  "configurations": [
    {
      "type": "node",
      "request": "launch",
      "name": "调试后端",
      "program": "${workspaceFolder}/src/server.js",
      "env": {
        "NODE_ENV": "development"
      }
    }
  ]
}
```
3. 按 F5 启动调试

### 5.3 查看请求日志
中间件已添加 `requestId`，错误日志格式：
```
[请求ID] Error message
```
可在 `src/middleware/requestId.js` 中增强日志。

### 5.4 调试数据库（JSON 文件）
数据存储在 `data/local-db.json`（自动创建）。

查看数据：
```bash
cat data/local-db.json
# 或在 Windows:
type data\local-db.json
```

**注意：** 文件可能被进程占用，编辑前确保服务已停止。

## 6. API 调试

### 6.1 使用 curl 测试

**注册：**
```bash
curl -X POST http://localhost:3000/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"test001","password":"123456","displayName":"测试用户","studentId":"20260001"}'
```

**登录：**
```bash
curl -X POST http://localhost:3000/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test001","password":"123456"}'
```

**获取用户信息（需 Token）：**
```bash
curl -X GET http://localhost:3000/api/v1/auth/me \
  -H "Authorization: Bearer <your-token>"
```

**上传记录：**
```bash
curl -X POST http://localhost:3000/api/v1/records/batch \
  -H "Authorization: Bearer <your-token>" \
  -H "Content-Type: application/json" \
  -d '{"records":[{"capturedAt":"2026-04-07T12:00:00.000Z","metrics":{"latencyMs":28,"downloadMbps":82.5,"uploadMbps":31.2,"packetLossRate":0.01},"location":{"building":"library","floor":3}}]}'
```

### 6.2 使用 Postman / Insomnia
1. 创建新请求
2. 设置 URL 和方法
3. 添加 Headers：`Content-Type: application/json`
4. 认证相关接口添加 Body（raw JSON）
5. 需要 Token 的接口在 Headers 添加 `Authorization: Bearer <token>`

### 6.3 调试认证问题
- 检查 JWT_SECRET 是否一致（登录和验证使用相同的 secret）
- Token 过期时间默认为 7 天（`JWT_EXPIRES_IN=7d`）
- Token 格式必须为 `Bearer <token>`

## 7. 常见问题与解决方案

### 7.1 端口已被占用
**错误：** `Error: listen EADDRINUSE: address already in use :::3000`

**解决：**
```bash
# Windows: 查找占用端口的进程
netstat -ano | findstr :3000

# 结束进程（PID 为 1234）
taskkill /PID 1234 /F

# 或修改 .env 中的 PORT 为其他端口
PORT=3001
```

### 7.2 找不到模块
**错误：** `Error: Cannot find module 'express'`

**解决：**
```bash
npm install
```

### 7.3 .env 文件未加载
**现象：** 配置值不是预期的

**解决：**
- 确保 `.env` 文件在项目根目录（与 `package.json` 同级）
- 检查文件名不是 `.env.txt`（Windows 可能隐藏扩展名）
- 验证环境变量格式：`KEY=VALUE`，无引号

### 7.4 CORS 错误
**错误：** `Access to fetch at ... has been blocked by CORS policy`

**解决：**
在 `.env` 中添加前端地址：
```env
ALLOWED_ORIGINS=http://localhost:5173,http://127.0.0.1:5173
```
重启服务。

### 7.5 JSON 数据文件问题
**现象：** 数据未保存或读取错误

**解决：**
```bash
# 检查 data 目录是否存在
ls data/

# 检查文件权限
# Windows: 确保文件未被其他程序锁定

# 清空数据（慎用）
rm data/local-db.json
# 重启服务会自动创建空数据库
```

### 7.6 JWT 令牌无效
**错误：** `invalid token` 或 `Unauthorized`

**解决：**
- 确认 Token 未过期
- 确认请求头格式正确：`Authorization: Bearer <token>`
- 检查 `JWT_SECRET` 是否更改过（更改后旧 Token 失效）

### 7.7 PowerShell 执行策略问题
**错误：** `npm.ps1 无法加载，因为在此系统上禁止运行脚本`

**解决：**
```powershell
# 方法 1：使用 npm.cmd
npm.cmd install
npm.cmd run dev

# 方法 2：更改执行策略（需要管理员权限）
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
```

## 8. 目录结构速查

```
campus-net-backend/
├── src/
│   ├── server.js           # 服务入口
│   ├── app.js              # Express 应用配置
│   ├── config/             # 环境配置
│   ├── controllers/        # 控制器（处理请求）
│   ├── middleware/         # 中间件（鉴权、错误处理）
│   ├── repositories/       # 数据访问层
│   ├── routes/             # 路由定义
│   ├── services/           # 业务逻辑
│   └── utils/              # 工具函数
├── data/                   # 运行时数据（local-db.json）
├── scripts/               # 检查脚本
├── package.json
└── .env                   # 环境变量（需自行创建）
```

## 9. 调试特定模块

### 9.1 调试认证流程
核心文件：
- `src/controllers/auth.controller.js` - 处理注册/登录请求
- `src/services/auth.service.js` - 认证业务逻辑
- `src/services/token.service.js` - JWT 生成和验证
- `src/repositories/userRepository.js` - 用户数据读写

**调试点：**
1. 在 `auth.service.js` 的 `register` 函数添加日志
2. 检查密码哈希：`bcrypt.hashSync`
3. 查看 Token 生成：`tokenService.generateToken`

### 9.2 调试数据存储
核心文件：
- `src/repositories/jsonDatabase.js` - JSON 文件读写
- `src/repositories/recordRepository.js` - 记录仓储
- `src/repositories/userRepository.js` - 用户仓储

**调试点：**
1. 检查 `dataFile` 路径是否正确
2. 查看文件读写是否抛出异常
3. 确认 JSON 格式正确

### 9.3 调试中间件
- `src/middleware/authenticate.js` - JWT 鉴权
- `src/middleware/errorHandler.js` - 错误处理
- `src/middleware/requestId.js` - 请求 ID 生成

**调试点：**
1. 在 `authenticate` 中打印 `req.headers.authorization`
2. 检查错误是否被正确捕获和处理

## 10. 性能调试

### 10.1 使用 Node.js Profiler
```bash
# 生成性能分析文件
node --prof src/server.js

# 处理分析结果
node --prof-process isolate-*.log > processed.txt
```

### 10.2 监控内存使用
```bash
# 查看内存占用
node --inspect src/server.js
# 在 Chrome DevTools Memory 标签页分析
```

## 11. 日志级别调整

当前日志分为：
- `console.log` - 普通信息
- `console.error` - 错误（在 errorHandler.js:22）

如需更详细日志，可在 `src/config/index.js` 中设置：
```javascript
const config = {
  // ...
  logLevel: process.env.LOG_LEVEL || 'info',  // 可扩展
};
```

## 12. 连接 Android 模拟器调试

### 12.1 确保模拟器网络连通
```bash
# 在模拟器中访问宿主机
curl http://10.0.2.2:3000/api/v1/health
```

### 12.2 真机调试
1. 确保手机和电脑在同一局域网
2. 修改 `.env`：
```env
HOST=0.0.0.0
```
3. 查找电脑局域网 IP：
   - Windows: `ipconfig`
   - Mac/Linux: `ifconfig`
4. 在手机浏览器访问 `http://<电脑IP>:3000/api/v1/health`

## 13. 数据库迁移（未来扩展）

当前使用 JSON 文件，如需迁移到数据库：
1. 保留 `repositories/` 接口不变
2. 实现新的 `userRepository.js` 和 `recordRepository.js` 使用 SQL/NoSQL
3. 更新 `DATA_FILE` 相关配置

## 14. 快速诊断命令

```bash
# 1. 检查服务是否运行
curl http://localhost:3000/api/v1/health

# 2. 查看端口占用
netstat -ano | findstr :3000

# 3. 查看最近日志（Windows）
Get-Content data\local-db.json | Select-Object -Last 20

# 4. 检查 Node 版本
node --version

# 5. 清理缓存重装
rm -rf node_modules package-lock.json
npm install

# 6. 检查环境变量
node -e "console.log(require('./src/config').config)"
```

## 15. 联系与支持

如有问题，请检查：
1. Node.js 版本是否符合要求
2. 依赖是否完整安装
3. 环境变量配置是否正确
4. 端口是否被占用
5. 数据目录是否有写入权限

---

**最后更新：** 2026-04-12
**后端版本：** 0.1.0
