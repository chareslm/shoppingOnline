# transaction-db - Work Plan

## TL;DR (For humans)
<!-- Fill this LAST, after the detailed plan below is written, so it summarizes the REAL plan. -->
<!-- Plain English for a non-engineer: NO file paths, NO todo numbers, NO wave/agent/tool names. -->

**What you'll get:** 交易模块（购物车/订单/支付）的完整数据库设计：一份设计文档 + 可直接执行的建表 SQL 脚本，含状态机、并发安全和跨模块边界说明。

**Why this approach:** 按你们团队分工文档的约定（所有表带 version 乐观锁、ID 从认证上下文获取），采用雪花 ID 保证 5 人并行开发不冲突；模拟支付但预留微信字段，课程设计可跑通全流程且未来可升级。

**What it will NOT do:** 不写任何 Java/Spring 业务代码；不建其他成员负责的表（用户/商家/商品/库存）；不做真实微信支付对接；不实现定时任务代码（只设计支撑超时关闭的表结构）。

**Effort:** Medium
**Risk:** Low - 纯数据库设计，无既有代码可破坏；唯一风险是与其他成员的表字段约定不一致，已在文档中标注边界。

**Decisions to sanity-check:** ① 订单按商家拆单（一个订单只属于一个店铺）② 状态机 8 态（待支付→已支付→已发货→已完成 + 取消/关闭/退款）③ 库存预占记录表独立于商品模块的库存表。

Your next move: 批准后由执行者创建文档与 SQL 脚本。

---

> TL;DR (machine): Medium effort, Low risk. Deliverables: docs/database/transaction-module.md + database/transaction/{001_cart,002_order,003_payment}.sql + README + verify.sql. 11 tables, snowflake IDs, mock payment, stock reservation.

## Scope
### Must have
- `docs/database/transaction-module.md`：交易模块数据库设计文档（ER 关系、12 张表字段定义、订单状态机、并发与一致性策略、跨模块边界）
- `database/transaction/001_cart.sql`：购物车 3 张表（cart / cart_group / cart_item）
- `database/transaction/002_order.sql`：订单 5 张表（order / order_item / stock_reservation / order_operation_log）
- `database/transaction/003_payment.sql`：支付 4 张表（payment_order / payment_record / refund_order / reconciliation_record）
- `database/transaction/README.md`：执行顺序、依赖说明（成员 1 的 user 表、成员 2 的 shop 表、成员 3 的 sku 表）
- 所有表：BIGINT 雪花 ID 主键、created_at/updated_at/version 三件套、utf8mb4、InnoDB
- 金额统一 DECIMAL(10,2)；订单号/支付单号/退款单号 VARCHAR(32) 唯一
- 状态字段用 TINYINT + CHECK 约束（MySQL 8.0.16+ 生效）

### Must NOT have (guardrails, anti-slop, scope boundaries)
- 不创建 user / shop / sku / stock 等属于其他成员的表（只引用其 ID，不建外键约束到未存在的表——用普通索引 + 文档说明）
- 不写任何 Java/Spring/定时任务/Redis 代码
- 不做真实微信支付对接（只预留字段）
- 不引入任何新依赖（纯 SQL + Markdown）
- 不修改 README.md / module-ownership.md / git-workflow.md 等既有文档
- 不创建 docs/api/ 下的 OpenAPI 文档（那是接口层，后续单独规划）

## Verification strategy
> Zero human intervention - all verification is agent-executed.
- Test decision: tests-after + 可执行 SQL 验证
- 验证方式：用 Docker 一次性 MySQL 8 容器执行全部脚本，然后跑约束/状态机/唯一键断言 SQL；若 docker 不可用，fallback 到本机 `mysql` 客户端（`mysql --version` 确认存在）
- Evidence: .omo/evidence/transaction-db/task-<N>-transaction-db.<ext>

## Execution strategy
### Parallel execution waves
- Wave 1: T1 设计文档（先行，SQL 脚本依赖其字段定义）
- Wave 2: T2 + T3 + T4 三个 SQL 脚本（并行，均依赖 T1 的字段定义）
- Wave 3: T5 README + 全量执行验证（依赖 T2/T3/T4）

### Dependency matrix
| Todo | Depends on | Blocks | Can parallelize with |
| --- | --- | --- | --- |
| T1 设计文档 | 无 | T2/T3/T4 | — |
| T2 001_cart.sql | T1 | T5 | T3, T4 |
| T3 002_order.sql | T1 | T5 | T2, T4 |
| T4 003_payment.sql | T1 | T5 | T2, T3 |
| T5 README + 验证 | T2/T3/T4 | — | — |

