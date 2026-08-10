# 后端 Spring Boot 工程与 Package 规范

> 本文件是当前项目的后端工程基线。后端只创建一个 Spring Boot 应用，所有成员在同一 Maven 工程中按模块开发。

## 1. 工程根与父包

后端工程位于 `backend/`，不创建五个 Spring Boot 服务，也不按成员建立五个 Maven 子模块。

```text
backend/
├── pom.xml
├── src/main/java/com/chareslm/shopping/
│   └── ShoppingApplication.java
├── src/main/resources/
└── src/test/java/com/chareslm/shopping/
```

统一父包为：

```java
com.chareslm.shopping
```

启动类固定为：

```java
package com.chareslm.shopping;

@SpringBootApplication
public class ShoppingApplication {
    public static void main(String[] args) {
        SpringApplication.run(ShoppingApplication.class, args);
    }
}
```

所有代码必须位于 `com.chareslm.shopping` 之下，避免 Spring Boot 扫描不到组件。

## 2. 最终 package 树

```text
com.chareslm.shopping
├── common/                    # 真正跨模块的通用能力；不放具体业务
│   ├── api/                   # ApiResponse、PageResponse、统一错误码
│   ├── config/                # 全局 Jackson、CORS、MyBatis-Plus、OpenAPI 配置
│   ├── constant/              # 全局常量，例如请求头名、时间格式
│   ├── exception/             # BusinessException、GlobalExceptionHandler
│   ├── model/                 # BaseEntity、分页查询基础对象
│   └── util/                  # 与具体业务无关的工具类
├── security/                  # 横切安全基础设施
│   ├── config/                # SecurityConfig、密码编码器配置
│   ├── filter/                # JwtAuthenticationFilter
│   ├── jwt/                   # Token 生成、解析、校验、黑名单接口
│   ├── context/               # LoginUser、CurrentUser 获取工具
│   └── authorization/         # 权限与数据范围判定组件
├── auth/                      # 注册、登录、刷新 Token、登出、验证码
├── user/                      # 用户资料、地址、偏好、店铺关注
├── statistics/                # 业务事件、指标、实时汇总、报表
├── merchant/                  # 商家入驻、店铺、员工、资质、客服组织
├── product/                   # 类目、SPU、SKU、价格、库存、上下架
├── search/                    # Elasticsearch 商品索引、检索、建议、热词
├── review/                    # 评价资格、评价、商家回复、评分聚合
├── cart/                      # 购物车、购物项、结算前校验
├── trade/                     # 结算、主/子订单、订单状态机、超时关闭
├── payment/                   # 支付单、回调、对账、退款
├── chat/                      # WebSocket、会话、客服分配、聊天消息
├── message/                   # 站内信、消息模板、偏好、推送
└── contentsafety/             # 后续扩展：敏感词、审核、申诉
```

`contentsafety` 和社区功能属于后续扩展；未进入第一阶段时不创建空业务类。

## 3. 每个业务模块的内部结构

除简单模块外，所有业务模块采用同一内部约定：

```text
<module>/
├── controller/                # HTTP 接口；只接收请求、鉴权、调用 service
├── service/                   # 业务接口
│   └── impl/                  # 业务实现、事务边界
├── entity/                    # 与数据库表映射的 MyBatis-Plus 实体
├── mapper/                    # Mapper 接口
├── dto/                       # 请求/响应 DTO；可按 request、response 再拆分
├── converter/                 # Entity、DTO、VO 的转换
├── event/                     # 本模块发布或消费的领域/业务事件（需要时）
└── enums/                     # 仅属于本模块的枚举和状态机状态
```

示例：

```text
auth/
├── controller/AuthController.java
├── service/AuthService.java
├── service/impl/AuthServiceImpl.java
├── dto/request/LoginRequest.java
├── dto/response/LoginResponse.java
└── event/UserLoggedInEvent.java

user/
├── controller/UserProfileController.java
├── controller/UserAddressController.java
├── entity/UserProfile.java
├── entity/UserAddress.java
├── mapper/UserProfileMapper.java
├── mapper/UserAddressMapper.java
└── service/UserProfileService.java
```

