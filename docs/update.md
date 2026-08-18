# 本地开发进度

## 2026-08-18 第一次修改：商家入驻、审核及本地部署完善

### P0 — Bug 修复

- 修复新商家账号无法强制首次改密的问题：
  - `database/V8__merchant_tables.sql` 为 `user` 增加 `must_change_password`（V8 已执行后保持不可变）。
  - `backend/.../auth/entity/UserAccount.java`、`UserAccountMapper.java`、`AuthServiceImpl.java` 在临时密码创建、登录响应和正式改密时维护该标记。
  - `backend/.../security/filter/MustChangePasswordFilter.java` 在服务端限制临时密码账号只能访问本人信息、改密和退出接口。
  - `frontend-web/src/views/ForcedPasswordChangeView.vue` 与管理端 `ChangePasswordDialog.vue` 提供不可绕过的首次改密入口。
- 修复商家角色授权缺少操作人映射：`UserRole.java` 增加 `grantedBy`，账号审核写入平台审核员 ID。
- 修复未配置 SMTP 时健康检查返回 `OUT_OF_SERVICE`：`application.yml` 默认关闭 Mail Health；Compose 本地健康检查不再依赖可选中间件。
- 修复 Docker 持久化目录可能与其他项目混用：MySQL、Redis、Elasticsearch、商家资质统一隔离到 `${DATA_DIR}/shopping/`。
- 修复本地超级管理员初始化长期启用风险：忽略的 `application-local.yml` 已关闭 bootstrap 并清空一次性凭据。

### P1 — 内容新增

#### 后端与数据库

- `database/V8__merchant_tables.sql`：新增 `merchant_application`、`merchant_qualification_file`、`shop`，以及 `merchant:qualification:audit` 权限种子。
- `backend/src/main/java/com/chareslm/shopping/merchant/`：
  - 新增申请 DTO、实体、Mapper、公开提交接口、管理审核接口、服务及私有文件存储。
  - 支持企业、个体工商户、个人商家；执行“资质审核 → 账号审核”CAS 状态机。
  - 账号审核通过后复用普通账号或创建 `MERCHANT_OWNER`，并创建 `OPEN` 店铺。
  - 文件限制为 PDF/JPEG/PNG、单文件 10 MB，校验文件签名并防止路径穿越。
- `backend/src/main/java/com/chareslm/shopping/message/service/`：新增通用 SMTP 接口与实现；失败写入 `MAIL_FAILED` 并支持重试。
- `backend/src/test/java/com/chareslm/shopping/merchant/`：新增商家申请、状态转换、账号复用/新建和邮件失败测试。

#### 前端

- `frontend-web/src/modules/merchant/`：新增商家申请类型与 multipart API。
- `frontend-web/src/views/ForcedPasswordChangeView.vue`：新增临时密码首次改密页面。
- `frontend-admin/src/modules/merchant/`：新增资质审核队列、账号审核队列、详情、文件下载和邮件重试。

#### 文档与脚本

- 新增 `docs/api/merchant.md`、`docs/database/merchant-module.md`。
- 新增 `scripts/windows/deploy-local.ps1`：
  - 从 example 创建缺失的本地配置；
  - 校验必填 Secret、创建持久化目录、启动 Compose、等待健康状态并启动两端前端。
- 新增 `scripts/windows/clean-local.ps1`：
  - 默认仅停止服务并清理构建产物；
  - 依赖、本地配置和持久化数据均需显式开关；
  - 清除持久化数据需要输入 `PURGE`，且只删除 `${DATA_DIR}/shopping`。
- 新增 `scripts/windows/README.md`，说明常用与高风险命令。
- 新增 `deploy/docker-compose.yml.example`，完整示例化 MySQL、Redis、Elasticsearch、商家上传文件的持久化挂载及后端服务配置；部署脚本会在活动 Compose 文件缺失时自动复制。

### P2 — 内容修改

- 构建与配置：
  - `backend/pom.xml` 增加 Spring Mail。
  - `backend/src/main/resources/application.yml` 增加 multipart、SMTP、商家私有目录和可选健康检查配置，并补充必填/可选注释。
  - `deploy/docker-compose.yml` 增加 Java 21 后端服务、项目隔离持久化、私有上传目录和 SMTP 环境映射。
  - `deploy/.env.example`、`application-local.yml.example`、两端 `.env.example` 明确 `[REQUIRED]`、`[OPTIONAL]` 和修改场景。
  - 本地 `deploy/.env`、`application-local.yml` 与 example 完全分离，继续由 `.gitignore` 排除。
- 认证与安全：
  - 修改 `AuthController`、登录/当前用户响应、JWT Claims、错误码与异常映射，使 `mustChangePassword` 在前后端一致。
- 用户 Web：
  - 修改 `RegisterView.vue`，增加个人/商家注册切换、身份字段和资质上传。
  - 修改路由、登录、会话类型及 API，使新商家登录后自动进入首次改密。
- 管理台：
  - 将 `merchant/index.ts` 的待接入占位替换为资质审核与账号审核菜单。
  - 修改布局、会话类型和改密对话框，在临时密码状态下强制完成改密。
- 协作文档：
  - 更新 `AGENTS.md`、`technical-architecture.md`、`project-context.md`、`module-ownership.md`、`frontend-integration-guide.md`、`docs/api/auth.md`、`deploy/README.md`。
- 注释：
  - 为商家服务、Mapper 状态机、文件存储、SMTP、首次改密过滤器、前端审核/注册关键分支及部署选项补充职责和安全边界注释。

### P3 — 内容删除

- 未删除业务源码、迁移或正式文档。
- 已删除接口冒烟测试创建的临时 PDF/JSON、测试申请数据库记录及对应上传文件。
- 默认清理脚本不会删除任何持久化业务数据；只有显式 `-PurgePersistentData` 才执行不可恢复清理。

### 可选配置及修改位置

- 必须修改：`deploy/.env` 中 `MYSQL_APP_PASSWORD`、`MYSQL_ROOT_PASSWORD`、`JWT_SECRET`。
- 需要真实发信时修改：`MAIL_HOST`、`MAIL_PORT`、`MAIL_USERNAME`、`MAIL_PASSWORD`、`MAIL_FROM`。
- 端口冲突时可修改：`MYSQL_PORT`、`REDIS_PORT`、`ELASTICSEARCH_PORT`、`BACKEND_PORT`。
- Maven 缓存不在默认位置时可修改：`MAVEN_REPO_DIR`。
- 数据不放在 `D:/Project/data` 时可修改：`DATA_DIR`；脚本仍会追加 `shopping` 子目录。
- 原生后端开发时修改忽略文件 `application-local.yml`，不要把真实值写回 example。

### 验证结果

- 后端 Java 21 编译通过。
- 认证与商家相关测试 9/9 通过。
- 用户 Web、管理台生产构建通过，IDE Lints 无错误。
- 商家相关 21 个后端/前端文件已补齐职责、安全边界和非显然状态注释；隔离 Java 21 编译及两端 Vue 类型检查通过。
- Windows 部署/清理脚本通过 PowerShell AST 语法检查；Compose 配置与 Git 本地配置忽略规则检查通过。
- `docker-compose.yml.example` 已通过 `docker compose config --quiet` 独立校验。
- V8 已由 Flyway 成功应用；公开 multipart 申请上传冒烟测试通过且测试数据已清理。
- 后端健康状态 `UP`；MySQL、Redis、Elasticsearch 均为 healthy；两端前端 HTTP 200。
- 全量测试中的原有交易集成测试仍受测试环境数据库/内存条件影响，未观察到本次商家代码导致的断言失败。