## Todos
> Implementation + Test = ONE todo. Never separate.
<!-- APPEND TASK BATCHES BELOW THIS LINE WITH edit/apply_patch - never rewrite the headers above. -->
- [x] 1. 编写交易模块数据库设计文档
  What to do: 创建 `docs/database/transaction-module.md`，包含：① 模块边界与依赖（引用成员1 user、成员2 shop、成员3 sku 的 ID，不建表）② ER 关系说明（cart→cart_group→cart_item；order→order_item/stock_reservation/order_operation_log；payment_order→payment_record/refund_order；reconciliation_record 独立）③ 12 张表完整字段清单（表名/字段/类型/约束/说明，与 SQL 脚本逐字段一致）④ 订单状态机：0待支付→1已支付→2已发货→3已完成；0→4已取消；0→5已关闭（超时）；1/2→6退款中→7已退款，附流转表 ⑤ 并发与一致性策略：乐观锁 version、支付回调幂等（payment_no 唯一 + payment_record 去重）、库存预占/释放（stock_reservation 状态机 0预占→1扣减/2释放）、超时关闭（定时任务扫描 status=0 AND expire_time<now，同一事务内关单+释放预占）⑥ 索引清单 ⑦ 命名约定（snake_case、无前缀、雪花 BIGINT）。
  Must NOT do: 不写业务代码；不编造其他模块的表结构；文档字段必须与 SQL 脚本一致。
  Parallelization: Wave 1 | Blocked by: 无 | Blocks: T2/T3/T4
  References: docs/architecture/module-ownership.md（成员4职责与跨模块约定）; README.md（模块结构）; .omo/drafts/transaction-db.md（已确认决策：雪花ID/模拟支付/下单预占）
  Acceptance criteria: 文件存在；包含全部 11 张表；每张表有字段清单；状态机章节含 8 个状态与流转；并发策略章节含幂等/预占/超时三小节；文档中所有表名与 T2/T3/T4 脚本表名一致（执行 `grep -c "CREATE TABLE" database/transaction/*.sql` 与文档表清单数量核对）。
  QA scenarios: happy: 用 Read 逐节核对 12 张表字段与 SQL 脚本一致；failure: 若文档缺状态机流转或字段与脚本不一致 → 修正后重验。Evidence: .omo/evidence/transaction-db/task-1-transaction-db.md
  Commit: Y | docs(database): 交易模块数据库设计文档

- [x] 2. 购物车建表脚本 `database/transaction/001_cart.sql`
  What: 创建 3 张表（utf8mb4/InnoDB/雪花 BIGINT 主键/created_at+updated_at+version）：
  ① `cart`：id PK、user_id BIGINT NOT NULL（引用成员1用户表，不建外键）、status TINYINT DEFAULT 1、UNIQUE KEY uk_user(user_id)
  ② `cart_group`：id PK、cart_id BIGINT NOT NULL、shop_id BIGINT NOT NULL（引用成员2店铺表）、status TINYINT DEFAULT 1、UNIQUE KEY uk_cart_shop(cart_id, shop_id)、INDEX idx_shop(shop_id)
  ③ `cart_item`：id PK、cart_id BIGINT NOT NULL、group_id BIGINT NOT NULL、sku_id BIGINT NOT NULL（引用成员3 SKU 表）、quantity INT NOT NULL DEFAULT 1 CHECK(quantity>0)、checked TINYINT DEFAULT 1、price_snapshot DECIMAL(10,2)（下单时价格快照，用于有效性校验）、status TINYINT DEFAULT 1、UNIQUE KEY uk_cart_sku(cart_id, sku_id)、INDEX idx_group(group_id)、INDEX idx_sku(sku_id)
  脚本末尾加注释说明：价格与有效性校验（sku 上下架/库存）由服务层调用成员3接口完成，表只存快照。
  Must NOT do: 不建 user/shop/sku 表；不加外键约束（跨模块表未建，外键会失败）；不写存储过程/触发器。
  Parallelization: Wave 2 | Blocked by: T1 | Blocks: T5
  References: T1 设计文档（docs/database/transaction-module.md 购物车章节）
  Acceptance criteria: 文件存在；`mysql` 或 docker 执行无语法错误；`SHOW CREATE TABLE cart` 含 uk_user 唯一键；cart_item 含 CHECK(quantity>0)。
  Verification: happy — docker run mysql:8 一次性容器执行脚本，`SHOW TABLES` 返回 3 张表；failure — 重复执行脚本应报"表已存在"（幂等性说明，不要求 IF NOT EXISTS）。Evidence: .omo/evidence/transaction-db/task-2-transaction-db.sql
  Commit: 1 | feat(database): 购物车建表脚本

