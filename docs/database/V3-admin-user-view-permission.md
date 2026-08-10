# V3 管理端用户查询权限

迁移文件：`database/V3__admin_user_view_permission.sql`。

本迁移新增 `system:user:view` 权限，用于管理端按关键字和状态查询平台账号及其角色，并默认授予内置 `SUPER_ADMIN` 角色。用户邮箱和手机号在接口响应中脱敏；角色修改继续使用 `system:user:role:assign`，并要求当前超级管理员二次输入密码。

迁移使用幂等写法，可由 Flyway 在 V2 之后自动执行。不得将用户查询权限授予普通用户或无用户管理职责的商家员工。
