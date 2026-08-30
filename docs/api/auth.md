# 认证接口（第一版）

所有响应使用统一格式：`{ "code": 0, "message": "success", "data": ... }`。登录和刷新接口会同时签发短期 Access Token 与可轮换的 Refresh Token。

## 注册

`POST /api/auth/register`

请求体至少提供 `username`、`email`、`phone` 中的一项。用户名须以字母开头，由字母、数字或下划线组成，长度 3–64；当前中国大陆手机号格式为 11 位、以 `1` 开头；密码长度为 12–64 字符、UTF-8 编码不得超过 72 字节，并须同时包含大写字母、小写字母、数字和特殊字符。

```json
{
  "username": "alice_1",
  "password": "Password123!"
}
```

成功响应：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "userId": 101,
    "username": "alice_1",
    "status": "ACTIVE"
  }
}
```

重复用户名、邮箱或手机号返回 HTTP `409` 和业务码 `40901`。注册会分配 `USER` 角色，并创建资料与偏好记录。

## 密码登录

`POST /api/auth/login/password`

`identifier` 可为用户名、邮箱或手机号。`deviceId` 必须由客户端为该设备稳定生成；`deviceType` 为 `WEB`、`ANDROID`、`MINIAPP` 或 `ADMIN_WEB`。

```json
{
  "identifier": "alice_1",
  "password": "Password123!",
  "deviceId": "browser-installation-id",
  "deviceType": "WEB",
  "deviceName": "Chrome on Windows"
}
```

成功响应中的 `accessToken` 应通过 `Authorization: Bearer <accessToken>` 发送；有效期当前为 30 分钟。`refreshToken` 有效期为 7 天，只能提交给刷新接口，不能用于访问业务接口。`ADMIN_WEB` 仅允许 `ADMIN` / `SUPER_ADMIN`；商家与客服使用用户 Web 的 `WEB` 登录。

新签发的 Access Token 同时携带服务端内部设备标识，用于可靠判断当前设备。该标识由服务端根据登录设备记录写入，客户端不得自行提交或覆盖。升级前签发且尚未过期的旧 Access Token 不含设备标识；访问设备管理接口时会返回 `401`，客户端应按现有机制刷新 Token 后自动重试。

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "userId": 101,
    "username": "alice_1",
    "accessToken": "<JWT>",
    "refreshToken": "<JWT>",
    "expiresInSeconds": 1800,
    "roles": ["USER"],
    "permissions": [],
    "mustChangePassword": false
  }
}
```

错误状态：凭据错误为 HTTP `401` / `40102`；账号已禁用为 HTTP `403` / `40302`；连续失败导致的临时锁定为 HTTP `423` / `42301`。登录成功或失败均写入审计日志。

## 刷新 Token

`POST /api/auth/refresh`

```json
{
  "refreshToken": "<JWT>"
}
```

刷新成功会返回新的 Access Token 和新的 Refresh Token。旧 Refresh Token 在同一事务中被撤销，因此不能重复使用。

## 退出登录

`POST /api/auth/logout`

该接口需要携带 Access Token。请求体的 `deviceId` 指定要退出的设备：

```json
{
  "deviceId": "browser-installation-id"
}
```

退出登录会撤销该设备全部未撤销的 Refresh Token。已经签发的 Access Token 不保存服务端黑名单，仍可能使用至其 30 分钟有效期结束。

## 本人设备与会话管理

以下接口均需要包含设备标识的新 Access Token，只能查询和操作当前认证用户本人的设备。服务端不接受客户端提交用户 ID 作为数据范围。

### 设备列表

`GET /api/auth/devices`

按最后活跃时间倒序返回登录设备。IP 地址只返回脱敏值；`status` 根据是否仍有未撤销且未过期的 Refresh Token 计算。由于 HTTP 响应会将 Java `Long` 统一序列化为字符串，设备 `id` 必须按字符串处理。

```json
{
  "code": 0,
  "message": "success",
  "data": [
    {
      "id": "201",
      "deviceType": "WEB",
      "deviceName": "Chrome on Windows",
      "appVersion": null,
      "maskedIp": "192.168.*.*",
      "lastActiveAt": "2026-08-19T18:00:00",
      "createdAt": "2026-08-10T09:00:00",
      "status": "ACTIVE",
      "current": true,
      "sessionExpiresAt": "2026-08-26T18:00:00"
    }
  ]
}
```

### 退出指定设备

`POST /api/auth/devices/{deviceId}/revoke`

`deviceId` 为设备列表返回的内部设备 ID。接口会撤销该设备全部有效 Refresh Token、将设备标记为已撤销并写入 `DEVICE_REVOKE` 审计日志。重复调用保持成功幂等；目标设备不存在或不属于当前用户时统一返回 HTTP `404` / `40401`，避免泄露其他用户设备是否存在。

如果退出的是当前设备，客户端应在成功后立即清除本地 Access Token 和 Refresh Token 并返回登录页。

### 退出其他设备

`POST /api/auth/devices/revoke-others`

撤销当前用户除当前设备外的全部 Refresh Token，将其他设备标记为已撤销，并写入 `OTHER_DEVICES_REVOKE` 审计日志。当前设备依据签名 Access Token 中的服务端设备标识确定，不信任请求体参数。

设备撤销、普通退出和退出其他设备都不维护 Access Token 黑名单；相关设备已经签发的 Access Token 最长仍可能使用 30 分钟，但无法继续刷新。

## 当前用户

`GET /api/auth/me`

该接口需要 Access Token，返回 Token 中的用户 ID、用户名、角色、权限及 `mustChangePassword`。

## 修改本人密码

`PUT /api/auth/password`（微信小程序使用 `POST /api/auth/password`，请求体与校验相同）

