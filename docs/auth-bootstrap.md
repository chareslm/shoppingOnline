# 本地超级管理员初始化

系统不提供默认管理员账号。仅在本地开发需要首次创建管理账号时，使用一次性环境变量启用初始化器：

```powershell
$env:BOOTSTRAP_SUPER_ADMIN_ENABLED = "true"
$env:BOOTSTRAP_SUPER_ADMIN_USERNAME = "admin_local"
$env:BOOTSTRAP_SUPER_ADMIN_PASSWORD = "replace-with-a-strong-local-password"
$env:SPRING_PROFILES_ACTIVE = "local"
```

启动后，初始化器仅在不存在任何 `SUPER_ADMIN` 时创建该账号、分配 `SUPER_ADMIN` 角色并写入审计日志；不会输出密码。创建成功后应立刻关闭该环境变量。若账号已存在或系统已有超级管理员，初始化器不会覆盖密码或权限。

初始化器被代码层面的 `local` Profile 限制：即使生产环境误设初始化变量，也不会创建管理员账号。

## Flyway 迁移

新建空数据库时，应用会自动执行 `database/V*__*.sql`。现有的本地数据库已手工执行 V1、V2、但还没有 Flyway 历史表时，仅首次启动设置：

```powershell
$env:FLYWAY_BASELINE_ON_MIGRATE = "true"
```

该操作会以 V2 建立迁移基线，随后可关闭该变量。不要对来源不明或部分初始化的数据库开启此开关。
