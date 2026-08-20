# 项目上下文与协作约定

本文件为在 `shoppingOnline` 工作区中启动的 Codex 新对话提供稳定上下文。开始开发前先阅读本文件及 `docs/` 下的相关文档。

## 项目概况

- 项目名称：综合电商平台课程设计。
- 仓库：`https://github.com/chareslm/shoppingOnline.git`。
- 当前阶段：身份认证与 RBAC、强密码与本人改密、统一管理端认证及用户角色管理、Redis/Elasticsearch 本地基础设施、用户资料/地址/偏好接口、用户 Web 用户中心、设备与会话管理、审计日志查询和综合回归修复均已集成至 `develop`；Flutter Android App 与微信原生 TypeScript 小程序的统一认证、用户中心、设备管理和模块注册基础均已完成真实接口联调。统计模块的指标口径、查询权限和聚合边界准备基线已完成，当前等待商家数据范围和交易金额契约满足开发准入条件。成员 3 的商品（类目/SPU/SKU/库存/上下架/审核）、搜索（ES 索引/同步/MySQL 降级检索/热词/建议）与评价（资格校验/评价/回复/评分聚合）后端已集成，数据库迁移 V5~V7 已落地；成员 4 的购物车/订单/支付/退款后端已集成，数据库迁移 V4 已落地。成员 2 商家模块与成员 5 客服聊天/消息中心尚待建设。
- 开发策略：先采用模块化单体，后端使用 Java 21 + Spring Boot 4.1；本地 Compose 已提供 MySQL、Redis 和 Elasticsearch，消息队列在后期确有异步需求时再接入；Web 端使用 Vue 3 + TypeScript。
- 终端范围：一期建设用户 Web、Android Flutter App、微信原生小程序和统一管理端；三端共用同一后端 API、账号与业务规则。

## 仓库结构

```text
backend/          后端项目
frontend-web/     用户 Web 商城
frontend-app/     Flutter Android App
frontend-miniapp/ 微信原生小程序
frontend-admin/   管理员及商家管理端
deploy/           后期 Docker Compose 与中间件部署配置
database/         数据库建表、初始化和迁移脚本
docs/             需求、接口、架构和协作文档
```

## 团队分工

1. 项目管理员：身份、安全、后台、用户、统计，以及 Git 管理。
2. 成员 2：商家入驻、店铺、员工、客服与资质。
3. 成员 3：类目、商品、搜索和评价。
4. 成员 4：购物车、结算、订单、支付与退款。
5. 成员 5：客服聊天与消息中心。

身份权限模块是全平台基础能力：统一认证、JWT、RBAC、数据范围、设备管理和审计日志。业务接口不得信任客户端提交的用户、商家或管理员数据范围。

## Git 规则

- `main`：稳定、演示或发布版本。
- `develop`：日常集成分支。
- `feature/*`：从 `develop` 创建的功能分支，开发完成、自测并完成文档更新后，由项目管理员直接合并或推送至 `develop`。
- `fix/*`：缺陷修复分支。
- `develop` 作为日常集成分支，项目管理员可直接推送或合并；其他成员仍通过功能分支提交改动，由项目管理员集成。
- `main` 不得直接推送或合并；进入 `main` 的 Pull Request 至少需要 1 位非作者成员批准，并解决所有讨论后合并。
- 不得提交密码、Token、JWT 密钥、数据库密码、私钥、`.env`、`application-local.yml`、构建产物或 IDE 文件。

已确认的 GitHub Ruleset：`develop` 允许项目管理员直接推送或合并，但仍禁止强推和删除；`main` 启用 Pull Request、至少 1 个非作者批准、讨论解决、禁止强推和禁止删除。`main` 的管理员绕过仅用于紧急或发布处理，不取代正常 PR 审核。

## 文档位置

只有 `docs/progress.md` 是项目管理员本地开发流水，不同步到远程仓库。需求基线、技术架构、后端包结构、项目上下文、接口与数据库说明均为团队共享文档；任何成员修改相关实现时都必须同步对应文档。

- `docs/requirements-baseline.md`：项目需求基线、范围、关键流程和验收目标；开始任何业务开发前应阅读。
- `docs/technical-architecture.md`：最终技术选型、模块化单体结构和基础设施边界；开发或初始化工程前必须阅读。
- `docs/backend-package-architecture.md`：后端工程、父包、`common` 边界和模块 package 归属；创建后端代码前必须阅读。
- `docs/progress.md`：本地开发进度、已完成工作、风险与下一步；开始新任务前必须阅读，完成一个功能后必须更新。
- `docs/git-workflow.md`：完整 Git 协作规范。
- `docs/architecture/module-ownership.md`：模块分工及跨模块边界。
- `docs/architecture/statistics-foundation.md`：统计指标口径、权限、数据范围、事件和聚合边界；统计开发前必须阅读。
- `docs/project-context.md`：项目决策与当前状态记录。
- `docs/frontend-integration-guide.md`：四端公共基础层、前端模块目录、路由菜单注册和成员接入检查清单。
- `docs/api/auth.md`：认证与 RBAC 接口调用契约；业务模块接入 Bearer Token 前必须阅读。
- `docs/api/audit.md`：管理端审计日志查询、权限、筛选与脱敏契约。
- `docs/api/user.md`：用户资料、收货地址与偏好接口契约；用户 Web 接入前必须阅读。
- `docs/api/product.md`：商品（类目/SPU/SKU/库存/上下架）接口契约。
- `docs/api/search.md`：商品搜索、建议与热词接口契约。
- `docs/api/review.md`：评价、回复与评分聚合接口契约。
- `docs/auth-bootstrap.md`：本地首次超级管理员初始化说明；不得用于生产环境。
- `docs/database/V1-identity-and-user.md`：身份、用户、设备、Token、角色与审计相关数据库迁移说明。
- `docs/database/V2-authorization-permissions.md`：后台授权权限初始化迁移说明。
- `docs/database/V3-admin-user-view-permission.md`：管理端用户查询权限迁移说明。
- `docs/database/V8-audit-log-view-permission.md`：管理端审计日志查询权限迁移说明。
- `docs/database/product-module.md`：成员 3 商品/评价/搜索数据库设计与状态机说明（V5~V7）。

## 工作原则

1. 先确认改动所属模块及其负责人，跨模块变更须同步更新接口或数据库文档。
2. 数据库变更使用版本化迁移脚本，放在 `database/`。
3. 提交信息使用 `type(scope): summary`，例如 `feat(auth): add password login`。
4. 修改后执行适当的检查，并在交付时说明验证结果与未完成项。
5. 每完成一个功能，更新 `docs/progress.md`，写明完成内容、提交、验证结果、风险和下一步。
6. 若技术架构与需求基线冲突，停止扩大范围并在 `docs/progress.md` 记录待确认事项；必须由项目管理员明确决定。当前已确认一期包含 Web、App 和微信小程序三端。
