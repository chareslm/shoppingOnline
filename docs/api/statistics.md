# 统计精确查询接口

统计第一阶段直接从 MySQL 权威表执行只读聚合，不使用 Redis、Elasticsearch 或前端数据作为金额真相源。所有接口均需 Bearer Access Token，统一返回 `{ "code": 0, "message": "success", "data": ... }`。

## 统一查询规则

- `startAt`、`endAt` 必填，使用 `yyyy-MM-dd'T'HH:mm:ss`，按 `Asia/Shanghai` 解释。
- 时间范围统一为左闭右开 `[startAt, endAt)`；`startAt` 必须早于 `endAt`。
- `timezone` 可选，默认且仅允许 `Asia/Shanghai`。
- `granularity` 可选，第一版默认且仅允许 `DAY`。
- 一个查询最多覆盖 31 个业务自然日。
- 金额使用人民币元并以字符串返回；ID 及所有 Java `long` 计数按全局序列化规则返回字符串，前端不得用 JavaScript `number` 承载大计数。
- `metricVersion` 固定为 `v1`。`generatedAt` 是响应生成时间，`dataAsOf` 是本次精确查询的数据截止时间。

示例查询：

```text
?startAt=2026-08-01T00:00:00&endAt=2026-08-08T00:00:00&timezone=Asia/Shanghai&granularity=DAY
```

## 平台统计

权限：`statistics:platform:view`，V14 初始仅授予 `SUPER_ADMIN`。

### 平台概览

`GET /api/admin/statistics/platform/overview`

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "metricVersion": "v1",
    "timezone": "Asia/Shanghai",
    "generatedAt": "2026-08-25T14:30:00+08:00",
    "dataAsOf": "2026-08-25T14:30:00+08:00",
    "range": {
      "startAt": "2026-08-01T00:00:00",
      "endAt": "2026-08-08T00:00:00"
    },
    "metrics": {
      "newUsers": "12",
      "activeUsersSnapshot": "100",
      "paidOrderCount": "20",
      "paidBuyerCount": "16",
      "grossPaidAmount": "12680.00",
      "successfulRefundAmount": "380.00",
      "netCashflowActivity": "12300.00",
      "onSaleProductSnapshot": "50",
      "searchCount": "320",
      "displayedReviewCount": "18"
    }
  }
}
```

`newUsers` 只统计查询时仍持有 `USER` 角色且账号创建时间落在区间内的账号，避免把管理员和纯客服账号计入用户注册数。`activeUsersSnapshot` 同样只统计当前持有 `USER` 角色且状态为 `ACTIVE` 的账号。

支付总额包含状态 `1`（成功）和状态 `4`（全额退款后）的支付单，以保留原支付日 GMV；成功退款金额按退款成功时间统计，因此区间净收款活动额可以为负，不得命名为平台收入。

### 平台日趋势

`GET /api/admin/statistics/platform/trends`

响应元数据与概览一致，`points` 按业务日期升序并对缺失日期补零：

```json
{
  "metricVersion": "v1",
  "timezone": "Asia/Shanghai",
  "generatedAt": "2026-08-25T14:30:00+08:00",
  "dataAsOf": "2026-08-25T14:30:00+08:00",
  "range": {
    "startAt": "2026-08-01T00:00:00",
    "endAt": "2026-08-08T00:00:00"
  },
  "points": [
    {
      "date": "2026-08-01",
      "newUsers": "2",
      "paidOrderCount": "4",
      "paidBuyerCount": "3",
      "grossPaidAmount": "1200.00",
      "successfulRefundAmount": "50.00",
      "netCashflowActivity": "1150.00",
      "searchCount": "28"
    }
  ]
}
```

## 商家经营统计

权限：`statistics:shop:view`，V14 初始仅授予 `MERCHANT_OWNER`。服务端从当前认证用户解析已开通店铺，接口不接受客户端 `shopId`，客服账号无经营统计权限。

### 本店概览

`GET /api/merchant/statistics/overview`

除了统一元数据与时间范围，还返回服务端生效的 `shopId`、`shopName`：

```json
{
  "shopId": "1001",
  "shopName": "示例店铺",
  "metrics": {
    "paidOrderCount": "8",
    "paidBuyerCount": "7",
    "grossPaidAmount": "4200.00",
    "successfulRefundAmount": "100.00",
    "netCashflowActivity": "4100.00",
    "averageOrderValue": "525.00",
    "soldQuantity": "15",
    "onSaleProductSnapshot": "18",
    "displayedReviewCount": "32",
    "averageRating": "4.63"
  }
}
```

`displayedReviewCount` 和 `averageRating` 是查询时点快照；无评价时 `averageRating` 为 `null`。低库存 SKU 阈值尚未形成共享配置，因此第一版不返回低库存指标。

### 本店日趋势

`GET /api/merchant/statistics/trends`

`points` 每项包含 `date`、`paidOrderCount`、`paidBuyerCount`、`grossPaidAmount`、`successfulRefundAmount`、`netCashflowActivity` 和 `soldQuantity`，缺失日期补零。

## 错误与边界

- 未认证返回 HTTP `401` / `40101`。
- 无统计权限返回 HTTP `403` / `40301`。
- 商家账号无正常营业店铺返回 HTTP `403` / `40301`。
- 时间范围、时区、粒度或 31 日上限不合法返回 HTTP `400` / `40001`。
- 本阶段不提供导出、排行、用户本人统计、事件聚合或缓存接口。
