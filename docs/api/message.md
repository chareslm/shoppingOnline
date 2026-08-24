# 消息中心模块 API 接口文档

> 模块：message（用户站内信 + 通知偏好）
> 统一前缀：`/api/message/notifications`
> 鉴权：所有接口需携带 `Authorization: Bearer {jwt}`

---

## 一、站内信

### 1.1 查询我的通知列表

- **GET** `/api/message/notifications`
- **Query 参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| category | Integer | 否 | 通知分类: 1系统/2订单/3营销/4客服 |
| page | int | 否 | 页码，默认1 |
| pageSize | int | 否 | 每页大小，默认20 |

**成功响应**：
```json
{
  "code": 0,
  "data": [
    {
      "id": 10001,
      "templateId": 1,
      "templateCode": "ORDER_PAID",
      "title": "订单支付成功",
      "content": "您的订单 ORDER20250819 已支付成功, 金额 99.00 元",
      "category": 2,
      "categoryDesc": "订单通知",
      "bizType": "ORDER",
      "bizId": "ORDER20250819",
      "isRead": 0,
      "readTime": null,
      "pushStatus": 1,
      "pushTime": "2025-08-19T10:00:00",
      "createdAt": "2025-08-19T10:00:00"
    }
  ]
}
```

---

### 1.2 获取未读通知数量

- **GET** `/api/message/notifications/unread-count`
- **响应**：`Integer` 未读数量

---

### 1.3 标记单条通知为已读

- **PUT** `/api/message/notifications/{notificationId}/read`

---

### 1.4 批量标记通知为已读

- **PUT** `/api/message/notifications/read-batch`

**请求体**：
```json
{
  "notificationIds": [10001, 10002, 10003]
}
```

---

### 1.5 标记全部通知为已读

- **PUT** `/api/message/notifications/read-all`

---

## 二、通知偏好

### 2.1 获取我的通知偏好

- **GET** `/api/message/notifications/preference`

**响应**：
```json
{
  "id": 1,
  "userId": 20001,
  "systemEnabled": 1,      // 系统通知开关
  "orderEnabled": 1,       // 订单通知开关
  "marketingEnabled": 0,   // 营销通知开关
  "serviceEnabled": 1      // 客服消息开关
}
```

---

### 2.2 更新我的通知偏好

- **PUT** `/api/message/notifications/preference`

**请求体**：
```json
{
  "systemEnabled": 1,
  "orderEnabled": 1,
  "marketingEnabled": 1,
  "serviceEnabled": 0
}
```

---

## 三、错误码

| 错误码 | 说明 |
|--------|------|
| 40019 | 通知偏好不存在 |
| 40401 | 通知不存在或无权限 |