Mapper XML 与 Java package 对应，放在：

```text
backend/src/main/resources/mapper/
├── merchant/
├── product/
├── statistics/
└── trade/
```

简单 CRUD 可只使用 MyBatis-Plus `BaseMapper`；排行榜、经营统计、复杂 JOIN、跨表聚合使用 XML SQL，不强迫用 Wrapper 拼接。

## 4. `common` 的严格边界

`common` 只允许放“两个及以上模块都能复用，且不依赖任何业务模块”的代码。

允许：

- 统一响应体、分页响应、错误码和全局异常处理。
- 基础实体字段、通用配置、工具类、通用常量。
- 与业务无关的校验注解或 Web 参数处理器。

禁止：

- `UserService`、`OrderService`、商品 DTO、订单状态、角色名称等业务概念。
- 为图省事把所有 Entity、Mapper、DTO 放入 `common`。
- 让 `common` 依赖 `auth`、`user`、`product`、`trade` 等任何业务模块。

特别说明：`security` 不放在 `common`。它是全局横切模块，但自身包含 JWT、认证上下文与授权逻辑，独立为一级 package；`auth` 则是登录注册等业务流程模块。

## 5. 五人代码归属

| 成员 | 可以主写的 package | 主要内容 |
| --- | --- | --- |
| 项目管理员 | `security`、`auth`、`user`、`statistics`、`common` | 登录、JWT、RBAC、数据范围、资料地址偏好关注、审计、统计；`common` 变更须审慎审核 |
| 成员 2 | `merchant` | 商家入驻、店铺、员工、资质、客服组织 |
| 成员 3 | `product`、`search`、`review` | 商品、库存、搜索索引、评价 |
| 成员 4 | `cart`、`trade`、`payment` | 购物车、订单、支付、退款、库存预占协作 |
| 成员 5 | `chat`、`message` | 聊天会话、WebSocket、消息中心和推送 |

每个成员可以修改自己模块的测试和 Mapper XML。跨模块改动、`common`、`security`、数据库共享表、接口契约及 Maven 依赖需在 PR 中说明并由项目管理员复核。

## 6. 模块交互规则

1. Controller 不直接调用别的模块的 Mapper，也不直接操作别的模块的 Entity。
2. 需要读取另一模块信息时，优先调用该模块暴露的 Service；不要绕过 Service 直接查表。
3. 订单、支付、库存等跨模块强一致流程由 `trade` 协调，并通过明确 Service 方法和事务处理。
4. 搜索、统计、消息等可最终一致能力通过业务事件解耦；第一版可使用 Spring Application Event 或事务后同步处理，后续再接入 MQ。
5. DTO 不跨模块直接复用；跨模块参数定义为清晰的接口模型，避免一个模块改字段导致全局编译失败。

## 7. 已创建的基础代码

以下基础包和最小实现已创建；未列出的业务模块不要提前生成空业务类：

```text
common/api                    # 统一响应、分页响应与错误码
common/exception              # 业务异常与全局处理
common/config                 # Jackson、CORS、Flyway 等配置
security/config               # Spring Security 与 REST 安全错误响应
security/jwt                  # Access/Refresh Token 签发与解析
auth/config                   # 本地超级管理员一次性初始化
auth/controller               # 认证、账号安全与后台授权接口
auth/dto                      # 接口请求与响应 DTO
auth/entity                   # 账号、角色、权限、设备、Token、审计实体
auth/mapper                   # 身份权限数据访问
auth/service                  # 认证、授权查询、角色变更与审计服务
```

身份权限数据库迁移已由 `database/V1__identity_and_user.sql`、`database/V2__authorization_permissions.sql` 与 `database/V3__admin_user_view_permission.sql` 提供；字段说明见 `docs/database/`，调用契约见 `docs/api/auth.md`。
