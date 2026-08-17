# 商品接口（成员 3）

统一响应 `{ "code": 0, "message": "success", "data": ... }`；雪花 ID 以字符串返回。商家/管理员接口需携带 `Authorization: Bearer <accessToken>` 并具备对应权限；公开读接口（类目树、商品详情、商品列表）无需认证。

## 1. 类目

### 公开
- `GET /api/categories/tree`：全量类目树（仅启用类目）。

### 管理端（权限 `category:manage`）
- `GET /api/admin/categories`：平铺类目列表。
- `POST /api/admin/categories`：新建类目。请求 `{ parentId, name, level, sortOrder, icon, status }`。
- `PUT /api/admin/categories/{categoryId}`：更新类目。
- `DELETE /api/admin/categories/{categoryId}`：删除类目（有子类目时报 `40020`）。

## 2. 商品（SPU / SKU）

### 公开
- `GET /api/spu/page?categoryId=&keyword=&page=&pageSize=`：上架商品分页。
- `GET /api/spu/{spuId}`：商品详情（仅 `ON_SALE`，否则 `40403`）。

### 商家（权限）
- `POST /api/merchant/spu`（`product:create`）：创建商品。请求：
  ```json
  {
    "shopId": "100",
    "categoryId": "2088558349228400641",
    "brand": "Apple",
    "name": "iPhone 17 Pro",
    "subtitle": "flagship",
    "mainImage": "https://...",
    "images": ["https://..."],
    "detail": "...",
    "skus": [
      { "skuCode": "IP17-BLK", "attributes": "{\"color\":\"black\"}", "price": 8999.00, "stock": 100 }
    ]
  }
  ```
  创建后状态为 `DRAFT`。`shopId` 当前由客户端提交，待成员 2 商家模块接入后由服务端从认证上下文解析。
- `PUT /api/merchant/spu/{spuId}`（`product:update`）：编辑商品基础信息。修改受审字段会使已上架商品回到 `DRAFT`。
- `POST /api/merchant/spu/{spuId}/sku`（`product:update`）：追加 SKU。
- `PUT /api/merchant/spu/{spuId}/status`（`product:update`）：状态流转。请求 `{ action, remark }`，`action ∈ SUBMIT | PUBLISH | OFF_SHELF`。
- `GET /api/merchant/sku/{skuId}`：SKU 详情。
- `PUT /api/merchant/sku/{skuId}`（`product:update`）：更新 SKU（编码/规格/图/价格）。
- `PUT /api/merchant/sku/{skuId}/stock`（`product:stock:adjust`）：调整库存。请求 `{ change, remark }`，`change` 正数增加、负数减少。

### 管理员（权限）
- `PUT /api/admin/spu/{spuId}/audit`（`product:audit`）：审核商品。请求 `{ result, remark }`，`result ∈ APPROVE | REJECT`。

## 3. SPU 状态机

`DRAFT → PENDING_AUDIT → AUDIT_APPROVED → ON_SALE → OFF_SALE`；驳回回 `AUDIT_REJECTED`；`DRAFT/AUDIT_REJECTED` 可重新提交。非法流转返回 `40021`。

## 4. 错误码

- `40402`：类目不存在。
- `40020`：类目存在子类目，不可删除。
- `40403`：商品不存在。
- `40021`：商品状态流转非法。
- `40404`：SKU 不存在。
- `40011`：库存不足。
