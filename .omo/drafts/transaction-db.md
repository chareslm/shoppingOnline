# Draft: transaction-db

## Intent
- intent: CLEAR — 用户（成员 4）负责交易模块（购物车/订单/支付），要求规划数据库设计
- review_required: false
- 交付物（默认，待确认）：`docs/database/transaction-module.md` 设计文档 + `database/transaction/` SQL 建表脚本

## 已探索事实（证据）
- `README.md`：8 模块结构；backend=Java/Spring Boot（.gitignore 推断），deploy 含 MySQL/Redis/ES/Kibana/Filebeat
- `docs/architecture/module-ownership.md`：
  - 成员 4 = 交易：购物车、结算、订单、订单项、状态机、超时关闭、支付、回调、对账、退款
  - 约定：所有表含 created_at/updated_at/version；接口经 OpenAPI 维护于 docs/api/；ID 从认证上下文获取
- `docs/git-workflow.md`：feature/* 分支 + PR 流程
- 仓库无任何业务代码、无既有表结构、无需求文档 → 无既有 DB 约定可循

## 拓扑（Components ledger）
| id | 组件 | 状态 |
| --- | --- | --- |
| C1 | 购物车表设计（购物项、分组、价格快照、有效性校验） | 待决策 |
| C2 | 订单表设计（结算、订单、订单项、状态机、超时关闭） | 待决策 |
| C3 | 支付表设计（支付单、回调、对账、退款单） | 待决策 |
| C4 | 并发安全与一致性策略（乐观锁/行锁/幂等/对账） | 待决策 |
| C5 | 交付物（设计文档 + SQL 脚本 + 验证方式） | 待决策 |

## 待用户决策（owner-decisions）— 已全部确认
1. 主键策略：**雪花 ID**（BIGINT，全模块统一）
2. 支付对接：**模拟支付**（按微信支付接口字段设计，预留 out_trade_no/transaction_id/prepay_id）
3. 库存扣减：**下单预占 + 超时释放**（配合订单状态机与定时任务）

## 已采纳默认（可跳过问题）
- 金额精度：DECIMAL(10,2)（课程设计惯例）
- 购物车分组：按用户维度，购物项含商家/店铺分组（多商家平台）
- 超时关闭：定时任务扫描 + 状态机（Spring @Scheduled），预留 Redis 延迟队列扩展
- 并发安全：乐观锁 version（模块约定）+ 关键路径行锁/幂等键
- 表命名：snake_case，无前缀，模块语义命名（cart_item / order_info / payment_order / refund_order）
- 测试策略：SQL 脚本可执行验证 + 关键约束（唯一键/外键/状态机 CHECK）用例

## 状态
- status: approved（用户已回答全部 fork，授权生成计划）
- pending action: 运行 Metis 缺口审查 → 写 .omo/plans/transaction-db.md → 填充 todos + TL;DR