# 本地开发进度

## 2026-08-18 第二次修改：运行时 SMTP、手动建号、商家一次审核与撤销

承接同日第一次商家入驻落地之后的交互：系统管理员可在管理端维护 SMTP 并手动建号；商家资质审核通过即开通账号，不再单独账号审核；已通过商家可撤销、已撤销商家可重新授予；并修正登录提示、后端启动等待和资质文件路径。

### P0 — Bug 修复

- 修复网易 163 SMTP 在 Docker 内发信失败：
  - 465 端口 SSL 握手被对端关闭；994 端口 `smtps` 可用。
  - `SmtpMailTransport` 对 465/994 使用协议 `smtps`、TLS 1.2，不再套 `SSLSocketFactory`（二次包装会导致握手中止）。
  - 对网易主机投递顺序优先尝试 994，再 465；不把 25 当作 STARTTLS 回退。
- 修复管理端登录把 SMTP 163 邮箱、本机超级管理员用户名和「后端正在重启」写进登录页提示与错误文案：登录页只保留身份选择说明；账号错误仅提示「账号或密码错误」。
- 修复 Maven/`spring-boot:run` 重启约一分钟内前端登录报连接失败：管理端与用户 Web 增加 `backendRetry.ts`，对无响应网络错误重试约 90 秒；Compose 后端增加端口健康检查与 `start_period: 90s`；新增 `scripts/windows/wait-backend.ps1`，`deploy-local.ps1` 在前端启动前等待 `http://127.0.0.1:8080/actuator/health` 为 `UP`。
- 修复资质审核详情与原文件下载 404：
  - 忽略文件 `application-local.yml` 写死 Windows 上传目录，在 Linux 容器内覆盖 Compose 的 `MERCHANT_UPLOAD_DIR=/data/uploads`，实际文件在挂载目录而运行时去错误路径读取。
  - 改为 `${MERCHANT_UPLOAD_DIR:D:/Project/data/shopping/uploads}`，与 `application.yml` 一致。
  - `QualificationFileStorage` 改为 `FileSystemResource` + `Files.isRegularFile`，避免 `UrlResource.isReadable()` 误判。
- 修复打开申请详情时预览接口失败导致整页「申请详情加载失败」：详情 JSON 成功即打开抽屉；图片预览失败单独提示，仍可审核和下载。
- 修复下载按钮无错误反馈：下载失败解析 blob JSON 并提示；下载请求带 `download=true` 使用 `Content-Disposition: attachment`。

### P1 — 内容新增

#### 后端与数据库

- `database/V9__system_smtp_and_admin_user.sql`：单行表 `system_smtp_setting`；权限 `system:smtp:view`、`system:smtp:update`、`system:user:create`（仅 `SUPER_ADMIN`）。Flyway 当前 schema 为 v9。
- `backend/.../message/`：运行时 SMTP 库表、`SmtpRuntimeSettings`（库配置优先于环境变量）、`SmtpAdminController`（`GET/PUT /api/admin/system/smtp`、`POST .../smtp/test`）。查询不返回密码明文。
- `POST /api/admin/authorization/users`：手动创建平台账号，邮箱必填，生成临时密码并标记 `mustChangePassword`，SMTP 投递；失败不回滚建号。`POST /api/admin/authorization/users/{id}/credential-email` 可重发（重发轮换临时密码）。
- 商家一次审核：`auditQualification` 通过时 CAS `SUBMITTED` → `QUALIFICATION_APPROVED` 后立即走开通（建号/复用、`MERCHANT_OWNER`、`OPEN` 店铺、开通邮件）。存量 `QUALIFICATION_APPROVED` 仍可在待审核队列一次开通。`account-audit` 仅作兼容。
- 列表状态别名：`PENDING`（`SUBMITTED`/`QUALIFICATION_APPROVED`）、`APPROVED`（`ACCOUNT_APPROVED` 且店铺 `OPEN`）、`REVOKED`（`ACCOUNT_APPROVED` 且店铺 `SUSPENDED`）。
- `POST /api/admin/merchant/applications/{id}/revoke`：店铺 `OPEN` → `SUSPENDED`，删除 `MERCHANT_OWNER`，作废刷新令牌（`MERCHANT_REVOKED`），发送撤销邮件。
- `POST /api/admin/merchant/applications/{id}/restore`：店铺 `SUSPENDED` → `OPEN`，补回角色，发送恢复邮件。邮件失败只记 `MAIL_FAILED`，不回滚权限变更。
- 资质文件 GET：图片/PDF 默认 inline 供预览；`download=true` 为附件下载。
- 单测：`SmtpSettingServiceImplTest`、`SmtpMailTransportTest`、`AuthorizationManagementServiceImplTest` 建号场景；`MerchantApplicationServiceImplTest` 覆盖资质通过即开通、存量过渡态开通、撤销/重新授予。

#### 前端（系统管理员 / 平台管理员 / 商家）

系统管理员与平台管理员共用 `frontend-admin`，登录时选择身份（`adminMode: system | platform`），菜单按 `adminModes` 过滤；商家不进管理端，走用户 Web 商家身份。

**系统管理员（`adminModes: ['system']`，`SUPER_ADMIN`）**

