# 支付、退款与微信回调接口（第一版）

除回调接口外，以下接口均需携带 `Authorization: Bearer <accessToken>`，响应格式统一为 `{ "code": 0, "message": "success", "data": ... }`。服务端始终从 Token 中取得当前 `userId`，不接受客户端传入用户 ID。

支付单状态：`0` 待支付 → `1` 已支付。退款单状态：`0` 已提交（待处理）→ `1` 退款成功。

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

本地开发用模拟支付，直接将支付单置为已支付并回调订单（等价于真实渠道支付成功的异步通知）。成功响应 `status` 为 `1` 且带 `payTime`。支付单不存在或不属于当前用户返回 HTTP `404` / `40401`；已处理返回 HTTP `400` / `40014`。

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

## 微信支付回调（公开）

`POST /api/mock/wechat/callback/pay`

模拟微信支付成功通知，**无需认证**（回调来自支付渠道）。接口幂等：同一支付单重复回调不会重复处理。

```json
{
  "paymentOrderId": "2087121728781209602",
  "rawData": "渠道原始报文（模拟）"
}
```

支付单不存在返回 HTTP `400` / `40015`。处理成功返回 `data: null`。

## 微信退款回调（公开）

`POST /api/mock/wechat/callback/refund`

模拟微信退款结果通知，**无需认证**。幂等处理，将退款单置为退款成功（`status=1`）。

```json
{
  "refundId": "2087122092918099970",
  "rawData": "渠道原始报文（模拟）"
}
```

退款单不存在或状态不允许返回 HTTP `400` / `40016`。

## 错误码

- `40012`：订单状态不允许该操作。
- `40014`：支付单已处理（重复支付）。
- `40015`：支付单不存在。
- `40016`：退款单状态不允许。
- `40401`：资源不存在或不属于当前用户。
- `40101`：未认证；`40301`：无权限。

## 与订单状态机的联动

- 支付成功（mock-pay 或回调）→ 订单 `0` → `1`。
- 退款申请 → 订单 `1`/`2` → `6`（退款中）；退款回调成功 → 退款单 `0` → `1`（订单保持 `6`，退款完成后由商家端处理后续状态）。
- 超时未支付 → 定时任务关闭订单为 `4`。
