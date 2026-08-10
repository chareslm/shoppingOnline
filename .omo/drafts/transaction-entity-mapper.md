# Draft: transaction-entity-mapper

## Intent
- intent: CLEAR — 用户（成员4）要求做交易模块「实体类 + Mapper 层」，方案 A（表加 created_by/updated_by 列）
- review_required: false
- 团队状态：尚未开工（其他成员未写代码），现在做无冲突

## 已探索事实（证据）
- 脚手架：Spring Boot 4.1 + Java 21 + MyBatis-Plus 3.5.17 + Security/JWT，模块目录 cart/trade/payment 已建（仅 package-info.java）
- `BaseEntity`（common/model）：id(ASSIGN_ID 雪花)、createdAt/updatedAt(FieldFill)、createdBy/updatedBy(FieldFill)、version
- **缺口**：common 包无 MetaObjectHandler → FieldFill 自动填充不生效，需补
- `@MapperScan("com.chareslm.shopping.**.mapper")` 已配置 → Mapper 放各模块 mapper 子包
- `CurrentUser.require()` → LoginUser(userId, username, roles, permissions)，userId 从认证上下文获取
- `ApiResponse<T>(code, message, data, traceId)` record；ErrorCode 仅通用 6 个
- `application-local.yml.example`：DB 连接格式 + JWT_SECRET（≥32字节）
- `order` 是 MySQL 保留字 → @TableName("`order`")
- 数据库 shopping 已建 11 张表（已验证）

## 已确认决策
1. 方案 A：8 张业务表（cart/cart_group/cart_item/order/order_item/stock_reservation/payment_order/refund_order）ALTER TABLE 加 created_by/updated_by
2. 3 张特殊表（order_operation_log/payment_record 仅 created_at；reconciliation_record 有 created_at/updated_at 无 version）实体不继承 BaseEntity，自定义字段
3. 补 common 包 MetaObjectHandler（脚手架缺口，全团队受益）
4. 配置 application-local.yml（用户 MySQL 密码 20040403jyX@ + 生成 JWT_SECRET）

## 拓扑（Components ledger）
| id | 组件 | 状态 |
| --- | --- | --- |
| C1 | 数据库加列（ALTER 8 表 + 更新设计文档） | 待执行 |
| C2 | common 包 MetaObjectHandler | 待执行 |
| C3 | application-local.yml 配置 | 待执行 |
| C4 | 11 个实体类（8 继承 BaseEntity + 3 自定义） | 待执行 |
| C5 | 11 个 Mapper 接口 | 待执行 |
| C6 | 验证（mvn compile + test） | 待执行 |

## 状态
- status: approved（用户已确认方案 A + 执行实体类/Mapper 层）
- pending action: 填充计划 todos → 直接执行（子代理环境不稳定，由 orchestrator 直接实施）