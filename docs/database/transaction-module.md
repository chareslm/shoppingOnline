# 交易模块数据库设计（成员 4）

> 电商平台课程设计 · 交易模块（购物车 / 订单 / 支付）
> 本文档定义交易模块全部 11 张表的结构、状态机与并发一致性策略，是 `database/transaction/*.sql` 建表脚本的唯一依据。

## 1. 模块边界与依赖

本模块**只引用**其他成员负责的表的主键 ID，**不创建、不建立外键约束**到以下表（跨模块表可能尚未创建，外键会导致建表失败；引用关系用普通索引 + 服务层校验保证）：

| 被引用表 | 归属成员 | 引用字段 | 用途 |
| --- | --- | --- | --- |
| `user` | 成员 1（身份） | `user_id` | 购物车、订单、支付单归属；ID 从认证上下文获取，不接受客户端指定 |
| `shop` | 成员 2（商家） | `shop_id` | 购物车分组、订单归属店铺 |
| `sku` | 成员 3（商品） | `sku_id` | 购物项、订单项、库存预占指向的具体 SKU |

**约定**（来自 `docs/architecture/module-ownership.md`）：
- 所有表统一包含 `created_at` / `updated_at` / `version`（乐观锁）
- 用户 ID、商家 ID 从认证上下文获取，不接受客户端任意指定
- 高风险操作（退款审核、库存调整等）必须写入审计日志 → `order_operation_log`

## 2. ER 关系说明

```
cart 1 ── n cart_group 1 ── n cart_item
order 1 ── n order_item
order 1 ── n stock_reservation
order 1 ── n order_operation_log
order 1 ── 1 payment_order 1 ── n payment_record
payment_order 1 ── n refund_order
reconciliation_record（独立，按日对账）
```

- 一个用户一个购物车（`cart`），购物车按商家分组（`cart_group`），组内包含购物项（`cart_item`）
- 结算时按商家拆单：一个订单（`order`）只属于一个店铺，含多个订单项（`order_item`）
- 下单时创建库存预占记录（`stock_reservation`），超时关闭时释放
- 支付单（`payment_order`）与订单一对一；回调记录（`payment_record`）保证幂等；退款单（`refund_order`）挂在支付单下
- 对账记录（`reconciliation_record`）按日独立生成

## 3. 表结构清单（11 张）

> 通用约定：主键 `id BIGINT`（雪花 ID，应用层生成）；`created_at DATETIME`、`updated_at DATETIME`、`created_by BIGINT`、`updated_by BIGINT`、`version INT DEFAULT 0`（业务表五件套，与团队 BaseEntity 对齐；日志/对账表例外见各表）；引擎 InnoDB；字符集 utf8mb4；金额 `DECIMAL(10,2)`；状态 `TINYINT` + CHECK 约束（MySQL 8.0.16+ 生效）。

### 3.1 购物车模块（3 张）

#### cart（购物车）
| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | BIGINT | PK | 雪花 ID |
| user_id | BIGINT | NOT NULL, UNIQUE(uk_user) | 用户 ID（引用成员1 user 表） |
| status | TINYINT | DEFAULT 1 | 1 有效 / 0 停用 |
| created_at / updated_at / created_by / updated_by / version | — | 通用五件套 | — |

#### cart_group（购物车分组，按商家）
| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | BIGINT | PK | 雪花 ID |
| cart_id | BIGINT | NOT NULL, UNIQUE(uk_cart_shop) | 所属购物车 |
| shop_id | BIGINT | NOT NULL, UNIQUE(uk_cart_shop), INDEX(idx_shop) | 商家 ID（引用成员2 shop 表） |
| status | TINYINT | DEFAULT 1 | 1 有效 / 0 停用 |
| created_at / updated_at / created_by / updated_by / version | — | 通用五件套 | — |

#### cart_item（购物项）
| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | BIGINT | PK | 雪花 ID |
| cart_id | BIGINT | NOT NULL, UNIQUE(uk_cart_sku) | 所属购物车 |
| group_id | BIGINT | NOT NULL, INDEX(idx_group) | 所属分组 |
| sku_id | BIGINT | NOT NULL, UNIQUE(uk_cart_sku), INDEX(idx_sku) | SKU ID（引用成员3 sku 表） |
| quantity | INT | NOT NULL DEFAULT 1, CHECK(quantity>0) | 数量 |
| checked | TINYINT | DEFAULT 1 | 1 勾选结算 / 0 未勾选 |
| price_snapshot | DECIMAL(10,2) | NULL | 加入购物车时价格快照，结算时与最新价校验 |
| status | TINYINT | DEFAULT 1 | 1 有效 / 0 已移除 |
| created_at / updated_at / created_by / updated_by / version | — | 通用五件套 | — |

