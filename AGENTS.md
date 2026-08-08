# 项目上下文与协作约定

本文件为在 `shoppingOnline` 工作区中启动的 Codex 新对话提供稳定上下文。开始开发前先阅读本文件及 `docs/` 下的相关文档。

## 项目概况

- 项目名称：综合电商平台课程设计。
- 仓库：`https://github.com/chareslm/shoppingOnline.git`。
- 当前阶段：仓库、文档、目录和 Git 规则已初始化；业务工程及功能代码尚未开始。
- 开发策略：先采用模块化单体，后端计划使用 Java 21 + Spring Boot 3；数据层计划使用 MySQL、Redis 和 RabbitMQ；Web 端计划使用 Vue 3 + TypeScript。

## 仓库结构

```text
backend/          后端项目
frontend-user/    用户端 Web / H5
frontend-admin/   管理员及商家管理端
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
- `feature/*`：从 `develop` 创建的功能分支，开发完成后通过 Pull Request 合并回 `develop`。
- `fix/*`：缺陷修复分支。
- 常规工作不得直接推送 `main` 或 `develop`，即使管理员具有绕过权限。
- Pull Request 至少需要 1 位非作者成员批准，并解决所有讨论后合并。
- 不得提交密码、Token、JWT 密钥、数据库密码、私钥、`.env`、`application-local.yml`、构建产物或 IDE 文件。

已确认的 GitHub Ruleset：`main` 和 `develop` 都启用了 Pull Request、至少 1 个批准、讨论解决、禁止强推和禁止删除。`main` 额外将 `Repository admin` 设为 `Always allow`，用于紧急或发布处理；这不应取代正常 PR 审核。

## 文档位置

- `docs/git-workflow.md`：完整 Git 协作规范。
- `docs/architecture/module-ownership.md`：模块分工及跨模块边界。
- `docs/project-context.md`：项目决策与当前状态记录。

## 工作原则

1. 先确认改动所属模块及其负责人，跨模块变更须同步更新接口或数据库文档。
2. 数据库变更使用版本化迁移脚本，放在 `database/`。
3. 提交信息使用 `type(scope): summary`，例如 `feat(auth): add password login`。
4. 修改后执行适当的检查，并在交付时说明验证结果与未完成项。
