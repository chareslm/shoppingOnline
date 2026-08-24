# 统计模块指标、权限与聚合边界

> 状态：准备基线；当前不代表接口已经实现。
>
> 负责人：项目管理员（`statistics`、统一管理端统计框架与指标口径）。

## 1. 目标与当前结论

统计模块为平台治理和商家经营提供统一口径，不替代订单、支付、商品、商家等业务模块，也不修改这些模块的权威数据。

当前 `user`、`spu`、`sku`、`order`、`payment_order`、`refund_order`、`review` 和 `search_log` 已存在，但商家／店铺主体、账号—店铺关系及服务端店铺数据范围尚未落地；交易模块仍有已记录的价格归属、回调安全和跨店补偿问题。因此本阶段只固定指标、权限、数据范围、事件和聚合边界，不创建统计接口、聚合表或管理端页面，避免把不稳定的数据契约固化为长期口径。

统计功能满足下述准入条件后再进入实现：

1. 商家模块提供权威 `shop` 实体、店铺状态和“当前账号可管理店铺”查询契约。
2. 商品模块不再信任客户端提交的店铺归属，并明确销量字段由哪个模块、在什么时点更新。
3. 交易模块完成服务端价格与店铺归属校验、支付回调安全边界和跨店下单失败补偿。
4. 支付与退款模块确认成功状态、成功时间、部分退款及全额退款的最终语义。
5. 各来源模块确认事件发布时点和重放规则，或接受第一版由统计模块对权威表执行只读精确查询。

## 2. 统一统计规则

### 2.1 时间

- 默认业务时区固定为 `Asia/Shanghai`，接口不得使用浏览器本地时区推断统计日期。
- 时间范围统一使用左闭右开区间 `[startAt, endAt)`，避免相邻区间重复计算。
- 日粒度以业务时区自然日切分；响应同时返回 `timezone`、`generatedAt` 和 `dataAsOf`。
- 第一版单次趋势查询最多 31 个自然日；更长周期通过离线报表或预聚合查询。
- 事件按业务实际发生时间 `occurredAt` 归属日期，接收时间只用于延迟和补偿监控。

### 2.2 金额、数量与比例

- 金额单位统一为人民币元，使用 `DECIMAL` 计算并以 JSON 字符串返回，禁止使用浮点数。
- 比例在分母为 0 时返回 `null`，不把“无样本”显示为 0%。
- ID 按现有全局规则序列化为字符串。
- 每组指标返回 `metricVersion`；口径变化必须升级版本，不静默修改历史含义。
- 实时看板目标为分钟级最终一致；财务与对账数据只能来自离线精确计算，实时看板不得标记为财务结算结果。

### 2.3 去重与修正

- 订单数按店铺子订单 `order.id` 去重；当前模型中的一条 `order` 即一个店铺订单，不宣称它是平台总订单。
- 支付订单数按 `payment_order.id` 去重；买家数按 `payment_order.user_id` 去重。
- 匿名搜索当前没有稳定访客 ID，只统计次数，不计算匿名独立访客。
- 退款按成功退款单 `refund_order.id` 去重，失败、拒绝和待处理退款不计入成功退款金额。
- 迟到事件或业务修正必须可重复计算；预聚合使用唯一业务键和指标版本保证幂等。

## 3. 一期指标口径

### 3.1 平台治理指标

| 编码 | 指标 | v1 口径 | 权威来源 | 当前就绪度 |
| --- | --- | --- | --- | --- |
| `platform.new_users` | 新增注册用户数 | `user.created_at` 落在区间内的用户数，不因后续封禁回删 | `user` | 就绪 |
| `platform.active_users_snapshot` | 当前有效账号数 | 查询时点 `user.status = ACTIVE` 的账号数，仅为快照 | `user` | 就绪 |
| `platform.paid_order_count` | 支付订单数 | `pay_time` 落在区间内且曾成功支付的店铺订单数 | `payment_order`、`order` | 待交易契约稳定 |
| `platform.paid_buyer_count` | 支付买家数 | 上述支付订单对应 `user_id` 去重数 | `payment_order` | 待交易契约稳定 |
| `platform.gross_paid_amount` | 支付总额（GMV） | 按支付成功时间汇总原始成功支付金额；全额退款后仍保留在原支付日 | `payment_order` | 待支付语义确认 |
| `platform.successful_refund_amount` | 成功退款金额 | 按 `refund_time` 汇总 `refund_order.status = 1` 的金额 | `refund_order` | 待退款语义确认 |
| `platform.net_cashflow_activity` | 区间净收款活动额 | 同一区间支付总额减成功退款金额；退款可来自更早支付，因此不得称为收入且可能为负 | 支付、退款 | 待支付语义确认 |
| `platform.on_sale_product_snapshot` | 当前在售商品数 | 查询时点 `spu.status = ON_SALE` 的 SPU 数，仅为快照 | `spu` | 有条件就绪 |
| `platform.search_count` | 搜索次数 | 区间内 `search_log` 行数 | `search_log` | 就绪 |
| `platform.displayed_review_count` | 有效展示评价数 | 区间内创建且当前 `status = DISPLAYED` 的评价数 | `review` | 有条件就绪 |