- [x] 3. 订单建表脚本 `database/transaction/002_order.sql`
  What: 创建 4 张表：
  ① `order`：id PK、order_no VARCHAR(32) UNIQUE、user_id BIGINT NOT NULL、shop_id BIGINT NOT NULL、status TINYINT NOT NULL DEFAULT 0 CHECK(status BETWEEN 0 AND 7)、total_amount DECIMAL(10,2) NOT NULL、freight_amount DECIMAL(10,2) DEFAULT 0、discount_amount DECIMAL(10,2) DEFAULT 0、pay_amount DECIMAL(10,2) NOT NULL、receiver_name VARCHAR(50)、receiver_phone VARCHAR(20)、receiver_address VARCHAR(255)、remark VARCHAR(255)、pay_time DATETIME NULL、close_time DATETIME NULL、finish_time DATETIME NULL、cancel_reason VARCHAR(255)、INDEX idx_user(user_id)、INDEX idx_shop(shop_id)、INDEX idx_status(status)
  ② `order_item`：id PK、order_id BIGINT NOT NULL、sku_id BIGINT NOT NULL、sku_name VARCHAR(255)（快照）、sku_image VARCHAR(255)（快照）、price DECIMAL(10,2)（成交价快照）、quantity INT CHECK(quantity>0)、total_amount DECIMAL(10,2)、status TINYINT DEFAULT 0（0正常/1退款中/2已退款）、INDEX idx_order(order_id)、INDEX idx_sku(sku_id)
  ③ `stock_reservation`（库存预占，下单预占+超时释放核心表）：id PK、order_id BIGINT NOT NULL、sku_id BIGINT NOT NULL、quantity INT NOT NULL、status TINYINT DEFAULT 0（0预占中/1已扣减/2已释放）、expire_time DATETIME NOT NULL、INDEX idx_order(order_id)、INDEX idx_expire(status, expire_time)
  ④ `order_operation_log`（审计）：id PK、order_id BIGINT NOT NULL、operator_type TINYINT（1用户/2系统/3管理员）、operator_id BIGINT、action VARCHAR(50)（CREATE/PAY/CANCEL/CLOSE/SHIP/COMPLETE/REFUND）、from_status TINYINT、to_status TINYINT、remark VARCHAR(255)、INDEX idx_order(order_id)
  状态机注释写在 order 表定义上方：0待支付→1已支付→2已发货→3已完成；0→4已取消；0→5已关闭；1/2→6退款中→7已退款。
  Must NOT do: 不建 user/shop/sku 外键；不写触发器；不建 stock 表（库存属成员3，预占通过 stock_reservation 记录+服务层原子扣减）。
  Parallelization: Wave 2 | Blocked by: T1 | Blocks: T5
  Verification: 同 T2 方式执行；`SHOW CREATE TABLE order` 含 CHECK(status BETWEEN 0 AND 7)；插入 status=9 应报错（CHECK 生效）。
  Commit: 1 | feat(database): 订单建表脚本

