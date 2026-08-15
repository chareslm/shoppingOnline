# 商品 / 搜索 / 评价数据库设计（成员 3）

> 电商平台课程设计 · 商品模块（类目 / SPU / SKU / 状态流转）+ 评价模块 + 搜索日志
> 本文档定义成员 3 全部表结构、状态机与库存并发策略，是 `database/V5~V7__*.sql` 的唯一依据。

## 1. 模块边界与依赖

本模块**只引用**其他成员负责的表的主键 ID，**不建外键约束**（引用关系用普通索引 + 服务层校验）：

| 被引用表 | 归属成员 | 引用字段 | 用途 |
| --- | --- | --- | --- |
| `shop` | 成员 2（商家） | `shop_id` | SPU 归属店铺、评价归属店铺 |
| `user` | 成员 1（身份） | `user_id` / `operator_id` | 评价用户、操作审计 |
| `order` / `order_item` | 成员 4（交易） | `order_id` / `order_item_id` | 评价资格校验 |

**约定**（来自 `docs/architecture/module-ownership.md`）：
- 业务表统一包含 `created_at` / `updated_at` / `created_by` / `updated_by` / `version`（五件套）
- 主键 `id BIGINT` 雪花 ID（应用层生成，与交易模块引用 `sku_id` 的有符号 BIGINT 一致）
- 库存权威在 MySQL：`available_stock` / `reserved_stock` / `sold_stock` 三段库存

## 2. ER 关系说明

```
category（自关联树形结构：parent_id）
spu 1 ── n sku
spu 1 ── n product_status_log
spu 1 ── n review 1 ── 1 review_reply
search_log（独立，热词统计）
```

- 类目为树形结构（最多 3 层），SPU 挂载在叶子类目下
- 一个 SPU 多个 SKU（规格组合）；SKU 承载价格与库存
- 评价基于已完成订单项（`order_item`），一订单项一评价，一评价一商家回复

## 3. 表结构清单（8 张）

### 3.1 商品模块（4 张）

#### category（类目）
| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | BIGINT | PK | 雪花 ID |
| parent_id | BIGINT | NOT NULL DEFAULT 0 | 父类目，0 为根 |
| name | VARCHAR(64) | NOT NULL | 类目名称 |
| level | INT | NOT NULL DEFAULT 1 | 层级 1/2/3 |
| sort_order | INT | DEFAULT 0 | 排序值，越小越靠前 |
| icon | VARCHAR(512) | NULL | 图标 URL |
| status | TINYINT | DEFAULT 1 | 1 启用 / 0 停用 |
| 五件套 | — | — | created_at/updated_at/created_by/updated_by/version |

#### spu（商品）
| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | BIGINT | PK | 雪花 ID |
| shop_id | BIGINT | NOT NULL, idx_shop | 店铺（引用成员2） |
| category_id | BIGINT | NOT NULL, idx_category | 类目 |
| brand | VARCHAR(64) | NULL | 品牌 |
| name | VARCHAR(255) | NOT NULL, idx_name | 商品名称 |
| subtitle | VARCHAR(255) | NULL | 副标题 |
| main_image | VARCHAR(512) | NULL | 主图 |
| images | JSON | NULL | 轮播图数组 |
| detail | TEXT | NULL | 图文详情 |
| price_min / price_max | DECIMAL(10,2) | NULL | SKU 价格区间冗余 |
| sales | INT | DEFAULT 0 | 累计销量 |
| rating | DECIMAL(3,2) | DEFAULT 0 | 平均评分（评价回写） |
| status | VARCHAR(32) | NOT NULL DEFAULT 'DRAFT', CHECK | 状态机见 §4 |
| audit_remark | VARCHAR(255) | NULL | 审核备注/驳回原因 |
| 五件套 | — | — | — |

#### sku（库存保有单位）
| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | BIGINT | PK | 雪花 ID |
| spu_id | BIGINT | NOT NULL, idx_spu | 所属 SPU |
| sku_code | VARCHAR(64) | NULL, UNIQUE | 商家自定义编码 |
| attributes | JSON | NULL | 规格属性 |
| image | VARCHAR(512) | NULL | SKU 图 |
| price | DECIMAL(10,2) | NOT NULL, CHECK≥0 | 销售价 |
| available_stock | INT | NOT NULL DEFAULT 0, CHECK≥0 | 可售库存 |
| reserved_stock | INT | NOT NULL DEFAULT 0, CHECK≥0 | 预占库存 |
| sold_stock | INT | NOT NULL DEFAULT 0, CHECK≥0 | 已售库存 |
| status | TINYINT | DEFAULT 1 | 1 启用 / 0 停用 |
| 五件套 | — | — | — |

#### product_status_log（状态流转日志 / 审计）
| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | BIGINT | PK | 雪花 ID |
| spu_id | BIGINT | NOT NULL, idx_spu | 关联 SPU |
| operator_id | BIGINT | NULL | 操作者 ID |
| action | VARCHAR(32) | NOT NULL | SUBMIT/APPROVE/REJECT/PUBLISH/OFF_SHELF |
| from_status / to_status | VARCHAR(32) | NULL | 状态流转 |
| remark | VARCHAR(255) | NULL | 备注 |
| created_at | DATETIME | NOT NULL | 操作时间（仅 created_at） |

