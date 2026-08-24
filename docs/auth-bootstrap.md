# 本地超级管理员初始化

系统不提供默认管理员账号。本地可用下面任一方式做**一次性**初始化（仅 `local` Profile 生效）：

1. **Compose / 一键脚本（推荐）**：`deploy/.env` 中的 `BOOTSTRAP_SUPER_ADMIN_ENABLED` / `USERNAME` / `PASSWORD`。首次运行 `scripts/windows/deploy-local.ps1` 若新创建 `.env`，会生成并打印密码。
2. **本机直接跑 Spring Boot**：复制 `application-local.yml.example`，设置 `security.bootstrap-super-admin`。
3. **临时环境变量**：

```powershell
$env:BOOTSTRAP_SUPER_ADMIN_ENABLED = "true"
$env:BOOTSTRAP_SUPER_ADMIN_USERNAME = "admin_local"
$env:BOOTSTRAP_SUPER_ADMIN_PASSWORD = "replace-with-a-strong-local-password"
$env:SPRING_PROFILES_ACTIVE = "local"
```

启动后，初始化器仅在不存在任何 `SUPER_ADMIN` 时创建该账号、分配 `SUPER_ADMIN` 角色并写入审计日志；不会输出密码。创建成功后应关闭 `BOOTSTRAP_SUPER_ADMIN_ENABLED`（`.env` 或环境变量）。若账号已存在或系统已有超级管理员，初始化器不会覆盖密码或权限。

初始化密码必须为 12–64 个字符、UTF-8 编码不超过 72 字节，并同时包含大写字母、小写字母、数字和特殊字符。不得使用 `123456`、项目名、用户名或其他已泄露的常见密码。

初始化器被代码层面的 `local` Profile 限制：即使生产环境误设初始化变量，也不会创建管理员账号。

## Flyway 迁移

新建空数据库时，应用会自动执行 `database/V*__*.sql`。现有的本地数据库已手工执行 V1、V2、但还没有 Flyway 历史表时，仅首次启动设置：

```powershell
$env:FLYWAY_BASELINE_ON_MIGRATE = "true"
```

该操作会以 V2 建立迁移基线，随后可关闭该变量。不要对来源不明或部分初始化的数据库开启此开关。