- [x] 4. 支付建表脚本 `database/transaction/003_payment.sql`
  What: 创建 4 张表：
  ① `payment_order`：id PK、payment_no VARCHAR(32) UNIQUE、order_id BIGINT NOT NULL、user_id BIGINT NOT NULL、amount DECIMAL(10,2) NOT NULL、status TINYINT DEFAULT 0（0待支付/1成功/2失败/3已关闭/4已退款）、pay_channel VARCHAR(20) DEFAULT 'MOCK_WECHAT'、out_trade_no VARCHAR(64)（模拟=payment_no，预留微信）、transaction_id VARCHAR(64)（预留微信）、prepay_id VARCHAR(64)（预留）、pay_time DATETIME NULL、expire_time DATETIME NOT NULL、callback_time DATETIME NULL、callback_raw TEXT、UNIQUE KEY uk_order(order_id)、INDEX idx_user(user_id)
  ② `payment_record`（回调记录，幂等）：id PK、payment_order_id BIGINT NOT NULL、callback_type VARCHAR(20)（PAY/REFUND）、raw_data TEXT、status TINYINT DEFAULT 0（0待处理/1已处理/2重复）、process_result VARCHAR(255)、INDEX idx_payment(payment_order_id)
  ③ `refund_order`：id PK、refund_no VARCHAR(32) UNIQUE、payment_order_id BIGINT NOT NULL、order_id BIGINT NOT NULL、user_id BIGINT NOT NULL、amount DECIMAL(10,2) NOT NULL、reason VARCHAR(255)、status TINYINT DEFAULT 0（0待处理/1已退款/2失败/3已拒绝）、channel_refund_id VARCHAR(64)（预留）、refund_time DATETIME NULL、INDEX idx_order(order_id)、INDEX idx_payment(payment_order_id)
  ④ `reconciliation_record`（对账）：id PK、biz_date DATE NOT NULL、channel VARCHAR(20) DEFAULT 'MOCK_WECHAT'、total_amount DECIMAL(12,2)、total_count INT、diff_count INT DEFAULT 0、diff_amount DECIMAL(12,2) DEFAULT 0、status TINYINT DEFAULT 0（0待对账/1一致/2有差异）、detail JSON NULL、UNIQUE KEY uk_biz_date_channel(biz_date, channel)
  模拟支付说明注释：支付成功时服务层生成 transaction_id（模拟），回调写入 payment_record，幂等键 = payment_order_id + callback_type。
  Must NOT do: 不建真实微信对接；不建外键到 order（跨模块）；不写存储过程。
  Parallelization: Wave 2 | Blocked by: T1 | Blocks: T5
  Verification: 同 TASK 方式执行；`SHOW CREATE TABLE payment_order` 含 uk_order 唯一键；插入重复 payment_no 应报错。
  Commit: 1 | feat(database): 支付建表脚本

- [x] 5. README + 全量执行验证
  What: ① 创建 `database/transaction/README.md`：执行顺序（001→002→003）、依赖说明（user/shop/sku 表由成员1/2/3 提供，本模块只引用 ID）、模拟支付说明、状态机速查表。② 验证：用 docker 一次性 MySQL 8 容器（`docker run --rm -e MYSQL_ALLOW_EMPTY_PASSWORD=yes -v <repo>/database/transaction:/sql mysql:8 mysql -h127.0.0.1 -uroot < /sql/001_cart.sql` 依次执行 3 个脚本），然后执行断言 SQL：`SHOW TABLES` 应含 12 张表；`INSERT INTO order(status=9)` 应失败（CHECK）；`INSERT INTO cart_item(quantity=0)` 应失败；重复 payment_no 应失败；`SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='test'` = 12。若 docker 不可用，用本机 `mysql` 客户端等价执行。
  Must NOT do: 不修改 3 个 SQL 脚本内容（只验证）；不创建测试数据库之外的库。
  Parallelization: Wave 3 | Blocked by: T2/T3/T4 | Blocks: —
  Verification: 断言 SQL 全部通过；11 张表齐全；3 个非法插入全部被拒。
  Commit: 1 | docs(database): 交易模块建表说明与验证

## Final verification wave
> Runs in parallel after ALL todos. ALL must APPROVE. Surface results and wait for the user's explicit okay before declaring complete.
- [x] F1. Plan compliance audit — 5 个 todo 交付物全部存在，验收标准全部满足（11 张表/状态机/并发策略/CHECK/唯一键）
- [x] F2. Code quality review — SQL 实际执行通过；无外键到未建表；CHECK/唯一键齐全；命名与文档一致
- [x] F3. Real manual QA — MySQL 8.0.40 实际执行：11 张表创建成功；3 个非法用例被拒（3819/3819/1062）
- [x] F4. Scope fidelity — SHOW TABLES 确认仅 11 张交易模块表；未创建 user/shop/sku；未写业务代码；未引入依赖

## Commit strategy
- 每个 todo 完成后单独 commit（见各 todo Commit 行）
- 分支：从 develop 创建 `feature/transaction-db`（若 develop 不存在则从 main 创建）
- commit message 遵循仓库风格（docs:/feat: 前缀，中文描述）

## Success criteria
- [x] docs/database/transaction-module.md 存在且含 11 张表字段清单、状态机、并发策略
- [x] database/transaction/ 下 5 个文件（3 SQL + README + verify.sql）存在
- [x] 3 个 SQL 脚本在 MySQL 8 可执行，11 张表全部创建
- [x] 非法数据（status=9、quantity=0、重复 payment_no）被数据库拒绝
- [x] 未创建 user/shop/sku 表，未写业务代码