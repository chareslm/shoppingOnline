# transaction-entity-mapper - Work Plan

## TL;DR (For humans)
<!-- Fill this LAST, after the detailed plan below is written, so it summarizes the REAL plan. -->

**What you'll get:** 交易模块（购物车/订单/支付）的 Java 实体类 + Mapper 层：11 张表对应 11 个实体和 11 个 Mapper，数据库补上审计字段，应用能启动。

**Why this approach:** 团队约定实体继承 BaseEntity（雪花 ID + 自动填充审计字段），所以先给表加 created_by/updated_by 列对齐基类；补上脚手架缺失的 MetaObjectHandler 让自动填充真正生效。

**What it will NOT do:** 不写任何 Service/Controller 业务逻辑；不写 XML Mapper；不改其他成员的模块代码。

**Effort:** Medium
**Risk:** Low - 纯数据访问层，无业务逻辑；唯一风险是 `order` 保留字表名，已用反引号处理。

**Decisions to sanity-check:** ① 8 张业务表加审计列，3 张日志/对账表不继承 BaseEntity ② MetaObjectHandler 放 common 包（团队级，现在补最合适）③ 定时任务等无用户上下文场景 createdBy/updatedBy 填 null。

Your next move: 执行。

---

> TL;DR (machine): Medium effort, Low risk. ALTER 8 tables + MetaObjectHandler + application-local.yml + 11 entities + 11 mappers + mvn verify.

## Scope
### Must have
- `database/transaction/004_add_audit_columns.sql`：8 张业务表加 created_by/updated_by 列，执行成功
- `docs/database/transaction-module.md`：字段清单更新（加 created_by/updated_by）
- `common/config/MyMetaObjectHandler.java`：MetaObjectHandler 实现（createdAt/updatedAt/createdBy/updatedBy 自动填充）
- `application-local.yml`：数据库连接（root/20040403jyX@）+ JWT_SECRET（≥32字节）
- 11 个实体类：cart(3) + trade(4) + payment(4)，8 个继承 BaseEntity，3 个自定义
- 11 个 Mapper 接口：各模块 mapper 子包，extends BaseMapper<T>
- 验证：mvn compile + mvn test 通过

### Must NOT have (guardrails, anti-slop, scope boundaries)
- 不写 Service/Controller/XML Mapper（后续任务）
- 不改其他成员模块（auth/user/merchant/product/chat 等）的代码
- 不提交 application-local.yml（gitignore 已忽略）
- 不引入新依赖
- 不修改 001/002/003 建表脚本（用 004 增量脚本）

## Verification strategy
> Zero human intervention - all verification is agent-executed.
- Test decision: tests-after + mvn 编译/测试
- Evidence: .omo/evidence/transaction-entity-mapper/task-<N>.txt

## Execution strategy
### Parallel execution waves
- Wave 1: T0 数据库加列（先行，实体依赖列存在）
- Wave 2: T1 MetaObjectHandler + T2 application-local.yml（并行，均不依赖 T0）
- Wave 3: T3 实体类（依赖 T0 列存在）
- Wave 4: T4 Mapper（依赖 T3）
- Wave 5: T5 验证（依赖全部）

### Dependency matrix
| Todo | Depends on | Blocks | Can parallelize with |
| --- | --- | --- | --- |
| T0 数据库加列 | 无 | T3 | T1, T2 |
| T1 MetaObjectHandler | 无 | T5 | T0, T2 |
| T2 application-local.yml | 无 | T5 | T0, T1 |
| T3 实体类 | T0 | T4 | — |
| T4 Mapper | T3 | T5 | — |
| T5 验证 | T1/T2/T3/T4 | — | — |

