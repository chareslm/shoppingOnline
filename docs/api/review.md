# 评价接口（成员 3）

统一响应 `{ "code": 0, "message": "success", "data": ... }`；雪花 ID 以字符串返回。评价列表/评分聚合公开；提交评价需登录；回复/审核需对应权限。

## 1. 提交评价（登录）

`POST /api/review`

```json
{
  "orderItemId": "2088558420800004097",
  "rating": 5,
  "content": "很好用",
  "images": ["https://..."],
  "anonymous": false
}
```

评价资格：订单项必须属于当前用户，且所属订单状态为「已完成」（`order.status = 3`）；同一订单项只能评价一次。

## 2. 商品评价列表（公开）

`GET /api/review/spu/{spuId}?page=&pageSize=`

返回 `DISPLAYED` 状态的评价（含商家回复）：

```json
{
  "items": [
    {
      "id": "2088558420800004098",
      "spuId": "2088558420737089538",
      "skuId": "2088558420800004097",
      "userId": "1",
      "rating": 5,
      "content": "很好用",
      "images": ["https://..."],
      "anonymous": false,
      "createdAt": "2026-08-15T17:28:26",
      "reply": "感谢支持"
    }
  ],
  "total": "1",
  "page": 1,
  "pageSize": 20
}
```

## 3. 商品评分聚合（公开）

`GET /api/review/spu/{spuId}/stats`

```json
{
  "averageRating": 5.00,
  "totalCount": "1",
  "fiveStar": "1",
  "fourStar": "0",
  "threeStar": "0",
  "twoStar": "0",
  "oneStar": "0",
  "positiveRate": 100.00
}
```

`positiveRate` 为好评率（4~5 星占比，百分比）。

## 4. 商家回复（权限 `review:reply`）

`PUT /api/merchant/review/{reviewId}/reply`

请求 `{ "content": "感谢支持" }`。一评价一回复，重复回复覆盖旧内容。

## 5. 评价审核（权限 `review:audit`）

`PUT /api/admin/review/{reviewId}/audit`

请求 `{ "action": "HIDE" | "DISPLAY" }`。隐藏评价后评分聚合与商品 `spu.rating` 同步更新。

## 6. 错误码

- `40022`：该订单项已评价。
- `40023`：评价资格不满足（订单非本人或未完成）。
- `40405`：评价不存在。
