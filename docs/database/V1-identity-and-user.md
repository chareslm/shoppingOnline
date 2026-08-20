# V1：身份、RBAC 与用户数据模型

迁移脚本：[V1__identity_and_user.sql](../../database/V1__identity_and_user.sql)。

本版本建立身份权限及用户域的首批数据表：统一账号、角色、权限、用户与角色/权限关联、设备、刷新令牌、审计日志、用户资料、收货地址、用户偏好和店铺关注。

`shop_follow.shop_id` 暂不建立外键，因为店铺实体由 `merchant` 模块后续创建；应用层应仅写入已存在的店铺 ID。该约束应在商家模块完成数据表设计后统一复核。

迁移预置六个内置角色：`USER`、`MERCHANT_OWNER`、`MERCHANT_STAFF`、`CUSTOMER_SERVICE`、`ADMIN` 和 `SUPER_ADMIN`。不预置管理员账号、密码或任何令牌。

刷新令牌仅允许保存 SHA-256 等单向哈希后的值；审计日志不得记录密码、Token 明文或完整隐私数据。

设备与会话管理复用 V1 的 `user_device` 和 `refresh_token` 表，不新增迁移。登录会创建或重新激活设备；普通退出、指定设备撤销和其他设备批量撤销会将对应 `user_device.status` 更新为 `REVOKED`，同时仅更新尚未撤销的 Refresh Token。设备重新使用正确密码登录时可恢复为 `ACTIVE`。设备列表中的会话状态以未撤销且未过期的 Refresh Token 为准，避免密码修改或角色变更后设备状态字段短暂滞后。

设备撤销审计动作码为 `DEVICE_REVOKE` 和 `OTHER_DEVICES_REVOKE`；目标类型为 `DEVICE`，目标 ID 只保存内部设备主键，不保存客户端设备 ID、Token 或完整 IP。