## Todos
> Implementation + Test = ONE todo. Never separate.
<!-- APPEND TASK BATCHES BELOW THIS LINE WITH edit/apply_patch - never rewrite the headers above. -->
- [x] 0. 数据库加审计列（方案 A）
  What to do: 创建 `database/transaction/004_add_audit_columns.sql`，对 8 张业务表（cart/cart_group/cart_item/order/order_item/stock_reservation/payment_order/refund_order）执行 `ALTER TABLE ... ADD COLUMN created_by BIGINT NULL COMMENT '创建人ID' AFTER updated_at, ADD COLUMN updated_by BIGINT NULL COMMENT '更新人ID' AFTER created_by`；在 MySQL 执行；更新 `docs/database/transaction-module.md` 字段清单（8 张表加 created_by/updated_by，注明日志/对账表除外）。
  Must NOT do: 不改 001/002/003 脚本；不动 order_operation_log/payment_record/reconciliation_record。
  Parallelization: Wave 1 | Blocked by: 无 | Blocks: T3
  References: docs/database/transaction-module.md §3；BaseEntity.java（createdBy/updatedBy 字段）
  Acceptance criteria: 脚本执行 exit 0；`SHOW COLUMNS FROM cart LIKE 'created_by'` 返回 1 行；8 张表均含两列。
  QA scenarios: happy: mysql 执行 + SHOW COLUMNS 验证；failure: 重复执行应报"Duplicate column"（说明脚本非幂等，可接受）。Evidence: .omo/evidence/transaction-entity-mapper/task-0.txt
  Commit: Y | feat(database): 交易表增加审计列

- [x] 1. common 包补 MetaObjectHandler
  What to do: 创建 `backend/src/main/java/com/chareslm/shopping/common/config/MyMetaObjectHandler.java`：实现 `MetaObjectHandler`，insertFill 填 createdAt/updatedAt/createdBy/updatedBy，updateFill 填 updatedAt/updatedBy；createdBy/updatedBy 从 `CurrentUser.require().userId()` 获取，无认证上下文（定时任务）时填 null（try-catch 包裹）。
  Must NOT do: 不改 BaseEntity；不改其他 common 类。
  Parallelization: Wave 2 | Blocked by: 无 | Blocks: T5
  References: BaseEntity.java（FieldFill 字段名 createdAt/updatedAt/createdBy/updatedBy）；CurrentUser.java（require()）
  Acceptance criteria: 文件存在；mvn compile 通过；insertFill/updateFill 均实现。
  QA scenarios: happy: mvn compile；failure: 若 CurrentUser 抛异常应被捕获返回 null。Evidence: .omo/evidence/transaction-entity-mapper/task-1.txt
  Commit: Y | feat(common): 补充 MyBatis-Plus 审计字段自动填充

- [x] 2. 配置 application-local.yml
  What to do: 复制 `application-local.yml.example` 为 `backend/src/main/resources/application-local.yml`，填入：url=jdbc:mysql://127.0.0.1:3306/shopping?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai、username=root、password=20040403jyX@、jwt.secret=随机生成≥32字节密钥（如 openssl rand -base64 48 或手动生成）。
  Must NOT do: 不提交该文件（gitignore 已忽略）；不把密码写进任何会提交的文件。
  Parallelization: Wave 2 | Blocked by: 无 | Blocks: T5
  References: application-local.yml.example；application.yml（环境变量占位）
  Acceptance criteria: 文件存在；含正确密码与 ≥32 字节 secret；`git status` 显示该文件被忽略（untracked 不出现）。
  QA scenarios: happy: 文件存在且内容正确；failure: 若 git status 显示该文件 → 检查 .gitignore。Evidence: .omo/evidence/transaction-entity-mapper/task-2.txt
  Commit: N（本地文件不提交）

