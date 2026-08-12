# 购物车接口（第一版）

以下接口均需携带 `Authorization: Bearer <accessToken>`，响应格式统一为 `{ "code": 0, "message": "success", "data": ... }`。服务端始终从 Token 中取得当前 `userId`，不接受客户端传入用户 ID。

购物车按商家（`shopId`）分组；`skuId`、`shopId` 与 `price`（价格快照）当前由客户端提交，真实场景应在成员 3 的商品 / SKU 接口接入后由服务端校验并返回。价格快照仅用于结算展示，下单时以服务端校验为准。

## 查询购物车

`GET /api/cart`

成功响应：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "cartId": 2087107323255431169,
    "groups": [
      {
        "groupId": 2087107323255431170,
        "shopId": 1,
        "shopName": null,
        "items": [
          {
            "itemId": 2087107323255431171,
            "skuId": 101,
            "skuName": null,
            "skuImage": null,
            "price": 99.90,
            "quantity": 2,
            "checked": 1,
            "groupId": 2087107323255431170
          }
        ]
      }
    ]
  }
}
```

`checked` 为 `1`（勾选）或 `0`（未勾选）。`shopName`、`skuName`、`skuImage` 在成员 3 接入前为 `null`。

## 添加商品

`POST /api/cart/items`

```json
{
  "skuId": 101,
  "quantity": 2,
  "shopId": 1,
  "price": 99.90
}
```

`skuId`、`shopId`、`price` 必填，`quantity` 必填且 ≥ 1。重复添加同一 SKU 时累加数量。成功返回 `data: null`。

## 修改数量

`PUT /api/cart/items/{itemId}/quantity`

```json
{
  "quantity": 3
}
```

`quantity` 必填且 ≥ 1。

## 修改勾选状态

`PUT /api/cart/items/{itemId}/checked`

```json
{
  "checked": false
}
```

`checked` 必填，布尔值。

## 删除商品

`DELETE /api/cart/items/{itemId}`

删除当前用户购物车中的指定条目。条目不存在或不属于当前用户时统一返回 HTTP `404` / `40401`。

## 错误码

- `40001`：请求校验失败。
- `40401`：购物车条目不存在或不属于当前用户。
- `40101`：未认证。
- `40301`：无权限（正常用户流程不应出现）。
