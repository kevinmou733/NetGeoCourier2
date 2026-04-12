# 樱花内网穿透配置完整教程

## 一、注册账号

1. 访问官网：https://www.natfrp.com/
2. 点击右上角「注册」
3. 填写信息（建议用 GitHub 账号快速注册）
4. 邮箱验证（检查垃圾邮件箱）
5. 登录控制台：https://console.natfrp.com/

---

## 二、下载客户端

### Windows 用户
1. 进入「下载」页面：https://www.natfrp.com/download
2. 下载 **Sakura Frp 客户端 (Windows版)**
3. 解压到项目目录，例如：
```
D:\NetGeoCourier2\campus-net-backend\sakura-frp\
```

### macOS / Linux
选择对应版本下载，解压后使用终端运行。

---

## 三、在控制台创建隧道（关键步骤）

在配置客户端**之前**，需要先在网页控制台创建隧道：

### 3.1 创建隧道
1. 登录控制台：https://console.natfrp.com/
2. 点击左侧菜单「隧道管理」 → 「创建隧道」
3. 填写表单：
   - **隧道名称：** `campus-backend`（自定义，便于识别）
   - **隧道类型：** `HTTP`
   - **本地端口：** `3000`
   - **远程端口：** `0`（随机分配，免费版）
   - **节点选择：** 选「自动」或离你近的节点（如「上海-联通」）
   - **开启压缩：** ✅ 勾选
   - **开启加密：** ✅ 勾选（安全）
4. 点击「提交创建」

### 3.2 获取配置信息
创建成功后，页面显示：
```
隧道名称: campus-backend
外网地址: http://xxx.natfrp.cloud
授权密钥: sakura_xxxxx（重要！）
```
**复制「授权密钥」**（后面配置客户端要用）

### 3.3 记录外网地址
「外网地址」就是你之后告诉用户的访问地址：
```
http://xxx.natfrp.cloud
```

---

## 四、获取授权密钥

1. 创建隧道后，点击隧道名称进入详情页
2. 找到「授权密钥」或「客户端配置」
3. 点击「显示密钥」并复制
4. 如果密钥泄露，可以点击「重置密钥」

---

## 五、配置客户端

### 4.1 编辑配置文件

在解压目录找到 `config.json`（或 `frpc.ini`），用记事本打开：

**关键：** 将第 3 步复制的「授权密钥」填入

**如果是 `config.json`：**
```json
{
  "token": "粘贴你的授权密钥（sakura_xxxxx）",
  "log_level": "info",
  "log_file": "logs/frpc.log",
  "tunnels": {
    "campus-backend": {
      "type": "http",
      "local_port": 3000,
      "remote_port": 0,
      "domain": "0",
      "crypto": true,
      "compression": true,
      "use_encryption": true,
      "use_compression": true,
      "remark": "校园网后端-校赛",
      "node": "自动"  // 可选：指定节点
    }
  }
}
```

**如果是 `frpc.ini`：**
```ini
[common]
token = 粘贴你的授权密钥（sakura_xxxxx）

[campus-backend]
type = http
local_port = 3000
remote_port = 0
domain = 0
use_encryption = true
use_compression = true
remark = 校园网后端-校赛
```

**参数说明：**
- `local_port = 3000` → 本地后端端口
- `remote_port = 0` → 使用平台随机分配
- `domain = 0` → 使用平台随机域名
- `use_encryption = true` → 启用加密（推荐）
- `use_compression = true` → 启用压缩（推荐）

---

## 五、启动隧道

### 方法 A：双击启动（推荐新手）
在解压目录双击 `SakuraFrp.exe`（Windows）

控制台会显示：
```
[INFO] 隧道 [campus-backend] 启动成功
[INFO] 外网地址: http://xxx.natfrp.cloud
```

### 方法 B：命令行启动
```bash
# Windows 进入解压目录
cd D:\NetGeoCourier2\campus-net-backend\sakura-frp

# 启动
SakuraFrp.exe
```

