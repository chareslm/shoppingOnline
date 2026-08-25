# 支付与退款接口（第一版）

以下接口均需携带 `Authorization: Bearer <accessToken>`，响应格式统一为 `{ "code": 0, "message": "success", "data": ... }`。服务端始终从 Token 中取得当前 `userId`，不接受客户端传入用户 ID。

支付单状态：`0` 待支付 → `1` 已支付 → `4` 已退款（全额退款完成后）。退款单状态：`0` 已提交（待处理）→ `1` 退款成功。

> 雪花 ID 约定：`paymentOrderId`、`orderId`、`refundId`、`userId` 等 ID 字段统一以**字符串**返回（避免超出 JS Number 安全范围），请求体中提交时同样传字符串。

## 创建支付单

`POST /api/payments`

一单一付：一个订单对应一个支付单。

```json
{
  "orderId": "2087121585025634305"
}
```

成功响应：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "paymentOrderId": "2087121728781209602",
    "paymentNo": "PAY20260811181931325627",
    "orderId": "2087121585025634305",
    "userId": "1",
    "amount": 398.80,
    "payChannel": "MOCK_WECHAT",
    "status": 0,
    "payTime": null,
    "expireTime": "2026-08-11T18:48:57"
  }
}
```

`payChannel` 当前为 `MOCK_WECHAT`（模拟微信支付）。订单不存在或不属于当前用户返回 HTTP `404` / `40401`；订单状态不允许支付返回 HTTP `400` / `40012`。

## 模拟支付

`POST /api/payments/{paymentOrderId}/mock-pay`

本地开发用模拟支付，直接将支付单置为已支付并回调订单（等价于真实渠道支付成功的异步通知）。该路由默认不存在，只有显式设置 `TRADE_PAYMENT_MOCK_ENABLED=true` 才注册；即使开启也必须携带当前用户 Token 并校验支付单归属。成功响应 `status` 为 `1` 且带 `payTime`。支付单不存在或不属于当前用户返回 HTTP `404` / `40401`；已处理返回 HTTP `400` / `40014`。

## 查询支付单

`GET /api/payments/{paymentOrderId}`

支付单不存在或不属于当前用户时返回 HTTP `404` / `40401`。

## 申请退款

`POST /api/refunds`

对已支付的订单申请退款，退款金额不得超过订单已支付金额。

```json
{
  "orderId": "2087121585025634305",
  "amount": 398.80,
  "reason": "七天无理由退货"
}
```

`orderId` 必填，`amount`、`reason` 可选。成功返回 `data: null`。订单不存在或不属于当前用户返回 HTTP `404` / `40401`；订单状态不允许退款返回 HTTP `400` / `40012`。

## 查询退款列表

`GET /api/refunds`

返回当前用户的退款记录：

```json
{
  "code": 0,
  "message": "success",
  "data": [
    {
      "refundId": "2087122092918099970",
      "refundNo": "REF20260811182058194753",
      "orderId": "2087121585025634305",
      "paymentOrderId": "2087121728781209602",
      "amount": 398.80,
      "reason": "七天无理由退货",
      "status": 0,
      "refundTime": null,
      "createdAt": "2026-08-11T18:20:58"
    }
  ]
}
```

## 模拟退款完成

`POST /api/refunds/{refundId}/mock-complete`

本地演示使用，与模拟支付共用 `TRADE_PAYMENT_MOCK_ENABLED=true` 开关；默认配置下路由不存在。开启后仍必须携带当前用户的 Bearer Token，服务端校验退款单属于当前用户后才将其置为成功；其他用户的退款单统一返回 `40401`。匿名 `/api/mock/wechat/**` 回调已移除。

真实支付渠道回调尚未开放 HTTP 入口。后续接入微信支付时必须使用独立渠道路径，并完成平台证书验签、商户号与金额校验、时间窗口校验和幂等处理；不得恢复仅凭业务 ID 修改状态的匿名 Mock 接口。

## 错误码

- `40012`：订单状态不允许该操作。
- `40014`：支付单已处理（重复支付）。
- `40015`：支付单不存在。
- `40016`：退款单状态不允许。
- `40401`：资源不存在或不属于当前用户。
- `40101`：未认证；`40301`：无权限。

## 与订单状态机的联动

- 支付成功（认证 `mock-pay` 或后续验签渠道回调）→ 订单 `0` → `1`。
- 退款申请 → 订单 `1`/`2` → `6`（退款中）；认证模拟完成或后续验签渠道回调成功 → 退款单 `0` → `1`；累计退款达到实付金额 → 订单 `6` → `7`（退款完成）、支付单 `1` → `4`（已退款）；部分退款时订单保持 `6`、支付单保持 `1`。
- 超时未支付 → 定时任务关闭订单为 `5`（已关闭）。