- [x] 3. 实体类 11 个
  What to do: 创建 11 个实体类（Lombok @Getter/@Setter/@TableName）：
  cart 包：Cart/CartGroup/CartItem（继承 BaseEntity，字段对应表列，@TableField 映射 camelCase）
  trade 包：Order（@TableName("`order`") 反引号！）/OrderItem/StockReservation（继承 BaseEntity）；OrderOperationLog（不继承，自定义 id/orderId/operatorType/operatorId/action/fromStatus/toStatus/remark/createdAt）
  payment 包：PaymentOrder/RefundOrder（继承 BaseEntity）；PaymentRecord（不继承，自定义 id/paymentOrderId/callbackType/rawData/status/processResult/createdAt）；ReconciliationRecord（不继承，自定义 id/bizDate/channel/totalAmount/totalCount/diffCount/diffAmount/status/detail/createdAt/updatedAt）
  字段类型：BIGINT→Long、DECIMAL→BigDecimal、DATETIME→LocalDateTime、TINYINT→Integer、DATE→LocalDate、JSON→String、TEXT→String。
  Must NOT do: 不写 Service/Controller；不写 XML；不改 BaseEntity。
  Parallelization: Wave 3 | Blocked by: T0 | Blocks: T4
  References: docs/database/transaction-module.md §3（字段定义）；BaseEntity.java
  Acceptance criteria: 11 个文件存在；mvn compile 通过；Order 的 @TableName 含反引号。
  QA scenarios: happy: mvn compile；failure: 若编译报字段映射错误 → 对照表结构修正。Evidence: .omo/evidence/transaction-entity-mapper/task-3.txt
  Commit: Y | feat(cart/trade/payment): 交易模块实体类

- [x] 4. Mapper 接口 11 个
  What to do: 创建 11 个 Mapper 接口 extends BaseMapper<T>：cart/mapper/{CartMapper,CartGroupMapper,CartItemMapper}、trade/mapper/{OrderMapper,OrderItemMapper,StockReservationMapper,OrderOperationLogMapper}、payment/mapper/{PaymentOrderMapper,PaymentRecordMapper,RefundOrderMapper,ReconciliationRecordMapper}。加 @Mapper 注解（或依赖 @MapperScan 通配）。
  Must NOT do: 不写 XML；不写自定义方法。
  Parallelization: Wave 4 | Blocked by: T3 | Blocks: T5
  References: ShoppingApplication.java（@MapperScan("com.chareslm.shopping.**.mapper")）
  Acceptance criteria: 11 个文件存在；mvn compile 通过。
  QA scenarios: happy: mvn compile；failure: 若 Mapper 未扫描 → 检查包路径。Evidence: .omo/evidence/transaction-entity-mapper/task-4.txt
  Commit: Y | feat(cart/trade/payment): 交易模块 Mapper 接口

- [x] 5. 验证（mvn compile + test）
  What to do: 在 backend/ 执行 `mvn compile`（exit 0）；执行 `mvn test`（ShoppingApplicationTest contextLoads 通过，需 application-local.yml 已配置）。
  Must NOT do: 不修改测试代码；不跳过测试。
  Parallelization: Wave 5 | Blocked by: T1/T2/T3/T4 | Blocks: —
  References: backend/pom.xml；ShoppingApplicationTest.java
  Acceptance criteria: mvn compile exit 0；mvn test 全部通过（BUILD SUCCESS）。
  QA scenarios: happy: mvn test BUILD SUCCESS；failure: 若 contextLoads 失败 → 检查 application-local.yml 数据库连接。Evidence: .omo/evidence/transaction-entity-mapper/task-5.txt
  Commit: N（验证通过后由 T0-T4 的 commit 覆盖）

## Final verification wave
> Runs in parallel after ALL todos. ALL must APPROVE. Surface results and wait for the user's explicit okay before declaring complete.
- [ ] F1. Plan compliance audit — 6 个 todo 交付物全部存在，验收标准满足
- [ ] F2. Code quality review — 实体字段与表结构一致；@TableName 反引号正确；无业务逻辑混入
- [ ] F3. Real manual QA — mvn compile + mvn test 实际执行通过
- [ ] F4. Scope fidelity — 未改其他成员模块；未写 Service/Controller；application-local.yml 未提交

## Commit strategy
- T0-T4 各一个 commit（见各 todo Commit 行）；T2 不提交
- 分支：从 develop 创建 `feature/transaction-entity-mapper`（若 develop 不存在则从 main 创建）
- commit message 遵循仓库风格（feat: 前缀，中文描述）

## Success criteria
- [ ] 8 张业务表含 created_by/updated_by 列（SHOW COLUMNS 验证）
- [ ] MetaObjectHandler 存在且 mvn compile 通过
- [ ] application-local.yml 配置完成（git 忽略）
- [ ] 11 个实体类 + 11 个 Mapper 编译通过
- [ ] mvn test BUILD SUCCESS