> **价格与有效性校验**：结算时服务层调用成员 3 的 SKU 接口校验上下架状态与最新价格，与 `price_snapshot` 对比；表只存快照，不承担实时校验。

### 3.2 订单模块（4 张）

#### order（订单主表）
| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | BIGINT | PK | 雪花 ID |
| order_no | VARCHAR(32) | NOT NULL, UNIQUE | 业务订单号 |
| user_id | BIGINT | NOT NULL, INDEX(idx_user) | 下单用户 |
| shop_id | BIGINT | NOT NULL, INDEX(idx_shop) | 店铺（按商家拆单） |
| status | TINYINT | NOT NULL DEFAULT 0, CHECK(status BETWEEN 0 AND 7) | 状态机，见 §4 |
| total_amount | DECIMAL(10,2) | NOT NULL | 商品总额 |
| freight_amount | DECIMAL(10,2) | DEFAULT 0 | 运费 |
| discount_amount | DECIMAL(10,2) | DEFAULT 0 | 优惠金额 |
| pay_amount | DECIMAL(10,2) | NOT NULL | 实付金额 = total + freight - discount |
| receiver_name | VARCHAR(50) | NULL | 收货人快照 |
| receiver_phone | VARCHAR(20) | NULL | 收货电话快照 |
| receiver_address | VARCHAR(255) | NULL | 收货地址快照 |
| remark | VARCHAR(255) | NULL | 订单备注 |
| pay_time | DATETIME | NULL | 支付时间 |
| close_time | DATETIME | NULL | 超时关闭时间 |
| finish_time | DATETIME | NULL | 完成时间 |
| cancel_reason | VARCHAR(255) | NULL | 取消原因 |
| created_at / updated_at / created_by / updated_by / version | — | 通用五件套 | — |

#### order_item（订单项）
| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | BIGINT | PK | 雪花 ID |
| order_id | BIGINT | NOT NULL, INDEX(idx_order) | 所属订单 |
| sku_id | BIGINT | NOT NULL, INDEX(idx_sku) | SKU ID |
| sku_name | VARCHAR(255) | NULL | 商品名快照 |
| sku_image | VARCHAR(255) | NULL | 商品图快照 |
| price | DECIMAL(10,2) | NULL | 成交单价快照 |
| quantity | INT | NOT NULL, CHECK(quantity>0) | 数量 |
| total_amount | DECIMAL(10,2) | NULL | 小计 = price × quantity |
| status | TINYINT | DEFAULT 0 | 0 正常 / 1 退款中 / 2 已退款 |
| created_at / updated_at / created_by / updated_by / version | — | 通用五件套 | — |

#### stock_reservation（库存预占记录）
> 下单预占 + 超时释放的核心表。实际库存扣减由服务层调用成员 3 的库存接口完成（原子 `UPDATE ... WHERE stock >= quantity`），本表只记录预占生命周期。

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | BIGINT | PK | 雪花 ID |
| order_id | BIGINT | NOT NULL, INDEX(idx_order) | 关联订单 |
| sku_id | BIGINT | NOT NULL | SKU ID |
| quantity | INT | NOT NULL | 预占数量 |
| status | TINYINT | DEFAULT 0 | 0 预占中 / 1 已扣减 / 2 已释放 |
| expire_time | DATETIME | NOT NULL | 预占过期时间（= 订单支付超时时间） |
| created_at / updated_at / created_by / updated_by / version | — | 通用五件套 | — |
| — | — | INDEX(idx_expire) | (status, expire_time) 供定时任务扫描 |

#### order_operation_log（订单操作日志 / 审计）
| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | BIGINT | PK | 雪花 ID |
| order_id | BIGINT | NOT NULL, INDEX(idx_order) | 关联订单 |
| operator_type | TINYINT | NOT NULL | 1 用户 / 2 系统 / 3 管理员 |
| operator_id | BIGINT | NULL | 操作者 ID |
| action | VARCHAR(50) | NOT NULL | CREATE / PAY / CANCEL / CLOSE / SHIP / COMPLETE / REFUND |
| from_status | TINYINT | NULL | 变更前状态 |
| to_status | TINYINT | NULL | 变更后状态 |
| remark | VARCHAR(255) | NULL | 备注 |
| created_at | DATETIME | NOT NULL | 操作时间（本表只记 created_at） |

### 3.3 支付模块（4 张）