- `frontend-admin/src/modules/system/index.ts`：注册「用户与角色」「SMTP 配置」「系统日志」；SMTP 路由要求 `system:smtp:view`。
- `frontend-admin/src/views/SmtpSettingsView.vue`：查看/保存运行时 SMTP（二次确认当前管理员密码）、发送测试邮件；「填入 163 默认值」为 `smtp.163.com`、端口 994、不启用 STARTTLS、发件人与账号一致。
- `frontend-admin/src/services/system.ts`：封装 `GET/PUT /api/admin/system/smtp` 与 `POST .../smtp/test`。
- `frontend-admin/src/views/UserManagementView.vue`：手动创建平台账号（邮箱必填、角色、二次确认密码）、列表展示待改密、开通邮件失败可重发临时密码。
- `frontend-admin/src/services/auth.ts`：新增 `createUser`、`retryCredentialEmail`。
- `frontend-admin/src/types/auth.ts`：新增 `CreatedAdminUser`（含 `mustChangePassword`、`mailDeliveryStatus`）。

**平台管理员（`adminModes: ['platform']`，`ADMIN` / `SUPER_ADMIN`）**

- `frontend-admin/src/modules/merchant/index.ts`：单一菜单「商家审核」`/merchant/review`，权限 `merchant:qualification:audit`；旧资质/账号审核路径重定向到该页。
- `frontend-admin/src/modules/merchant/views/MerchantReviewView.vue`：待审核 / 已通过 / 已撤销三个队列。
- `frontend-admin/src/modules/merchant/views/MerchantReviewQueueView.vue`：待审核一次结论（通过即开通）；详情预览资质图片；已通过撤销；已撤销重新授予；`MAIL_FAILED` 重发邮件。
- `frontend-admin/src/modules/merchant/services/merchant.ts`：列表/详情、资质审核、预览 blob、附件下载（`download=true`）、撤销、重新授予、邮件重试。
- `frontend-admin/src/modules/merchant/types.ts`：补充 `shopStatus`。
- `frontend-admin/src/style.css`：资质图片预览样式。

**商家（用户 Web，不登录管理端）**

- `frontend-web/src/views/RegisterView.vue`：商家入驻说明改为资质审核通过即开通账号，不再承诺单独账号审核。
- 商家工作台仍为用户 Web `portalModes: ['merchant']` 占位菜单，本次未改店铺经营页。

**两端公共**

- `frontend-admin/src/views/LoginView.vue`：系统/平台身份选择；去掉 163 与后端重启提示词。
- `frontend-admin/src/services/backendRetry.ts`、`frontend-web/src/services/backendRetry.ts`：冷启动网络错误重试。
- `frontend-admin/src/services/http.ts`：拦截器接入启动重试。

#### 文档与脚本

- 新增 `docs/api/system.md`、`docs/database/V9-system-smtp-and-admin-user.md`。
- 更新 `docs/api/merchant.md`、`docs/database/merchant-module.md`、`docs/api/auth.md`、`docs/frontend-integration-guide.md`、`docs/project-context.md`、`docs/architecture/module-ownership.md`、`AGENTS.md`。
- 新增 `scripts/windows/wait-backend.ps1`。
- `deploy/docker-compose.yml` 与 `.example`：后端 healthcheck（`/dev/tcp/127.0.0.1:8080`）。

### P2 — 内容修改

- SMTP：库内主机优先于环境变量；网易主机投递配置见 P0。
- 商家状态机：资质通过即 `ACCOUNT_APPROVED`；撤销使用已有店铺状态 `SUSPENDED`，不新增 Flyway 申请状态。
- 管理端布局与工作台：`AdminLayout.vue` 按登录身份显示「系统管理台 / 平台管理台」；`DashboardView.vue` 区分系统管理员与平台管理员工作区说明。系统侧可见用户与 SMTP，平台侧可见商家审核等业务菜单。
- 管理端商家菜单仍为「商家审核」，旧「资质审核 / 账号审核」路由重定向到同一页。
- 本地上传目录：`application.yml`、`application-local.yml`（忽略文件）及其 example 使用 `MERCHANT_UPLOAD_DIR` 占位，避免 profile 覆盖 Compose 挂载路径。

### P3 — 内容删除

- 删除管理端登录页关于 `admin_local`、163 邮箱和后端未就绪的多余提示。
- 删除管理端「账号审核」独立队列交互（接口 `account-audit` 保留给存量数据）。
- 未删除 V8 申请状态枚举中的 `QUALIFICATION_APPROVED`（作为开通前过渡态保留）。

### 可选配置及修改位置

- 系统管理员在管理端「SMTP 配置」保存运行时发信参数；163 使用授权码、端口 **994**、`smtps`，发件人与登录账号一致。
- 未在管理端保存主机时，仍回退 `deploy/.env` 的 `MAIL_*`。
- 资质文件：本机默认 `D:/Project/data/shopping/uploads`；Compose 为容器内 `/data/uploads`，对应宿主机 `${DATA_DIR}/shopping/uploads`。不要在 `application-local.yml` 写死与 Compose 冲突的绝对路径。
- 重启 Docker 后端后须等到 health `UP` 再登录（可用 `scripts/windows/wait-backend.ps1`）。

### 验证结果

- Flyway 已应用至 v9；`GET /actuator/health` 在后端重启后为 `UP`。
- `MerchantApplicationServiceImplTest` 在 `shopping-backend` 容器内通过。
- 163 连通性：容器内 openssl 对 994 可拿到 `*.163.com` 证书；465/25 不作为可用回退。
- 资质文件在容器 `/data/uploads/2026-08-18/` 与数据库 `storage_key` 对齐后，详情预览与附件下载可用。
- 未将 `docs/progress.md` 纳入本次整理；本文件为明确要求的阶段整合。

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
