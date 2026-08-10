# 最终技术架构

> 来源：项目管理员确认的《最终技术栈》。本文件是团队共享架构基线；开始工程初始化、基础设施配置或跨模块开发前必须阅读。

## 1. 架构决策

项目采用**模块化单体**，即一个 Spring Boot 应用承载全部核心业务模块，不按五人分工拆成五个独立后端服务。这样可降低分布式事务、部署与调试复杂度，且更利于课程项目先完成交易闭环。

建议后端包结构：

```text
backend/src/main/java/com/chareslm/shopping/
├── common/
├── security/
├── auth/
├── user/
├── statistics/
├── merchant/
├── product/
├── search/
├── review/
├── cart/
├── trade/
├── payment/
├── chat/
├── message/
└── contentsafety/
```

模块负责人保持不变：成员 1 负责 `auth/user/security/statistics`，成员 2 负责 `merchant`，成员 3 负责 `product/search/review`，成员 4 负责 `cart/trade/payment`，成员 5 负责 `chat/message`。

## 2. 最终技术选型

| 层级 | 技术 | 用途 |
| --- | --- | --- |
| 后端 | Java 21、Spring Boot、Spring MVC、Maven | REST API、业务逻辑、配置与构建 |
| 安全 | Spring Security + JWT | 登录认证、授权、RBAC |
| 数据访问 | MyBatis-Plus，复杂查询使用 XML SQL | MySQL CRUD、分页、统计与复杂 JOIN |
| 核心数据 | MySQL 8.4 LTS | 用户、商品、订单、支付等权威业务数据 |
| 缓存 | Redis（后期逐步接入） | 验证码、缓存、热点、幂等、限流、在线状态 |
| 搜索 | Elasticsearch | 商品全文检索、筛选、建议、热词 |
| 日志检索 | Logback + Filebeat + Elasticsearch + Kibana | 结构化日志采集、检索与分析 |
| API 文档 | OpenAPI 3 + Knife4j | 接口契约、调试与前后端协作 |
| 用户 Web | Vue 3 + Vite + TypeScript + Vue Router + Pinia + Axios | PC/Web 商城 |
| 管理端 | Vue 3 + Vite + TypeScript + Element Plus + Vue Router + Pinia + Axios | 管理员、商家、客服工作台 |
| Android App | Flutter + Dart + Dio + Riverpod + go_router | 移动端购物与商家高频操作 |
| 微信端 | 微信原生小程序 + TypeScript | 微信小程序 |
| 实时通信 | WebSocket | 客服聊天、实时消息 |
| 部署（后期） | Docker / Docker Compose | MySQL、Redis、Elasticsearch、Kibana、Filebeat 环境统一 |

## 3. 数据与中间件边界

### MySQL 是业务真相源

用户、商品、库存、订单、支付与退款等关键数据都以 MySQL 为准。库存、订单、支付等强一致链路不能依赖 Elasticsearch 或 Redis 作为权威来源。

### Redis 逐步引入

本地开发环境已通过 Docker Compose 提供 Redis 7.4；第一阶段先不将 Redis 作为订单、库存等核心数据的权威来源。

第一阶段先实现 MySQL 版本并打通流程，再根据热点引入缓存。预留的典型用途包括：

- 登录验证码、Token 黑名单和短期登录状态。
- 商品详情、热门商品和热词缓存。
- 下单防重复、临时结算数据、分布式锁和秒杀限购。
- 聊天在线状态、客服在线状态和未读数。

### Elasticsearch 仅作搜索与日志索引

本地开发环境已通过 Docker Compose 提供 Elasticsearch 9.4 单节点；安全认证仅为本地开发关闭，部署到共享或生产环境时必须启用认证与 TLS。

商品在 MySQL 写成功后同步到 Elasticsearch；第一版可以同步调用索引服务，若索引失败则通过后台全量重建接口补偿：

```text
POST /api/admin/search/reindex/products
```

后续再考虑使用业务事件或消息队列异步同步。商品搜索与日志使用不同索引，例如：

```text
mall-product-v1
mall-log-backend-YYYY.MM
mall-operation-log-v1
```

推荐日志链路：

```text
Spring Boot → Logback → application.log / error.log / audit.log
→ Filebeat → Elasticsearch → Kibana
```

日志应结构化，至少包含时间、级别、服务名、`traceId`、主体 ID、模块、动作、消息及关键业务 ID；不得记录密码、Token 或完整隐私信息。

## 4. 安全与权限实现

认证链路：

```text
用户名/密码 → Spring Security Authentication → JWT 签发
→ 前端 Bearer Token → JWT Filter 校验 → SecurityContext → Controller
```

采用 RBAC。基础角色包括：

```text
USER
MERCHANT_OWNER
MERCHANT_STAFF
CUSTOMER_SERVICE
ADMIN
SUPER_ADMIN
```

基础表：`user`、`role`、`permission`、`user_role`、`role_permission`。权限仍按 `resource:action` 设计，例如 `product:audit`、`merchant:audit`、`user:disable`、`statistics:view`。

当前已实现的认证基线：注册、密码登录、本人改密、30 分钟 Access Token、7 天且轮换使用的 Refresh Token、设备登出与当前用户查询。后台角色、权限、用户查询及平台角色分配接口已落地；角色分配要求权限校验和密码二次确认，并写入审计日志。接口契约见 `docs/api/auth.md`，本地首次超级管理员初始化见 `docs/auth-bootstrap.md`。

角色变更会撤销目标账号的 Refresh Token；已签发的 Access Token 最多仍可使用至其 30 分钟有效期结束。若后续需要立即失效，再引入认证版本号或 Token 黑名单。

## 5. 前端与聊天架构

管理端统一使用 `frontend-admin`，并依据角色展示菜单，不拆成单独的管理员、商家和客服前端工程。用户 Web 使用 Vue 3；Flutter 用于 Android App。

管理端和用户 Web 采用模块注册机制：公共层集中维护认证、请求、布局和路由守卫，各领域负责人分别在 `merchant`、`product`、`trade`、`message` 目录中贡献本模块路由与菜单。中央注册表预先接入所有模块，成员增加业务页面时无需修改公共路由和布局。详细协作规范见 `docs/frontend-integration-guide.md`。

聊天职责划分：

```text
WebSocket：实时传递
MySQL：聊天记录持久化
Redis（后期）：在线状态、未读状态、临时会话状态
```

## 6. 已确认的最终目录

```text
backend/
frontend-web/
frontend-app/
frontend-miniapp/
frontend-admin/
deploy/
├── docker-compose.yml
├── mysql/
├── redis/
├── elasticsearch/
├── kibana/
└── filebeat/
database/
docs/
├── architecture/
├── api/
├── database/
├── git/
└── requirements/
README.md
.gitignore
```

## 7. 已确认决策

1. 一期建设用户 Web、Flutter Android App 和微信原生小程序三端，三端共用 Spring Boot 后端 API、账号和业务规则。
2. 仓库统一使用 `frontend-web`、`frontend-app`、`frontend-miniapp` 和 `frontend-admin` 四个前端目录；原 `frontend-user` 已迁移为 `frontend-web`。