该接口需要 Access Token。调用者必须提交当前密码和符合强密码策略的新密码；新密码不得与当前密码相同。

商家审核新建的账号首次登录时 `mustChangePassword=true`。服务端在该标记清除前仅允许访问 `/api/auth/me`、`/api/auth/password` 和 `/api/auth/logout`；改密成功后清除标记并撤销 Refresh Token，客户端应退出并要求使用新密码重新登录。

```json
{
  "currentPassword": "CurrentPassword123!",
  "newPassword": "NewPassword456!"
}
```

当前密码错误返回 HTTP `401` / `40102`；新密码不符合策略或与当前密码相同返回 HTTP `400` / `40001`。修改成功后会更新 BCrypt 密码哈希、撤销该账号全部 Refresh Token，并写入 `PASSWORD_CHANGE` 审计日志。客户端应立即清除本地 Access Token 和 Refresh Token，要求用户使用新密码重新登录。

由于当前 Access Token 为无状态 JWT，其他设备已经签发的 Access Token 最长仍可使用 30 分钟；其 Refresh Token 已失效，无法继续续期。

## 后台 RBAC 查询

以下接口均需要 Access Token，并在服务端检查权限；前端菜单隐藏不构成授权边界。

- `GET /api/admin/authorization/roles`：要求 `system:role:view`，返回有效角色及数据范围。
- `GET /api/admin/authorization/permissions`：要求 `system:permission:view`，返回有效权限编码。
- `GET /api/admin/authorization/users`：要求 `system:user:view`，按账号关键字和状态分页返回用户及其角色。
- `POST /api/admin/authorization/users`：要求 `system:user:create`，由系统管理员手动创建平台账号。
- `POST /api/admin/authorization/users/{userId}/credential-email`：要求 `system:user:create`，向仍须改密的账号重发临时密码。

`SUPER_ADMIN` 在 V2、V3、V9 迁移后默认拥有上述系统管理权限。`ADMIN` 目前只表示平台管理员身份，尚未默认授予系统管理权限；后续应按具体岗位配置权限，不应仅依赖角色名称放行接口。 SMTP 配置见 [system.md](system.md)。

### 管理端用户查询

`GET /api/admin/authorization/users?keyword=alice&status=ACTIVE&page=1&pageSize=20`

查询参数均可选，`page` 默认为 `1`，`pageSize` 默认为 `20` 且最大为 `50`。`keyword` 同时匹配用户名、邮箱和手机号；`status` 可为 `ACTIVE`、`DISABLED`、`LOCKED` 或 `PENDING_VERIFICATION`。邮箱和手机号仅返回脱敏值。

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "items": [
      {
        "userId": 101,
        "username": "alice_1",
        "maskedEmail": "a***@example.com",
        "maskedPhone": "138****5678",
        "status": "ACTIVE",
        "mustChangePassword": false,
        "roles": [
          { "id": 1, "code": "USER", "name": "普通用户", "dataScope": "SELF", "builtIn": true }
        ],
        "createdAt": "2026-08-10T09:00:00",
        "lastLoginAt": null
      }
    ],
    "total": 1,
    "page": 1,
    "pageSize": 20
  }
}
```

## 跨域访问

开发环境默认允许管理端 `http://localhost:5173`、用户 Web `http://localhost:5174` 及二者对应的 `127.0.0.1` 回环来源访问 API。部署或使用其他前端端口时，通过 `CORS_ALLOWED_ORIGINS` 配置精确的逗号分隔来源列表；接口不允许携带跨域 Cookie。

## 高风险角色变更

`PUT /api/admin/authorization/users/{userId}/roles`

该接口需要 `system:user:role:assign` 权限。除 Access Token 外，操作人必须再次提交自己的当前密码；密码错误返回 HTTP `401`。当前仅 `SUPER_ADMIN` 可调用该接口，且只能替换 `USER`、`ADMIN`、`SUPER_ADMIN` 三种平台角色；已有的商家员工与客服等业务角色会保留，并由后续 `merchant` 模块按店铺数据范围处理。系统会校验角色均存在且有效，并防止撤销最后一个超级管理员。

```json
{
  "roleIds": [5],
  "currentPassword": "Password123!"
}
```

无论成功还是密码确认失败，操作都会保留独立事务的审计日志。变更成功后会撤销目标用户所有 Refresh Token；目标用户已签发的 Access Token 最多仍可使用 30 分钟。

## 系统管理员手动建号

`POST /api/admin/authorization/users`

该接口需要 `system:user:create` 权限。邮箱必填；用户名和手机号可选。除 Access Token 外，操作人必须再次提交自己的当前密码。系统生成符合强密码策略的临时密码，将 `must_change_password` 置为 true，创建资料与偏好，并只分配 `USER`、`ADMIN`、`SUPER_ADMIN` 平台角色。临时密码只通过 SMTP 投递给邮箱，接口不返回明文。

```json
{
  "username": "staff_one",
  "email": "staff@example.com",
  "phone": "13800138000",
  "roleIds": [2],
  "currentPassword": "Password123!"
}
```

成功响应包含 `mailDeliveryStatus`：`SENT`、`MAIL_FAILED` 或 `SKIPPED`（SMTP 已关闭）。邮件失败或关闭发信均不回滚建号。首次登录后必须修改密码；在改密完成前，服务端只允许访问本人信息、改密和退出。SMTP 关闭时初始密码为 `123456QWERqwer!@`。

`POST /api/admin/authorization/users/{userId}/credential-email` 仅允许对仍标记 `mustChangePassword` 且已有邮箱的账号重发。重发会轮换临时密码并撤销该账号 Refresh Token。