#### payment_order（支付单）
| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | BIGINT | PK | 雪花 ID |
| payment_no | VARCHAR(32) | NOT NULL, UNIQUE | 支付单号（幂等键） |
| order_id | BIGINT | NOT NULL, UNIQUE(uk_order) | 关联订单（一单一付） |
| user_id | BIGINT | NOT NULL, INDEX(idx_user) | 支付用户 |
| amount | DECIMAL(10,2) | NOT NULL | 支付金额 |
| status | TINYINT | DEFAULT 0 | 0 待支付 / 1 成功 / 2 失败 / 3 已关闭 / 4 已退款 |
| pay_channel | VARCHAR(20) | DEFAULT 'MOCK_WECHAT' | 支付渠道（模拟微信） |
| out_trade_no | VARCHAR(64) | NULL | 微信商户订单号（模拟 = payment_no，预留） |
| transaction_id | VARCHAR(64) | NULL | 微信支付单号（模拟生成，预留） |
| prepay_id | VARCHAR(64) | NULL | 微信预支付 ID（预留） |
| pay_time | DATETIME | NULL | 支付成功时间 |
| expire_time | DATETIME | NOT NULL | 支付超时时间 |
| callback_time | DATETIME | NULL | 回调到达时间 |
| callback_raw | TEXT | NULL | 回调原始报文 |
| created_at / updated_at / created_by / updated_by / version | — | 通用五件套 | — |

#### payment_record（支付回调记录，幂等）
| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | BIGINT | PK | 雪花 ID |
| payment_order_id | BIGINT | NOT NULL, INDEX(idx_payment) | 关联支付单 |
| callback_type | VARCHAR(20) | NOT NULL | PAY / REFUND |
| raw_data | TEXT | NULL | 回调原始数据 |
| status | TINYINT | DEFAULT 0 | 0 待处理 / 1 已处理 / 2 重复 |
| process_result | VARCHAR(255) | NULL | 处理结果说明 |
| created_at | DATETIME | NOT NULL | 回调时间（本表只记 created_at） |

> **幂等策略**：同一支付单同一回调类型只处理一次——服务层先查 `payment_record` 是否已存在 `(payment_order_id, callback_type)` 已处理记录，存在则直接返回成功（标记 2 重复）。

#### refund_order（退款单）
| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | BIGINT | PK | 雪花 ID |
| refund_no | VARCHAR(32) | NOT NULL, UNIQUE | 退款单号 |
| payment_order_id | BIGINT | NOT NULL, INDEX(idx_payment) | 关联支付单 |
| order_id | BIGINT | NOT NULL, INDEX(idx_order) | 关联订单 |
| user_id | BIGINT | NOT NULL | 退款用户 |
| amount | DECIMAL(10,2) | NOT NULL | 退款金额 |
| reason | VARCHAR(255) | NULL | 退款原因 |
| status | TINYINT | DEFAULT 0 | 0 待处理 / 1 已退款 / 2 失败 / 3 已拒绝 |
| channel_refund_id | VARCHAR(64) | NULL | 渠道退款单号（预留） |
| refund_time | DATETIME | NULL | 退款完成时间 |
| created_at / updated_at / created_by / updated_by / version | — | 通用五件套 | — |

#### reconciliation_record（对账记录）
| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | BIGINT | PK | 雪花 ID |
| biz_date | DATE | NOT NULL, UNIQUE(uk_biz_date_channel) | 对账日期 |
| channel | VARCHAR(20) | NOT NULL DEFAULT 'MOCK_WECHAT', UNIQUE(uk_biz_date_channel) | 渠道 |
| total_amount | DECIMAL(12,2) | NULL | 渠道侧总金额 |
| total_count | INT | NULL | 渠道侧总笔数 |
| diff_count | INT | DEFAULT 0 | 差异笔数 |
| diff_amount | DECIMAL(12,2) | DEFAULT 0 | 差异金额 |
| status | TINYINT | DEFAULT 0 | 0 待对账 / 1 一致 / 2 有差异 |
| detail | JSON | NULL | 差异明细 |
| created_at / updated_at | — | 通用（无 version） | — |

> **对账逻辑**：每日任务对比当日 `payment_order`（成功单）与渠道侧数据，写入 `reconciliation_record`；有差异时 `status=2` 并记录 `detail`。

## 4. 订单状态机（8 态）

| 状态值 | 名称 | 说明 |
| --- | --- | --- |
| 0 | 待支付 | 下单成功，等待支付 |
| 1 | 已支付 | 支付成功，待发货 |
| 2 | 已发货 | 商家发货 |
| 3 | 已完成 | 确认收货 |
| 4 | 已取消 | 用户主动取消（仅待支付可取消） |
| 5 | 已关闭 | 超时未支付，系统关闭 |
| 6 | 退款中 | 退款申请处理中 |
| 7 | 已退款 | 退款完成 |

