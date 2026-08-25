# 聊天模块 API 接口文档

> 模块：chat（客服聊天会话 + 消息收发）
> 统一前缀：`/api/chat`
> 鉴权：所有接口需携带 `Authorization: Bearer {jwt}`

---

## 一、会话管理

### 1.1 创建会话

- **POST** `/api/chat/sessions`
- **描述**：用户发起客服会话，若已有进行中会话则直接返回
- **权限**：普通用户 USER / 客服 CUSTOMER_SERVICE

**请求体**：
```json
{
  "shopId": "1001",        // 必填，目标店铺ID
  "subject": "关于订单问题",  // 可选，会话主题
  "firstMessage": "你好"     // 可选，首条消息
}
```

`shopId` 必须对应正常营业的店铺；不存在、暂停、冻结或关闭的店铺统一返回 `40401`。服务端不会仅凭客户端提交的店铺 ID 授予任何管理权限。

**成功响应**：
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "sessionId": 10001,
    "userId": 20001,
    "shopId": 1001,
    "csUserId": null,
    "subject": "关于订单问题",
    "lastMessage": "你好",
    "lastMessageTime": "2025-08-19T10:00:00",
    "status": 0,
    "priority": 0,
    "createdAt": "2025-08-19T10:00:00",
    "unreadCount": 0
  }
}
```

---

### 1.2 查询我的会话列表

- **GET** `/api/chat/sessions`
- **描述**：当前登录用户作为发起方的会话列表
- **权限**：USER / CUSTOMER_SERVICE
- **Query 参数**：无

---

### 1.3 客服工作台会话列表

- **GET** `/api/chat/sessions/cs`
- **描述**：客服查看已分配 + 待分配的会话
- **权限**：CUSTOMER_SERVICE

服务端先根据当前客服账号解析其 `ACTIVE` 店铺关系，只返回本店会话；不得查看或领取其他店铺的待分配会话。

---

### 1.4 获取会话详情

- **GET** `/api/chat/sessions/{sessionId}`
- **Path 参数**：`sessionId` - 会话ID
- **权限**：会话参与者

---

### 1.5 客服领取会话

- **PUT** `/api/chat/sessions/{sessionId}/assign`
- **描述**：将未分配会话分配给当前客服
- **权限**：CUSTOMER_SERVICE

---

### 1.6 结束会话

- **PUT** `/api/chat/sessions/{sessionId}/close`
- **描述**：用户或客服结束会话
- **权限**：会话参与者

---

### 1.7 获取会话未读数

- **GET** `/api/chat/sessions/{sessionId}/unread`
- **权限**：会话参与者
- **响应**：`Integer` 未读消息数

---

## 二、消息收发

### 2.1 发送消息

- **POST** `/api/chat/messages/{sessionId}`
- **描述**：向指定会话发送消息
- **权限**：会话参与者

**请求体**：
```json
{
  "content": "你好，请问这个商品有货吗？",
  "msgType": 1,              // 1文本/2图片/3商品卡片/4系统通知
  "extraData": {              // 可选，商品卡片等扩展数据
    "productId": 30001,
    "productName": "蓝牙耳机"
  }
}
```

---

### 2.2 查询会话消息列表

- **GET** `/api/chat/messages/{sessionId}`
- **Query 参数**：`page`(默认1), `pageSize`(默认50)
- **描述**：分页获取历史消息，按时间正序
- **权限**：会话参与者

---

### 2.3 拉取离线消息

- **GET** `/api/chat/messages/{sessionId}/offline`
- **Query 参数**：`lastMessageId` (可选)
- **描述**：断线重连后从指定消息ID之后补拉新消息
- **权限**：会话参与者

---

### 2.4 标记消息已读

- **PUT** `/api/chat/messages/{sessionId}/read`
- **请求体**：消息ID数组 `[1001, 1002, 1003]`
- **描述**：标记对方发来的消息为已读
- **权限**：会话参与者

---

### 2.5 撤回消息

- **DELETE** `/api/chat/messages/{messageId}`
- **描述**：发送方本人在5分钟内可撤回
- **错误码**：`40017` 撤回时间窗口已过期

---

## 三、WebSocket 实时推送

### 获取一次性连接票据

- **POST** `/api/chat/websocket-ticket`
- **鉴权**：Bearer Access Token
- **说明**：返回 30 秒有效、只能消费一次的随机票据。Access Token 不得放入 WebSocket URL。

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "ticket": "single-use-ticket",
    "expiresAt": "2026-08-25T12:00:30Z"
  }
}
```

### 连接

- **URL**：`ws://host/ws/chat?ticket={single-use-ticket}`
- **描述**：客户端连上后即可接收实时消息推送

缺失、过期、伪造或重复使用的票据在握手阶段返回 `401`。允许的 WebSocket Origin 与 HTTP CORS 白名单一致。客户端写消息仍使用认证 HTTP API，WebSocket 只负责服务端推送。

### 推送消息格式

```json
{
  "type": "MESSAGE_SENT",
  "data": {
    "messageId": 10001,
    "sessionId": 20001,
    "senderId": 30001,
    "senderType": 1,
    "content": "你好",
    "msgType": 1,
    "createdAt": "2025-08-19T10:00:00"
  }
}
```

| type | 说明 |
|------|------|
| `MESSAGE_SENT` | 新消息推送 |
| `MESSAGE_READ` | 消息已读通知 |
| `SESSION_CLOSED` | 会话关闭通知 |
