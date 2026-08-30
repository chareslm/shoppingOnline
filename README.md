# 电商平台课程设计

本项目为五人合作开发的综合电商平台课程设计。

## 项目组成

- `backend`：后端项目。
- `frontend-web`：用户 Web 商城。
- `frontend-app`：Flutter Android App。
- `frontend-miniapp`：微信原生小程序。
- `frontend-admin`：系统管理员与平台管理员管理端。
- `deploy`：Docker Compose 及 MySQL、Redis、Elasticsearch、Kibana、Filebeat 部署配置。
- `database`：数据库建表、初始化和迁移脚本。
- `docs`：需求、架构、接口和协作文档。

## Git 分支

- `main`：稳定版本。
- `develop`：开发集成版本。
- `feature/*`：功能开发分支。
- `fix/*`：Bug 修复分支。

日常开发从 `develop` 创建功能分支，并通过 Pull Request 合并回 `develop`。详细规则见 [docs/git-workflow.md](docs/git-workflow.md)。

微信小程序的导入、API 地址和本地联调说明见 [frontend-miniapp/README.md](frontend-miniapp/README.md)。

## Windows 快速部署

本地配置必须从 example 复制，真实密码和 SMTP 凭据不得写入 example 或提交到 Git。

```powershell
# 准备配置、启动 Docker 服务并启动两个 Vue 开发服务器
powershell -ExecutionPolicy Bypass -File scripts/windows/deploy-local.ps1

# 只起中间件与后端，不打开 Vite（再按下面各端命令单独启动）
powershell -ExecutionPolicy Bypass -File scripts/windows/deploy-local.ps1 -SkipFrontends

# 默认安全清理：保留持久化数据、本地配置和依赖
powershell -ExecutionPolicy Bypass -File scripts/windows/clean-local.ps1
```

高风险清理及可选参数见 [scripts/windows/README.md](scripts/windows/README.md)。

启动完成后先确认后端健康再打开各端登录页：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/windows/wait-backend.ps1
# 或访问 http://127.0.0.1:8080/actuator/health ，应为 UP
```

## 各端启动

四端共用同一后端（默认 `http://127.0.0.1:8080`）。未先把后端拉到 `UP` 时，不要在前端登录。

| 端 | 目录 | 默认地址 |
| --- | --- | --- |
| 后端 API | `backend` | http://127.0.0.1:8080 |
| 管理端 | `frontend-admin` | http://localhost:5173 |
| 用户 Web | `frontend-web` | http://127.0.0.1:5174 |
| Flutter App | `frontend-app` | Android 模拟器 / 真机 |
| 微信小程序 | `frontend-miniapp` | 微信开发者工具 |

### 后端

依赖本机或 Compose 中的 MySQL、Redis、Elasticsearch。一键脚本会起这些服务；若只在宿主机跑 Spring Boot：

```powershell
cd backend
# 首次：复制 src/main/resources/application-local.yml.example 为 application-local.yml 并填写 JWT_SECRET 与数据库
mvn spring-boot:run
```

说明见 [backend/README.md](backend/README.md)、[docs/auth-bootstrap.md](docs/auth-bootstrap.md)。

### 管理端

```powershell
cd frontend-admin
npm install
# 首次可复制 .env.example 为 .env，用 VITE_API_BASE_URL 覆盖 API 地址
npm run dev
```

浏览器打开 http://localhost:5173 。详见 [frontend-admin/README.md](frontend-admin/README.md)。

### 用户 Web

```powershell
cd frontend-web
npm install
# 首次可复制 .env.example 为 .env，用 VITE_API_BASE_URL 覆盖 API 地址
npm run dev
```

浏览器打开 http://127.0.0.1:5174 。详见 [frontend-web/README.md](frontend-web/README.md)。

### Flutter Android App

需已安装 Flutter SDK，并启动 Android 模拟器或连接真机。

```powershell
cd frontend-app
flutter pub get
# 模拟器访问宿主机后端
flutter run --dart-define=API_BASE_URL=http://10.0.2.2:8080
```

真机把 `10.0.2.2` 换成电脑的局域网 IP。详见 [frontend-app/README.md](frontend-app/README.md)。

### 微信小程序

```powershell
cd frontend-miniapp
npm install
npm run typecheck
```

用微信开发者工具导入仓库中的 `frontend-miniapp` 目录（不要只打开 `miniprogram/`）。公共配置使用 `touristappid`；本地联调可复制 `project.private.config.json.example` 为被忽略的 `project.private.config.json`，并关闭合法域名校验。开发 API 默认 `http://127.0.0.1:8080`。真机不能访问 `127.0.0.1`，需改局域网地址。详见 [frontend-miniapp/README.md](frontend-miniapp/README.md)。