### 状态流转表

| from | to | 触发条件 | 操作者 |
| --- | --- | --- | --- |
| 0 | 1 | 支付回调成功 | 系统 |
| 0 | 4 | 用户取消订单 | 用户 |
| 0 | 5 | 超时未支付（expire_time < now） | 系统定时任务 |
| 1 | 2 | 商家发货 | 商家 |
| 2 | 3 | 用户确认收货 | 用户 |
| 1 | 6 | 申请退款 | 用户 |
| 2 | 6 | 申请退款 | 用户 |
| 6 | 7 | 退款成功 | 系统 |
| 6 | 1 / 2 | 退款拒绝，回到原状态 | 系统 |

> 每次状态变更必须写入 `order_operation_log`（action 对应 CREATE/PAY/CANCEL/CLOSE/SHIP/COMPLETE/REFUND）。

## 5. 并发与一致性策略

### 5.1 乐观锁
所有业务表含 `version INT`，更新时 `WHERE id=? AND version=?`，影响行数为 0 则重试或报并发冲突。

### 5.2 支付回调幂等
- `payment_no` 唯一约束兜底，重复创建支付单直接失败
- 回调处理：先查 `payment_record` 是否存在 `(payment_order_id, callback_type)` 已处理记录 → 存在则返回成功并标记重复；不存在则在同一事务内：写 `payment_record` + 更新 `payment_order.status=1` + 更新 `order.status=1` + 写 `order_operation_log`

### 5.3 库存预占与释放
- **下单**：事务内创建 `order` + `order_item` + `stock_reservation(status=0, expire_time=支付超时时间)`，并调用成员 3 库存接口原子预占（`UPDATE stock SET reserved=reserved+? WHERE sku_id=? AND stock-reserved>=?`）
- **支付成功**：`stock_reservation.status 0→1`（已扣减）
- **超时关闭**：定时任务扫描 `order.status=0 AND close_time<now` → 同一事务内：关单（0→5）+ 释放预占（0→2）+ 写操作日志；释放时调用成员 3 库存接口回补

### 5.4 超时关闭
- 定时任务（Spring @Scheduled，如每分钟）扫描 `order` 表 `status=0 AND close_time < NOW()`，批量处理
- 预留扩展：后续可换 Redis 延迟队列，表结构不变

### 5.5 对账
- 每日定时任务：汇总当日 `payment_order` 成功单（金额/笔数）与渠道侧对比，写入 `reconciliation_record`
- 差异单人工/系统复核，`detail` JSON 记录明细

## 6. 索引清单

| 表 | 索引 | 用途 |
| --- | --- | --- |
| cart | uk_user(user_id) | 一用户一购物车 |
| cart_group | uk_cart_shop(cart_id, shop_id) | 一车一商家一组；idx_shop 按商家查 |
| cart_item | uk_cart_sku(cart_id, sku_id) | 一车一 SKU 一项；idx_group / idx_sku |
| order | idx_user / idx_shop / idx_status | 用户订单、店铺订单、状态扫描 |
| order_item | idx_order / idx_sku | 订单项查询 |
| stock_reservation | idx_order / idx_expire(status, expire_time) | 超时释放扫描 |
| order_operation_log | idx_order | 订单审计查询 |
| payment_order | uk_order(order_id) / idx_user / payment_no UNIQUE | 一单一付、幂等 |
| payment_record | idx_payment | 回调幂等查询 |
| refund_order | idx_order / idx_payment / refund_no UNIQUE | 退款查询 |
| reconciliation_record | uk_biz_date_channel(biz_date, channel) | 一日一渠道一条 |

## 7. 命名约定

- 表名/字段名：snake_case，无前缀，语义命名
- 主键：`id BIGINT` 雪花 ID（应用层生成，非自增）
- 金额：`DECIMAL(10,2)`（对账表用 DECIMAL(12,2)）
- 时间：`DATETIME`；状态：`TINYINT` + CHECK 约束
- 通用三件套：`created_at` / `updated_at` / `version`（日志类表仅 `created_at`）

## 8. 与 SQL 脚本一致性

建表脚本 `database/transaction/001_cart.sql`（cart / cart_group / cart_item）、`002_order.sql`（order / order_item / stock_reservation / order_operation_log）、`003_payment.sql`（payment_order / payment_record / refund_order / reconciliation_record）必须与本文档字段定义逐字一致。表名清单（11 张）：

cart, cart_group, cart_item, order, order_item, stock_reservation, order_operation_log, payment_order, payment_record, refund_order, reconciliation_record