`gross_paid_amount` 不是平台收入：当前系统没有佣金、平台服务费、税费和资金清算模型。平台页面和接口不得使用“营收”命名。

### 3.2 商家经营指标

所有商家指标必须先由服务端解析当前账号允许访问的店铺集合，再按权威 `shop_id` 过滤。请求中的 `shopId` 只用于缩小范围，不能扩大权限。

| 编码 | 指标 | v1 口径 |
| --- | --- | --- |
| `shop.paid_order_count` | 支付订单数 | 指定店铺、按支付成功时间统计的店铺订单数 |
| `shop.paid_buyer_count` | 支付买家数 | 指定店铺支付用户去重数 |
| `shop.gross_paid_amount` | 支付总额 | 指定店铺成功支付金额，退款不回写原支付日 |
| `shop.successful_refund_amount` | 成功退款金额 | 指定店铺按退款成功时间汇总的退款金额 |
| `shop.net_cashflow_activity` | 区间净收款活动额 | 区间支付总额减区间成功退款金额 |
| `shop.average_order_value` | 客单价 | `gross_paid_amount / paid_order_count`；分母为 0 时返回 `null` |
| `shop.sold_quantity` | 支付商品件数 | 成功支付订单项 `quantity` 之和；退款不回溯，另行展示退款金额 |
| `shop.on_sale_product_snapshot` | 当前在售商品数 | 查询时点指定店铺 `ON_SALE` SPU 数 |
| `shop.low_stock_sku_snapshot` | 低库存 SKU 数 | 查询时点启用 SKU 中 `available_stock <= threshold` 的数量；阈值由平台配置，v1 默认值须在实现前确认 |
| `shop.displayed_review_count` | 有效评价数 | 指定店铺当前可展示评价数 |
| `shop.average_rating` | 平均评分 | 指定店铺当前可展示评价的算术平均值；无样本返回 `null` |

退款件数、商品毛利、转化率、复购率、访客数和广告归因不进入 v1：当前模型缺少退款到订单项分摊、成本、统一访问会话及归因数据。

### 3.3 用户本人指标

用户指标只允许查询当前认证用户本人，不接受请求参数指定任意 `userId`。

| 编码 | 指标 | v1 口径 |
| --- | --- | --- |
| `user.paid_order_count` | 本人支付订单数 | 当前用户在区间内成功支付的店铺订单数 |
| `user.gross_paid_amount` | 本人支付总额 | 当前用户按支付成功时间汇总的原始支付金额 |
| `user.successful_refund_amount` | 本人成功退款金额 | 当前用户按退款成功时间汇总的成功退款金额 |
| `user.displayed_review_count` | 本人有效评价数 | 当前用户在区间内创建且目前仍展示的评价数 |

本人支付总额仅用于消费概览，不代表最终实际消费成本；跨期退款通过独立退款指标展示。用户统计在用户端商品／交易契约稳定后接入，不阻塞平台统计准备。

### 3.4 暂缓指标

- “总订单”跨店合并口径：当前缺少平台主订单／结算单标识。
- 支付转化率：缺少可信的结算曝光或访问会话分母。
- 退款率（支付批次口径）：需要把退款可靠归属到原支付批次，并明确观察窗口。
- 留存、日活和月活：当前只有登录审计，不能等价代表有效业务活跃。
- 商品利润、商家收入和平台收入：缺少成本、佣金、服务费和结算模型。
- 实时库存告警：需等待商品模块确认阈值配置与库存变更事件。

## 4. 权限与数据范围

计划使用下列独立权限，实际迁移在统计接口开始实现时创建：

| 权限编码 | 数据范围 | 初始授予角色 | 用途 |
| --- | --- | --- | --- |
| `statistics:self:view` | `SELF` | `USER` | 用户本人消费概览 |
| `statistics:platform:view` | `ALL` | `SUPER_ADMIN` | 平台总览、趋势与跨店排行 |
| `statistics:shop:view` | `SHOP` | `MERCHANT_OWNER`、经授权的 `MERCHANT_STAFF` | 本人可管理店铺的经营数据 |
| `statistics:report:export` | 与基础查看权限取交集 | 首期不默认授予 | 导出离线报表；属于敏感查询并写审计 |

授权必须同时满足“拥有权限”和“数据范围允许”：

1. `statistics:platform:view` 不能由 `SHOP` 数据范围角色使用。
2. 商家账号的店铺集合由 `merchant` 模块根据认证主体解析；统计模块不维护第二份账号—店铺关系。
3. `SUPER_ADMIN` 查询单店数据时可传 `shopId` 作为筛选；商家账号传入越权 `shopId` 统一返回 404，避免泄露店铺存在性。
4. 前端菜单、图表参数和隐藏按钮均不构成安全边界。
5. 导出、跨店排行和管理员敏感下钻记录审计，详情不得包含完整手机号、地址、支付报文或 Token。

