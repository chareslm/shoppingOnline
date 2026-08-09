# V2：后台 RBAC 权限种子

迁移脚本：[V2__authorization_permissions.sql](../../database/V2__authorization_permissions.sql)。

本版本新增角色查看、权限查看与用户角色分配三个权限编码，并仅授予 `SUPER_ADMIN`。不创建任何默认管理员账号。

`system:user:role:assign` 是高风险权限；后续实现分配或撤销角色接口时必须加入二次验证与审计，不能只凭 Access Token 执行。