### 方法 C：后台运行（不关闭窗口）
```bash
# Windows 使用 start 命令
start SakuraFrp.exe
```

---

## 六、获取外网地址

启动成功后，控制台会输出类似：
```
隧道名称: campus-backend
外网协议: http
外网地址: http://7dh2s.natfrp.cloud
内网地址: 127.0.0.1:3000
```

**复制外网地址：** `http://7dh2s.natfrp.cloud`

---

## 七、验证穿透成功

### 7.1 本地先启动后端
```bash
cd D:\NetGeoCourier2\campus-net-backend\campus-net-backend
npm run dev
```

看到输出：
```
Campus network backend listening on http://0.0.0.0:3000
```

### 7.2 测试穿透地址
在任意浏览器（或手机）访问：
```
http://7dh2s.natfrp.cloud/api/v1/health
```

**预期返回：**
```json
{"status":"ok"}
```

如果成功，说明穿透配置正确！

---

## 八、配置 Android App

### 8.1 修改后端地址

根据你的 App 类型：

**情况 A：硬编码在代码中**
```javascript
// android/app/src/main/assets/... 或 config.js
const API_BASE_URL = 'http://7dh2s.natfrp.cloud';
// 重新打包 APK
```

**情况 B：可配置的设置页面**
在 App 设置中手动输入：
```
后端地址: http://7dh2s.natfrp.cloud
```

**情况 C：使用动态配置（推荐）**
如果 App 支持从配置文件读取：
```json
// app-config.json（放在 assets 目录）
{
  "API_BASE_URL": "http://7dh2s.natfrp.cloud"
}
```

### 8.2 测试登录
在 App 中尝试注册/登录，应该能正常访问。

---

## 九、高级配置（可选）

### 9.1 使用自定义域名
如果你有域名 `yourdomain.com`：

1. **DNS 解析设置：**
   - 主机记录：`api`（即 `api.yourdomain.com`）
   - 记录类型：`CNAME`
   - 记录值：`xxx.natfrp.cloud`

2. **修改隧道配置：**
```json
{
  "tunnels": {
    "campus-backend": {
      "type": "http",
      "local_port": 3000,
      "remote_port": 0,
      "domain": "api.yourdomain.com"  // 改成你的域名
    }
  }
}
```

3. **重启隧道**
用户访问：`http://api.yourdomain.com/api/v1/health`

### 9.2 设置访问密码（增强安全）
```json
{
  "tunnels": {
    "campus-backend": {
      "type": "http",
      "local_port": 3000,
      "remote_port": 0,
      "domain": "0",
      "auth": {
        "username": "campus2024",
        "password": "你的密码"
      }
    }
  }
}
```
用户访问时会弹出浏览器认证窗口。

### 9.3 绑定固定端口
```json
{
  "tunnels": {
    "campus-backend": {
      "type": "http",
      "local_port": 3000,
      "remote_port": 80,  // 固定为 80 端口
      "domain": "0"
    }
  }
}
```
访问地址变为：`http://xxx.natfrp.cloud`（去掉端口号）

---

## 十、保持隧道稳定运行

### 10.1 设置开机自启（Windows）

1. 创建启动脚本 `start-frp.bat`：
```batch
@echo off
cd /d D:\NetGeoCourier2\campus-net-backend\sakura-frp
start SakuraFrp.exe
exit
```

2. 按 `Win+R` 输入 `shell:startup`
3. 将 `start-frp.bat` 复制到打开的目录
4. 重启电脑，隧道会自动运行

### 10.2 使用任务计划程序（更稳定）
1. 打开「任务计划程序」
2. 创建基本任务 → 触发器「登录时」
3. 操作：启动程序 → 选择 `SakuraFrp.exe`
4. 完成

---

## 十一、常见问题

### Q1: 隧道启动失败，提示「密钥无效」
**解决：**
1. 检查 `config.json` 中的 `token` 是否正确复制
2. 去控制台重新生成密钥
3. 确保客户端是最新版本

