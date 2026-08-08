# 项目上下文记录

> 最后更新：2026-08-08

## 项目定位

本项目是五人协作完成的综合电商平台课程设计，面向用户购物、商家经营和平台治理。课程设计阶段采用模块化单体，避免过早拆分微服务。

## 当前仓库状态

- GitHub 仓库：`https://github.com/chareslm/shoppingOnline.git`。
- 已存在远程分支：`main`、`develop`。
- 当前目录仅有工程占位及协作文档，尚未初始化 Spring Boot、Vue 或数据库迁移工程。
- Git 跟踪的根目录：`backend/`、`frontend-user/`、`frontend-admin/`、`database/`、`docs/`。

## 业务分工

| 负责人 | 模块 | 主要职责 |
| --- | --- | --- |
| 项目管理员 | 身份、安全、后台、用户、统计、Git | 认证、授权、账号安全、用户资料/地址、管理员后台基础、审计、业务事件和指标口径 |
| 成员 2 | 商家 | 入驻、店铺、员工、客服、资质 |
| 成员 3 | 商品 | 类目、SPU、SKU、价格、库存、上下架、搜索、评价 |
| 成员 4 | 交易 | 购物车、结算、订单、支付、退款、状态机、并发一致性 |
| 成员 5 | 聊天与消息 | 会话、客服分配、聊天、站内信、多渠道推送 |

## GitHub 分支与规则

```text
feature/* 或 fix/* → Pull Request → develop → Pull Request → main
```

- `main` 与 `develop`：必须通过 Pull Request，至少 1 个批准，且所有讨论已解决。
- 两个分支均禁止强制推送和删除。
- `main`：启用“审批在新提交后失效”；`Repository admin` 位于 Bypass list，配置为 `Always allow`。
- `develop`：已启用 PR、1 个批准、讨论解决、禁止删除和禁止强推；尚未确认是否对管理员配置 bypass。
- 已测试：未配置绕过时，直接推送受保护分支会被 GitHub 以 `GH013` 拒绝；为 `main` 配置管理员 `Always allow` 后，管理员的直接推送模拟可通过。

## 使用约定

1. 从 `develop` 更新后再创建自己的功能分支。
2. 提交后推送功能分支，并在 GitHub 创建目标为 `develop` 的 PR。
3. PR 作者不能审批自己的 PR，须由其他团队成员审核。
4. 高风险或跨模块改动须在 PR 描述中说明影响范围。
5. 任何 Secret、数据库密码、JWT 密钥、第三方 API Key、`.env` 或 `application-local.yml` 均不得提交。

## 下一阶段建议

项目管理员优先在 `feature/auth-admin` 建立后端基础工程，并实现账号、登录、JWT、RBAC、管理员基础页面和审计日志。其他模块再基于统一认证上下文对接。
