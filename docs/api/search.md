# 搜索接口（成员 3）

统一响应 `{ "code": 0, "message": "success", "data": ... }`；雪花 ID 以字符串返回。搜索接口公开，无需认证（未登录时热词统计不含用户维度）。

商品全文检索优先走 Elasticsearch（索引 `mall-product-v1`），ES 不可用时自动降级 MySQL LIKE，保证检索可用。

## 1. 商品检索

`GET /api/search`

| 参数 | 类型 | 说明 |
| --- | --- | --- |
| keyword | string | 关键词（匹配名称/副标题/品牌） |
| categoryId | string(long) | 类目过滤 |
| brand | string | 品牌过滤 |
| priceMin / priceMax | number | 价格区间 |
| sort | string | `DEFAULT` / `SALES_DESC` / `PRICE_ASC` / `PRICE_DESC` / `RATING_DESC` / `NEWEST` |
| page / pageSize | int | 分页（page≥1，pageSize≤50） |

响应 `data`：
```json
{
  "items": [
    {
      "spuId": "2088558420737089538",
      "shopId": "100",
      "categoryId": "2088558349228400641",
      "brand": "Apple",
      "name": "iPhone 17 Pro 256GB",
      "subtitle": "flagship smartphone",
      "mainImage": "https://...",
      "priceMin": 8999.00,
      "priceMax": 8999.00,
      "sales": 0,
      "rating": 0.00
    }
  ],
  "total": "1",
  "page": 1,
  "pageSize": 20
}
```

## 2. 搜索建议

`GET /api/search/suggest?keyword=ip`

返回近 7 天搜索日志中前缀匹配的关键词：`{ "suggestions": ["iphone"] }`。

## 3. 搜索热词

`GET /api/search/hot-words?limit=10`

返回近 7 天搜索频次 Top N：`{ "words": [ { "keyword": "iphone", "count": "2" } ] }`。

## 4. 索引重建（管理员，权限 `product:audit`）

`POST /api/admin/search/reindex`

全量读取 `ON_SALE` 商品写入 ES，返回 `{ "indexed": 123 }`。商品新增/上下架时通过 `ProductChangedEvent` 事务提交后异步增量同步 ES。
