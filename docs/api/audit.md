# 管理端审计日志接口

## 权限与数据范围

接口统一要求 Bearer Token 和独立权限 `system:audit:view`。V11 迁移仅将该权限授予内置 `SUPER_ADMIN`，因此当前数据范围为平台全量审计日志；普通用户、普通管理员、商家及客服角色默认无权访问。若后续向其他管理角色开放，必须先定义对应数据范围，不能只依赖前端隐藏菜单。

审计日志为只追加安全记录，不提供修改或删除接口。响应中的 Java `Long` 均按字符串返回，包括日志 ID、操作者 ID 和分页 `total`。

## 分页查询

`GET /api/admin/audit-logs`

支持以下查询参数：

| 参数 | 必填 | 说明 |
| --- | --- | --- |
| `actorKeyword` | 否 | 操作者用户名模糊匹配，或用户 ID 精确匹配，最长 64 字符 |
| `module` | 否 | 模块精确匹配，例如 `AUTH`、`AUTHORIZATION` |
| `actionCode` | 否 | 动作编码精确匹配，例如 `PASSWORD_LOGIN`、`USER_ROLE_REPLACE` |
| `success` | 否 | `true` 或 `false` |
| `startAt` | 否 | ISO 本地日期时间，例如 `2026-08-20T00:00:00` |
| `endAt` | 否 | ISO 本地日期时间，不能早于 `startAt` |
| `page` | 否 | 默认 1，最小 1 |
| `pageSize` | 否 | 默认 20，范围 1—100 |

成功响应示例：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "items": [
      {
        "id": "166",
        "actorUserId": "29",
        "actorUsername": "admin_user",
        "module": "AUTHORIZATION",
        "actionCode": "USER_ROLE_REPLACE",
        "targetType": "USER",
        "targetId": "31",
        "success": true,
        "traceId": "trace-123",
        "requestMethod": "PUT",
        "requestPath": "/api/admin/authorization/users/31/roles",
        "maskedClientIp": "192.168.*.*",
        "client": "Microsoft Edge 151.0.0.0 on Windows",
        "detail": null,
        "createdAt": "2026-08-20T10:55:16.277"
      }
    ],
    "total": "1",
    "page": 1,
    "pageSize": 20
  },
  "traceId": null
}
```

## 脱敏规则

- IPv4 仅保留前两段，IPv6 仅保留首段；接口不返回完整来源 IP。
- User-Agent 只归纳浏览器/客户端名称、主版本信息和操作系统，不返回完整指纹字符串。
- `detail` 为 JSON 时递归检查字段名；密码、Token、Secret、Authorization、Cookie、Credential、私钥、手机号、邮箱、实名和地址相关字段统一返回 `***`。
- 无法解析的历史 `detail` 不返回原文，只显示“内容不可解析”。
- 请求仅保存 URI 路径，不保存查询字符串；不得在路径、动作编码或目标 ID 中写入密码、Token 或完整隐私信息。

认证、设备撤销和平台角色变更审计会同时记录请求方法、路径、来源 IP、User-Agent 与客户端提供的 `X-Trace-Id`。认证注册链路的审计与账号写入使用同一事务，避免新用户外键尚未提交时产生锁等待；针对既有账号的设备/授权高风险操作继续使用独立审计事务。
