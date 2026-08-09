# 用户资料、地址与偏好接口（第一版）

以下接口均需携带 `Authorization: Bearer <accessToken>`，响应格式统一为 `{ "code": 0, "message": "success", "data": ... }`。服务端始终从 Token 中取得当前 `userId`，不接受客户端传入用户 ID。

## 用户资料

- `GET /api/users/me/profile`：获取当前用户资料。
- `PUT /api/users/me/profile`：更新当前用户资料。可更新字段为 `nickname`、`avatarUrl`、`realName`、`gender`（`UNKNOWN`、`MALE`、`FEMALE`）、`birthday` 和 `bio`；未提供的字段保持原值，空字符串会清空可选文本字段。

## 收货地址

- `GET /api/users/me/addresses`：按默认地址优先返回当前用户地址。
- `POST /api/users/me/addresses`：创建地址。首个地址自动成为默认地址；传入 `isDefault: true` 会替换当前默认地址。
- `PUT /api/users/me/addresses/{addressId}`：更新本人地址。
- `PUT /api/users/me/addresses/{addressId}/default`：设为本人默认地址。
- `DELETE /api/users/me/addresses/{addressId}`：删除本人地址。删除默认地址后，系统会将剩余地址中排序第一项设为默认地址。

地址不存在或不属于当前用户时统一返回 HTTP `404` / `40401`，避免泄露其他用户地址是否存在。

## 用户偏好

- `GET /api/users/me/preferences`：查询当前通知与展示偏好。
- `PUT /api/users/me/preferences`：完整更新 `marketingEnabled`、`orderNotificationEnabled`、`systemNotificationEnabled`，并可选传递 JSON 对象 `extraPreferences`。

## 店铺关注预留

V1 已预留 `shop_follow` 表，但商家／店铺模块尚未定义店铺实体、可关注状态与跨模块查询契约。因此本版本不提供关注或取消关注接口；后续接入前必须验证店铺存在且可关注。
