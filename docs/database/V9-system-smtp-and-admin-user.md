# V9：系统 SMTP 配置与手动建号权限

迁移脚本：[V9__system_smtp_and_admin_user.sql](../../database/V9__system_smtp_and_admin_user.sql)。

本版本新增单行表 `system_smtp_setting`，供系统管理员在管理端保存运行时 SMTP 主机、端口、账号、密码、发件人和 TLS 选项。密码只写入数据库，查询接口不返回明文。未在管理端保存主机时，发信仍可回退到环境变量 / `application-local.yml` 中的 Spring Mail 配置。

同时新增三个仅授予 `SUPER_ADMIN` 的权限：

- `system:smtp:view`：查看 SMTP 配置（不含密码）。
- `system:smtp:update`：更新 SMTP 配置，须二次确认当前管理员密码。
- `system:user:create`：手动创建平台账号。账号必须填写邮箱；系统生成临时密码、标记 `must_change_password`，并通过 SMTP 投递。邮件失败不回滚建号，可重发（重发会轮换临时密码）。

迁移使用幂等权限种子，可由 Flyway 在 V8 之后自动执行。
