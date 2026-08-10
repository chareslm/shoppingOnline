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

成功响应中的 `accessToken` 应通过 `Authorization: Bearer <accessToken>` 发送；有效期当前为 30 分钟。`refreshToken` 有效期为 7 天，只能提交给刷新接口，不能用于访问业务接口。

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
    "permissions": []
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

## 当前用户

`GET /api/auth/me`

该接口需要 Access Token，返回 Token 中的用户 ID、用户名、角色和权限。

## 修改本人密码

`PUT /api/auth/password`

该接口需要 Access Token。调用者必须提交当前密码和符合强密码策略的新密码；新密码不得与当前密码相同。

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

`SUPER_ADMIN` 在 V2、V3 迁移后默认拥有上述只读权限。`ADMIN` 目前只表示平台管理员身份，尚未默认授予系统管理权限；后续应按具体岗位配置权限，不应仅依赖角色名称放行接口。

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