### 3.2 评价模块（2 张）

#### review（评价）
| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | BIGINT | PK | 雪花 ID |
| order_id | BIGINT | NOT NULL | 关联订单 |
| order_item_id | BIGINT | NOT NULL, UNIQUE(uk_review_order_item) | 关联订单项（评价资格） |
| spu_id / sku_id | BIGINT | NOT NULL, idx | 商品 |
| user_id | BIGINT | NOT NULL, idx_user | 评价用户 |
| shop_id | BIGINT | NOT NULL, idx | 店铺 |
| rating | TINYINT | NOT NULL, CHECK 1~5 | 评分 |
| content | VARCHAR(1000) | NULL | 评价内容 |
| images | JSON | NULL | 晒图数组 |
| is_anonymous | TINYINT | DEFAULT 0 | 1 匿名 / 0 实名 |
| status | VARCHAR(32) | DEFAULT 'DISPLAYED', CHECK | DISPLAYED/HIDDEN |
| 五件套 | — | — | — |

#### review_reply（商家回复）
| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | BIGINT | PK | 雪花 ID |
| review_id | BIGINT | NOT NULL, UNIQUE | 一评价一回复 |
| shop_id | BIGINT | NOT NULL | 店铺 |
| content | VARCHAR(1000) | NOT NULL | 回复内容 |
| replied_by | BIGINT | NULL | 回复人 ID |
| 五件套 | — | — | — |

### 3.3 搜索模块（1 张，索引在 ES）

#### search_log（搜索日志，热词统计）
| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | BIGINT | PK | 雪花 ID |
| keyword | VARCHAR(128) | NOT NULL, idx | 关键词（小写归一） |
| user_id | BIGINT | NULL | 搜索用户，未登录 NULL |
| created_at | DATETIME | NOT NULL, idx | 搜索时间（仅 created_at） |

> 商品全文检索索引在 Elasticsearch `mall-product-v1`，由 `ProductSearchDocument` 定义；MySQL 的 `spu` 为数据主源，ES 仅作搜索索引（可秒级最终一致）。ES 挂掉时检索降级 MySQL LIKE。

## 4. SPU 状态机（6 态）

| 状态 | 名称 | 说明 |
| --- | --- | --- |
| DRAFT | 草稿 | 商家新建/编辑后 |
| PENDING_AUDIT | 待审核 | 提交审核 |
| AUDIT_APPROVED | 审核通过 | 管理员通过，待上架 |
| AUDIT_REJECTED | 审核驳回 | 管理员驳回 |
| ON_SALE | 上架中 | 可搜索、可购买 |
| OFF_SALE | 已下架 | 商家下架 |

状态流转：`DRAFT → PENDING_AUDIT → AUDIT_APPROVED → ON_SALE → OFF_SALE → ON_SALE`；`PENDING_AUDIT` 可驳回至 `AUDIT_REJECTED`；`DRAFT/AUDIT_REJECTED` 可重新提交。修改受审字段（名称/主图/详情等）会使审核结果失效，已上架/待上架商品回到 `DRAFT`。每次流转写 `product_status_log`。

## 5. 库存并发策略

- 三段库存：`available_stock`（可售）、`reserved_stock`（预占）、`sold_stock`（已售）
- **下单预占**：`UPDATE sku SET available_stock=available_stock-?, reserved_stock=reserved_stock+? WHERE id=? AND status=1 AND available_stock>=?`（单条条件 UPDATE，禁止先查再改）
- **支付扣减**：`reserved_stock-?`、`sold_stock+?`，条件 `reserved_stock>=?`（预占已在预占时扣减，此处只把预占转为已售）
- **超时释放**：`reserved_stock-?`、`available_stock+?`
- 实现：`product/client/MysqlStockClient` 实现 `trade/client/StockClient`，通过 `trade.stock.mock-enabled=false` 启用（默认仍用交易模块 `MockStockClient` 内存模拟）

## 6. 索引清单

| 表 | 索引 | 用途 |
| --- | --- | --- |
| category | idx_parent / idx_level_sort | 树形查询 |
| spu | idx_shop / idx_category / idx_status / idx_name | 商家/类目/状态/名称检索 |
| sku | uk_sku_code / idx_spu / idx_status | 编码唯一、SPU 展开 |
| product_status_log | idx_spu | 流转审计 |
| review | uk_review_order_item / idx_spu / idx_sku / idx_user / idx_shop_status | 评价资格、聚合、用户/店铺查询 |
| review_reply | uk_reply_review / idx_shop | 一评价一回复 |
| search_log | idx_keyword_created / idx_created | 热词统计 |

## 7. 与 SQL 脚本一致性

建表脚本 `database/V5__product_tables.sql`（category / spu / sku / product_status_log + 商品权限）、`V6__review_tables.sql`（review / review_reply + 评价权限）、`V7__search_tables.sql`（search_log）必须与本文档字段定义一致。表名清单（8 张）：

category, spu, sku, product_status_log, review, review_reply, search_log（+ ES 索引 mall-product-v1）