## 5. 模块与数据边界

### 5.1 来源模块职责

- `user`：定义用户创建、状态变化的权威时间和状态。
- `merchant`：定义店铺、店铺状态、账号—店铺关系和服务端数据范围。
- `product`：定义商品状态、SKU 库存、销量及评价聚合更新边界。
- `trade`：定义店铺订单、订单项、支付时点相关订单状态及跨店拆单标识。
- `payment`：定义支付成功、退款成功和对账语义。
- `search`、`review`：定义搜索日志和有效评价口径。

来源模块不得把统计聚合逻辑塞入 Controller，也不得直接更新统计模块内部表。来源数据更正时应发布可重放的更正事件或提供确定性的重算依据。

### 5.2 统计模块职责

- 维护指标字典、版本、时间规则和数据范围校验。
- 消费标准业务事件或执行只读精确查询，生成平台／店铺统计读模型。
- 提供统一统计 API、趋势补零、排行、缓存和离线重算能力。
- 不直接修改 `user`、`shop`、`spu`、`sku`、`order`、支付、退款、评价或搜索日志。
- 不跨模块直接调用 Mapper；需要同步读取时使用来源模块暴露的查询服务，复杂离线聚合使用经评审的只读 SQL/XML Mapper。

## 6. 事件与聚合方案

### 6.1 标准事件信封

```json
{
  "eventId": "string",
  "eventType": "payment.succeeded",
  "schemaVersion": 1,
  "occurredAt": "2026-08-20T12:00:00+08:00",
  "sourceModule": "payment",
  "aggregateType": "PAYMENT_ORDER",
  "aggregateId": "string",
  "actorUserId": "string-or-null",
  "shopId": "string-or-null",
  "traceId": "string-or-null",
  "payload": {}
}
```

首批事件名称预留为：

- `user.registered`
- `payment.succeeded`
- `refund.succeeded`
- `order.completed`
- `product.published`、`product.off_shelf`
- `inventory.changed`
- `review.displayed`、`review.hidden`
- `search.performed`

事件在业务事务成功提交后发布。仅使用 Spring Application Event 时不能宣称可靠投递；涉及可恢复实时聚合前必须增加 Outbox，或保留按权威表重算的补偿路径。消费者以 `eventId + schemaVersion` 幂等，事件 payload 只包含统计需要的最小字段，不复制隐私数据和完整业务对象。

### 6.2 分阶段实现

1. **精确查询阶段**：数据量较小时直接从 MySQL 权威表执行只读聚合，作为指标正确性基准；不使用 Redis 或 Elasticsearch 作为金额真相源。
2. **日聚合阶段**：来源契约稳定后创建按业务日期、店铺和指标版本唯一的日汇总表；分钟级看板可增加短周期增量聚合。
3. **离线校正阶段**：定时按权威表重算最近日期，修正迟到退款和事件遗漏；记录 `dataAsOf` 与重算批次。
4. **报表阶段**：长周期查询和导出走离线报表，不阻塞在线交易库；对账始终使用支付／渠道精确数据。

## 7. API 与管理端预留

以下仅为路径和响应边界预留，正式契约在实现时写入 `docs/api/statistics.md`：

```text
GET /api/admin/statistics/platform/overview
GET /api/admin/statistics/platform/trends
GET /api/admin/statistics/shops/{shopId}/overview
GET /api/admin/statistics/shops/{shopId}/trends
GET /api/admin/statistics/shops/{shopId}/products/top
GET /api/users/me/statistics/overview
```

查询参数统一为 `startAt`、`endAt`、`timezone`、`granularity`；服务端校验范围并对缺失日期补零。响应至少包含 `metricVersion`、`timezone`、`generatedAt`、`dataAsOf` 和实际生效的数据范围。

统一管理端由项目管理员在 `system` 模块贡献统计路由和公共图表框架；商家经营统计仍复用该框架。页面只展示服务端已经完成权限过滤的数据，不在浏览器中下载平台全量数据后再筛选。

## 8. 开发准入与验收清单

- [ ] 商家模块提供权威店铺和账号—店铺查询服务，并覆盖跨店越权测试。
- [ ] 商品创建／修改中的店铺归属由服务端认证上下文或商家服务确定。
- [ ] 交易价格、店铺归属、支付回调和跨店补偿问题关闭。
- [ ] 支付与退款成功状态、发生时间和部分退款口径写入共享契约。
- [ ] 指标 SQL 使用 `[startAt, endAt)`、`Asia/Shanghai` 和 `DECIMAL`，并有边界时刻测试。
- [ ] 平台、商家、越权店铺和无权限账号的授权测试通过。
- [ ] 退款、迟到事件、重复事件、空区间和跨日补零测试通过。
- [ ] 指标响应带版本与数据截止时间；页面明确“实时估算”或“离线精确”。
- [ ] 导出与敏感下钻写审计并完成隐私脱敏。
- [ ] 真实 MySQL 数据与人工 SQL 对账一致后再接入缓存或预聚合。
