# Windows 本地部署与清理

脚本**不会**覆盖已提交的 `application.yml` / `docker-compose.yml`。本机路径和密钥只写入 gitignore 的 `deploy/.env`。

从仓库根目录一键启动（缺 `.env` 时从 example 复制，并自动填本机数据目录、Maven 缓存和密钥）：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/windows/deploy-local.ps1
```

常用开关：

```powershell
# 重建容器并重装前端依赖
powershell -ExecutionPolicy Bypass -File scripts/windows/deploy-local.ps1 -Recreate -InstallDependencies

# 只启动 Docker 后端与中间件，不打开 Vite
powershell -ExecutionPolicy Bypass -File scripts/windows/deploy-local.ps1 -SkipFrontends

# 后端健康检查（默认读取 deploy/.env 的 BACKEND_PORT）
powershell -ExecutionPolicy Bypass -File scripts/windows/wait-backend.ps1
```

首次生成 `deploy/.env` 时会打印一次性 `admin_local` 密码。登录成功后把 `BOOTSTRAP_SUPER_ADMIN_ENABLED` 设为 `false`。说明见 `docs/auth-bootstrap.md`。

## 配置从哪里来

| 文件 | 是否提交 | 作用 |
| --- | --- | --- |
| `deploy/.env.example` | 是 | 可选项说明；不要把真实密码写进 example |
| `deploy/.env` | 否 | 本机密钥与路径；脚本自动填充空值和 `replace-*` |
| `deploy/docker-compose.yml` | 是 | 可移植编排，路径只用 `${DATA_DIR}` / `${MAVEN_REPO_DIR}` |
| `deploy/docker-compose.yml.example` | 是 | 与上面同结构的模板，供对照，不必复制 |
| `application.yml` | 是 | 共享默认值，只用环境变量占位 |
| `application.yml.example` | 是 | 文档副本，**不要覆盖** `application.yml` |
| `application-local.yml.example` | 是 | 仅本机直接跑 Spring Boot 时复制 |
| `frontend-*/.env.example` | 是 | `[OPTIONAL]` API 地址，默认 `http://localhost:8080` |

`deploy/.env.example` 里 `[REQUIRED]` 为密钥，`[OPTIONAL]` 可留空或用默认，`[NATIVE]` 只在宿主机跑后端时生效。SMTP 请留空 `MAIL_HOST`（不要填 `smtp.example.com`）；需要发信时再填真实主机，或在管理端 SMTP 页配置。

## 清理

```powershell
# 日常：停容器、删构建产物；保留数据库、上传文件、.env、node_modules
powershell -ExecutionPolicy Bypass -File scripts/windows/clean-local.ps1

powershell -ExecutionPolicy Bypass -File scripts/windows/clean-local.ps1 -RemoveDependencies
powershell -ExecutionPolicy Bypass -File scripts/windows/clean-local.ps1 -RemoveLocalConfig

# 删除 ${DATA_DIR}/shopping（默认 %USERPROFILE%/shopping-data/shopping）
powershell -ExecutionPolicy Bypass -File scripts/windows/clean-local.ps1 -PurgePersistentData
```

`-PurgePersistentData` 需输入 `PURGE`；自动化时再加 `-Force`。
