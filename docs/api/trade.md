# 订单接口（第一版）

以下接口均需携带 `Authorization: Bearer <accessToken>`，响应格式统一为 `{ "code": 0, "message": "success", "data": ... }`。服务端始终从 Token 中取得当前 `userId`，不接受客户端传入用户 ID。

订单状态机：`0` 待支付 → `1` 已支付（待发货）→ `2` 已发货（待收货）→ `3` 已完成（确认收货）；`4` 已取消（用户取消）；`5` 已关闭（超时未支付，由定时任务自动关闭）；`6` 退款中（申请退款后由 `1`/`2` 进入）；`7` 退款完成（累计退款达到实付金额后由 `6` 进入）。

> 雪花 ID 约定：`orderId`、`itemId`、`skuId` 等 ID 字段统一以**字符串**返回（避免超出 JS Number 安全范围），请求体中提交时同样传字符串。

## 创建订单（结算下单）

`POST /api/orders`

结算当前用户**全部勾选**的购物项，按商家拆单，每个商家生成一个订单。下单时通过 MySQL 单条条件更新原子预占 `sku.available_stock/reserved_stock`；所有店铺订单、订单项和库存预占处于同一个本地事务，任一店铺失败时整笔结算回滚，不留下前序店铺库存或半完成订单。内存 Mock 库存仅允许隔离测试显式启用，不是运行时默认值。

```json
{
  "receiverName": "张三",
  "receiverPhone": "13800001111",
  "receiverAddress": "北京市海淀区测试路 1 号",
  "remark": "请尽快发货"
}
```

四个字段均可选；`receiverName`、`receiverPhone`、`receiverAddress` 建议由收货地址带入（地址模块接口见 `user.md`）。

成功响应返回订单列表（按商家拆单，通常为一个元素）：

```json
{
  "code": 0,
  "message": "success",
  "data": [
    {
      "orderId": "2087121585025634305",
      "orderNo": "20260811181857978034",
      "status": 0,
      "payAmount": 398.80,
      "totalAmount": 398.80,
      "discountAmount": 0,
      "freightAmount": 0,
      "receiverName": "张三",
      "receiverPhone": "13800001111",
      "receiverAddress": "北京市海淀区测试路 1 号",
      "remark": "请尽快发货",
      "payTime": null,
      "finishTime": null,
      "closeTime": "2026-08-11T18:48:57",
      "items": [
        {
          "itemId": "2087121585025634306",
          "skuId": "101",
          "skuName": "示例商品",
          "skuImage": "https://example.com/img/101.png",
          "price": 99.90,
          "quantity": 2,
          "totalAmount": 199.80,
          "status": 0
        }
      ]
    }
  ]
}
```

无勾选购物项返回 HTTP `400` / `40010`；购物项 SKU 不存在、已停用或所属商品未上架返回 HTTP `400` / `40024`；库存不足返回 HTTP `400` / `40011`。下单成功后勾选购物项从购物车移除。下单金额以服务端当前售价为准（不信任客户端提交的价格）。

## 查询我的订单

`GET /api/orders`

返回当前用户全部订单（不含分页，后续版本视数据量调整）。

## 查询订单详情

`GET /api/orders/{orderId}`

订单不存在或不属于当前用户时返回 HTTP `404` / `40401`，避免泄露其他用户订单是否存在。

## 取消订单

`PUT /api/orders/{orderId}/cancel`

仅当订单处于 `0`（待支付）时可取消，取消后状态为 `4` 并释放已预占库存。其他状态返回 HTTP `400` / `40012`。

## 确认收货

`PUT /api/orders/{orderId}/confirm`

仅当订单处于 `2`（已发货）时可确认收货，确认后状态为 `3` 并记录完成时间。其他状态返回 HTTP `400` / `40012`。

> 注意：当前版本尚未暴露"发货"接口（`markShipped` 已在服务层实现，属商家端功能，待成员 4 接入）；因此已支付（`1`）的订单暂无法通过 HTTP 走到确认收货。

## 超时关单（管理端）

`POST /api/admin/tasks/order-timeout`

需要 `SUPER_ADMIN` 角色。定时任务默认每分钟自动执行一次；该接口用于演示或手动补跑，返回本次关闭的订单数量：

```json
{
  "code": 0,
  "message": "success",
  "data": { "closedCount": 1 }
}
```

非 `SUPER_ADMIN` 调用返回 HTTP `403` / `40301`。

## 错误码

- `40010`：无勾选购物项可结算。
- `40011`：库存不足。
- `40012`：订单状态不允许该操作。
- `40013`：订单不存在。
- `40024`：购物项 SKU 不存在、已停用或所属商品未上架。
- `40401`：订单不存在或不属于当前用户。
- `40101`：未认证；`40301`：无权限。
