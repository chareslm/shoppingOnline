# 商品接口（成员 3）

统一响应 `{ "code": 0, "message": "success", "data": ... }`；雪花 ID 以字符串返回。商家/管理员接口需携带 `Authorization: Bearer <accessToken>` 并具备对应权限；公开读接口（类目树、商品详情、商品列表）无需认证。

## 1. 类目

### 公开
- `GET /api/categories/tree`：全量类目树（仅启用类目）。

### 管理端（权限 `category:manage`）
- `GET /api/admin/categories`：平铺类目列表。
- `POST /api/admin/categories`：新建类目。请求 `{ parentId, name, sortOrder, icon, status }`。`level` 由服务端按父类目计算（根为 1，最多 3 级）。
- `PUT /api/admin/categories/{categoryId}`：更新名称、排序、图标、启停。
- `DELETE /api/admin/categories/{categoryId}`：删除类目（有子类目时报 `40020`）。

## 2. 商品（SPU / SKU）

### 公开
- `GET /api/spu/page?categoryId=&keyword=&page=&pageSize=`：上架商品分页（含 `shopName`）。
- `GET /api/spu/{spuId}`：商品详情（仅 `ON_SALE`，否则 `40403`）。

### 商家（权限）
- `POST /api/merchant/spu`（`product:create`）：创建商品。店铺由当前登录商家的已开通店铺决定，不信任请求中的 `shopId`。请求：
  ```json
  {
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
  `skus[].attributes` 写入 MySQL JSON 列：须为 JSON 对象字符串，或服务端可解析的 `颜色:黑` / `颜色:黑,内存:256GB`。纯文本会存为 `{"规格":"..."}`。非法 JSON 对象返回 `40001`，不再 500。
  创建后状态为 `DRAFT`。无开通店铺时返回 `40301`。
- `POST /api/merchant/product-media`（`product:create` / `product:update`）：上传 JPEG/PNG 主图或详情图，单文件不超过 5MB。返回 `{ id, url }`，`url` 形如 `/api/product-media/{id}`，写入 `mainImage` / `images` / `detail`。
- `GET /api/product-media/{mediaId}`：公开读取商品图（不鉴权）。资质文件不得走此接口。
- `GET /api/merchant/spu/page`（`product:create` / `product:update` / `product:stock:adjust`）：**仅本店**商品分页，店铺由当前登录账号的已开通店铺（店主或已通过客服）决定，不信任查询参数中的店铺。列表项含 `shopId`、`shopName`。`shelf=LISTED` 仅已上架（`ON_SALE`），`shelf=UNLISTED` 为未上架。
- `GET /api/merchant/spu/{spuId}`：本店商品详情（含未上架，含 `shopName`）。非本店商品返回 `40301`。
- `PUT /api/merchant/spu/{spuId}`（`product:update`）：编辑商品基础信息。修改受审字段会使已上架商品回到 `DRAFT`。
- `POST /api/merchant/spu/{spuId}/sku`（`product:update`）：追加 SKU。
- `PUT /api/merchant/spu/{spuId}/status`（`product:update`）：商家状态流转。请求 `{ action, remark }`，`action ∈ SUBMIT | PUBLISH | OFF_SHELF`。`SUBMIT`：`DRAFT`/`AUDIT_REJECTED` → `PENDING_AUDIT`；`PUBLISH`：仅 `AUDIT_APPROVED`/`OFF_SALE` → `ON_SALE`（待审核不可自行上架）；`OFF_SHELF`：`ON_SALE` → `OFF_SALE`。
- `GET /api/merchant/sku/{skuId}`（`product:create` / `product:update` / `product:stock:adjust`）：本店 SKU 详情。非本店返回 `40301`。
- `PUT /api/merchant/sku/{skuId}`（`product:update`）：更新 SKU（编码/规格/图/价格）。
- `PUT /api/merchant/sku/{skuId}/stock`（`product:stock:adjust`）：调整本店 SKU 可售库存。请求 `{ change, remark }`，`change` 正数增加、负数减少。

### 管理员（权限 `product:audit`）
- `GET /api/admin/spu/page`：全平台商品分页，可按 `status`、`keyword` 过滤。列表项含 `shopId`、`shopName`，供审核队列展示所属店铺。
- `GET /api/admin/spu/{spuId}`：任意状态商品详情，含店铺名、类目名、主图/详情图、图文、SKU 规格/价格/库存与上次审核意见，供审核判断。
- `PUT /api/admin/spu/{spuId}/audit`：审核商品。请求 `{ result, remark }`，`result ∈ APPROVE | REJECT | REVOKE`。
  - `APPROVE`：`PENDING_AUDIT` / `AUDIT_REJECTED` / `AUDIT_APPROVED` → `ON_SALE`（通过即上架，也可对已收回商品重新通过）。
  - `REJECT`：`PENDING_AUDIT` → `AUDIT_REJECTED`。
  - `REVOKE`：`ON_SALE` / `AUDIT_APPROVED` / `OFF_SALE` → `AUDIT_REJECTED`（收回审核并下架）。

## 3. SPU 状态机

商家：`DRAFT` / `AUDIT_REJECTED` → `PENDING_AUDIT`。管理员 `APPROVE` 后进入 `ON_SALE`；商家可将 `ON_SALE` 下架为 `OFF_SALE`，再 `PUBLISH` 重新上架。管理员可 `REVOKE` 已通过商品至 `AUDIT_REJECTED`，再 `APPROVE` 重新上架。`AUDIT_APPROVED` 为历史中间态，新审核通过不再停留在该状态。非法流转返回 `40021`。

## 4. 错误码

- `40402`：类目不存在。
- `40020`：类目存在子类目，不可删除。
- `40403`：商品不存在。
- `40021`：商品状态流转非法。
- `40404`：SKU 不存在。
- `40011`：库存不足。
- `40032`：商品图片无效（类型或大小不符合）。
