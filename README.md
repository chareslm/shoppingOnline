# Shopping Online

一个面向课程设计的多商家综合电商平台，覆盖用户购物、商家经营与平台治理。

## 技术方向

- 后端：Java 21、Spring Boot 3、MySQL、Redis、RabbitMQ。
- Web：Vue 3、TypeScript、Vite。
- App：Flutter、Dart。
- 基础能力：JWT 认证与 RBAC 权限、审计日志、商品搜索、交易、支付模拟、聊天与消息。

## 目录说明

```text
backend/       后端模块化单体
web-user/      用户 Web 端
web-merchant/  商家 Web 端
web-admin/     管理员 Web 端
app-user/      用户 App
app-merchant/  商家 App
docs/          架构、接口、数据库和协作文档
```

## 团队协作

日常开发从 `develop` 分支创建 `feature/*` 分支。不得直接向 `main` 或 `develop` 推送。

详细规则见 [docs/git-workflow.md](docs/git-workflow.md)，模块分工见 [docs/architecture/module-ownership.md](docs/architecture/module-ownership.md)。
