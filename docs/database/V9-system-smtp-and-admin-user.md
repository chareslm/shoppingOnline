# V9：系统 SMTP 配置与手动建号权限

迁移脚本：[V9__system_smtp_and_admin_user.sql](../../database/V9__system_smtp_and_admin_user.sql)。

单行表 `system_smtp_setting` 供系统管理员保存运行时 SMTP（主机、端口、账号、密码、发件人、TLS）以及 `enabled` 发信开关。`enabled = 0` 时不发送任何事务邮件、不回退环境变量 SMTP；新建账号初始密码固定为 `123456QWERqwer!@`，邮件状态记为 `SKIPPED`。密码只写入数据库，查询接口不返回明文。未保存主机且 SMTP 仍启用时，发信可回退到环境变量 / Spring Mail。

仅授予 `SUPER_ADMIN` 的权限：

- `system:smtp:view`：查看 SMTP 配置（不含密码）。
- `system:smtp:update`：更新 SMTP 配置，须二次确认当前管理员密码。
- `system:user:create`：手动创建平台账号（邮箱必填，首次登录强制改密）。