### Q2: 外网地址无法访问
**排查步骤：**
1. 确认本地后端已启动：`http://localhost:3000/api/v1/health` 能访问
2. 检查隧道状态：控制台显示「运行中」
3. 尝试更换 `remote_port`（设为 0 或 8080）
4. 查看客户端日志文件 `logs/frpc.log`

### Q3: 访问速度慢
**优化：**
1. 在隧道配置中启用 `use_compression: true`
2. 选择离你近的节点（控制台可切换服务器区域）
3. 避免上传大文件（HTTP 隧道不适合传大文件）

### Q4: 域名无法绑定
**原因：**
- DNS 未生效（等待 10 分钟）
- CNAME 记录格式错误
- 隧道配置的 `domain` 字段未更新

### Q5: Windows 防火墙弹窗
**允许访问：**
第一次启动时会弹窗，选择「允许访问」→「专用网络」即可。

---

## 十二、监控和维护

### 12.1 查看隧道状态
登录控制台：https://console.natfrp.com/
- 隧道列表显示「在线」/「离线」
- 流量使用情况
- 连接数统计

### 12.2 查看日志
客户端日志文件位置：
```
sakura-frp\logs\frpc-日期.log
```

关键信息：
```
[INFO] 隧道启动成功
[INFO] 代理连接成功
[ERROR] 连接失败
```

### 12.3 流量监控
免费版每月 1TB，查看：
- 控制台「流量统计」
- 超出后隧道会暂停

---

## 十三、安全建议

### 13.1 定期更换密钥
控制台 → 用户中心 → 授权密钥 → 删除旧密钥 → 创建新密钥 → 更新 `config.json`

### 13.2 使用访问密码
在隧道配置中添加 `auth.username` 和 `auth.password`，防止他人滥用你的隧道。

### 13.3 限制访问时间
用完及时关闭客户端，避免长期暴露。

---

## 十四、一键启动脚本

创建 `start-backend-with-frp.bat`：
```batch
@echo off
echo 正在启动校园网后端和内网穿透...

cd /d D:\NetGeoCourier2\campus-net-backend\sakura-frp
start SakuraFrp.exe

timeout /t 3 /nobreak >nul

cd /d D:\NetGeoCourier2\campus-net-backend\campus-net-backend
npm run dev

pause
```

双击即可同时启动隧道和后端。

---

## 十五、停止隧道

### 方法 A：关闭客户端窗口
直接关闭 SakuraFrp 窗口即可。

### 方法 B：任务管理器
按 `Ctrl+Shift+Esc` → 找到 `SakuraFrp.exe` → 结束任务

### 方法 C：命令行
```bash
taskkill /IM SakuraFrp.exe /F
```

---

## 十六、备用方案

如果樱花内网穿透不稳定，可切换其他平台：

| 平台 | 免费额度 | 优点 | 缺点 |
|------|---------|------|------|
| **樱花** | 1TB/月 | 国内节点、速度快 | 有广告 |
| **ngrok** | 1GB/月 | 国际通用、稳定 | 跨境、慢 |
| **frp 自建** | 无限 | 完全控制 | 需服务器成本 |
| **Cloudflare Tunnel** | 无限 | 免费 HTTPS、安全 | 需域名 |

---

## 快速参考卡

```
启动隧道：双击 SakuraFrp.exe
查看地址：控制台日志中的 "外网地址"
停止隧道：关闭窗口或任务管理器结束进程
修改配置：编辑 config.json → 重启客户端
查看日志：sakura-frp/logs/frpc-*.log
```

---

## 下一步

1. ✅ 注册账号并下载客户端
2. ✅ 配置 `config.json` 填写密钥
3. ✅ 启动后端 `npm run dev`
4. ✅ 启动隧道 `SakuraFrp.exe`
5. ✅ 测试 `http://xxx.natfrp.cloud/api/v1/health`
6. ✅ 修改 App 配置为新地址

**遇到问题？** 查看 `logs/frpc.log` 或去控制台查看隧道状态。

---

**文档版本：** 1.0
**适用平台：** Windows/macOS/Linux
**最后更新：** 2026-04-12
