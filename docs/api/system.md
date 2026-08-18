# 系统管理接口（SMTP）

系统管理员在管理端保存运行时 SMTP 配置。查询不返回密码明文。未保存主机时，发信回退到环境变量 / Spring Mail。

所有接口需要 Access Token。前端菜单隐藏不构成授权边界。

## 查看 SMTP 配置

`GET /api/admin/system/smtp`

要求 `system:smtp:view`。`passwordConfigured` 仅表示是否已保存密码；`usingEnvironmentFallback` 为 true 时，当前发信仍使用环境变量。

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "host": "smtp.example.com",
    "port": 587,
    "username": "noreply@example.com",
    "fromAddress": "noreply@example.com",
    "smtpAuth": true,
    "starttlsEnabled": true,
    "passwordConfigured": true,
    "usingEnvironmentFallback": false
  }
}
```

## 更新 SMTP 配置

`PUT /api/admin/system/smtp`

要求 `system:smtp:update`。除 Access Token 外必须再次提交当前管理员密码；密码错误返回 HTTP `401` / `40102`。`password` 留空或省略时保留已保存密码。`fromAddress` 可选，允许与 SMTP 账号相同或使用显示名，不强制 RFC 邮箱格式。

```json
{
  "host": "smtp.example.com",
  "port": 587,
  "username": "noreply@example.com",
  "password": "authorization-code",
  "fromAddress": "noreply@example.com",
  "smtpAuth": true,
  "starttlsEnabled": true,
  "currentPassword": "Password123!"
}
```

成功响应与查询接口相同。保存后立即用于商家开通邮件和手动建号邮件，无需重启后端。端口为 465 或 994 时服务端使用隐式 SSL（TLS 1.2），并忽略 STARTTLS。网易 163/126 发件人若留空或不是邮箱，将使用 SMTP 账号。

## 发送测试邮件

`POST /api/admin/system/smtp/test`

要求 `system:smtp:update`，并再次提交当前管理员密码。`to` 可选；省略时发到已保存的发件人或 SMTP 账号。投递失败返回 HTTP `400` / `40031`，`message` 为可阅读的原因（认证失败、端口/TLS、发件人不一致等）。

```json
{
  "to": "name@163.com",
  "currentPassword": "Password123!"
}
```

