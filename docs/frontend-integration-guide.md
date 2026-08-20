# 前端模块接入规范

本文规定用户 Web、统一管理端、Flutter Android App 和微信原生小程序的公共基础层与业务模块边界。目标是让各成员在不重复实现认证、不修改其他成员目录的前提下接入页面。

## 1. 基本原则

1. 四端共用后端账号、Bearer Token、角色、权限和业务规则，不在前端复制服务端授权逻辑。
2. 公共基础层负责登录、Token 刷新、退出、路由守卫、请求封装、布局和通用错误处理。
3. 各成员只在本人模块目录中维护业务页面、接口调用、类型和模块路由/菜单贡献。
4. 没有稳定接口契约时只保留空注册点，不提前写死请求字段、状态值或模拟业务规则。
5. 菜单隐藏仅改善交互，后端接口仍必须使用认证主体、权限编码和数据范围完成最终校验。

## 2. 模块与负责人

| 模块键 | 负责人 | 主要页面范围 |
| --- | --- | --- |
| `system` / `account` | 项目管理员 | 认证、账号安全、用户中心、RBAC、统计基础 |
| `merchant` | 成员 2 | 商家入驻、店铺、员工、客服及资质 |
| `product` | 成员 3 | 类目、商品、搜索、评价 |
| `trade` | 成员 4 | 购物车、结算、订单、支付及退款 |
| `message` | 成员 5 | 聊天、会话、站内消息及通知 |

店铺关注继续等待 `merchant` 模块提供店铺实体和查询契约，不得由前端自行假设 `shopId` 有效。

## 3. 管理端接入

管理端模块位于：

```text
frontend-admin/src/modules/
├── system/
├── merchant/
├── product/
├── trade/
├── message/
├── registry.ts
└── types.ts
```

各模块的 `index.ts` 导出一个 `AdminModuleContribution`：

```ts
export const productModule: AdminModuleContribution = {
  key: 'product',
  owner: '成员 3',
  routes: [
    {
      path: 'product/list',
      name: 'product-list',
      component: () => import('./views/ProductListView.vue'),
      meta: { permissions: ['product:view'] },
    },
  ],
  menuItems: [
    {
      index: '/product/list',
      label: '商品管理',
      icon: Goods,
      permissions: ['product:view'],
    },
  ],
}
```

已经注册的模块无需修改 `router/index.ts`、`layouts/AdminLayout.vue` 或中央 `registry.ts`。负责人直接替换本人模块中的占位菜单并增加路由即可。

管理端当前约定：

- 项目管理员的 `system` 模块已提供权限概览、用户与角色、审计日志页面；审计菜单和路由必须同时要求 `system:audit:view`。
- 业务路由使用模块前缀，例如 `merchant/shops`、`product/list`、`trade/orders`。
- 路由名称使用模块前缀，例如 `merchant-shop-list`。
- `meta.permissions` 和菜单 `permissions` 当前采用“任意一个匹配即可”的语义。
- 页面组件优先使用动态 `import()`，避免继续扩大管理端首屏 bundle。
- API 封装放在模块自己的 `api/` 或 `services/` 中，并复用公共 `services/http.ts`。
- 不得在业务模块中重新创建 Axios 实例、Token 刷新器或登录状态存储。

## 4. 用户 Web 接入

用户 Web 模块位于：

```text
frontend-web/src/modules/
├── account/
├── merchant/
├── product/
├── trade/
├── message/
├── registry.ts
└── types.ts
```

各模块导出 `WebModuleContribution`，包含业务路由和需要展示的顶层导航。尚未实现的模块保持空数组，因此不会向用户显示无效入口。

```ts
export const tradeModule: WebModuleContribution = {
  key: 'trade',
  owner: '成员 4',
  routes: [
    { path: 'orders', name: 'order-list', component: () => import('./views/OrderListView.vue') },
  ],
  menuItems: [{ to: '/orders', label: '我的订单' }],
}
```

商品详情等不适合顶层导航的页面只注册路由，不加入 `menuItems`。

## 5. Flutter 与微信小程序

Flutter Android App 与微信原生 TypeScript 小程序均已初始化，并采用以下业务划分：

```text
features/
├── account/
├── merchant/
├── product/
├── trade/
└── message/
```

公共网络层、Token 安全存储、刷新队列、路由守卫和通用响应解析放在 `core/`，业务模块不得复制。Flutter 使用 Riverpod、go_router 和 Dio；微信小程序使用 TypeScript，并由统一请求封装注入 Bearer Token。

Flutter 当前公共基础位于 `frontend-app/lib/core/`，认证、修改密码、个人资料、收货地址、偏好设置和登录设备管理位于 `frontend-app/lib/features/account/`。登录固定提交 `deviceType: ANDROID`，用户端路由要求账号包含 `USER` 角色；API 地址通过 `--dart-define=API_BASE_URL=...` 配置，Android 模拟器访问宿主机时默认使用 `http://10.0.2.2:8080`。Debug 构建仅为本地联调允许明文 HTTP，Release 构建必须使用 HTTPS。

Flutter 模块注册约定：

- `frontend-app/lib/app/module_registry.dart` 是中央注册表，已接入五个模块，成员无需修改。
- 每个 `features/<module>/module.dart` 导出一个 `AppModuleContribution`，包含模块键、负责人和本模块 `GoRoute`。
- `merchant`、`product`、`trade` 和 `message` 当前均为空路由注册点，不向用户展示入口，也不提前定义业务模型。
- 成员只在本人模块下增加 `domain/data/presentation` 和路由，网络请求必须复用 `apiClientProvider`。
- Flutter 的角色菜单或路由限制不构成安全边界，业务数据范围仍由后端认证主体和权限校验决定。

微信小程序模块注册约定：

- `frontend-miniapp/miniprogram/app/module-registry.ts` 是中央注册表，已接入五个模块，成员无需修改。
- 每个 `features/<module>/module.ts` 导出一个 `AppModuleContribution`，包含模块键、负责人和本模块页面路径。
- `merchant`、`product`、`trade` 和 `message` 当前均为空页面注册点，不提前定义业务模型或展示入口。
- 公共请求必须复用 `core/http/api-client.ts`，不得在业务模块中重复实现 Token 注入、刷新队列或统一响应解析。
- 登录固定提交 `deviceType: MINIAPP`；启动会话和用户页面要求账号包含 `USER` 角色，但客户端角色判断不替代后端授权。
- 开发 API 默认由 `config/environment.ts` 指向 `http://127.0.0.1:8080`；其他本机端口通过小程序存储键 `shopping.apiBaseUrl.development` 覆盖，不修改源码。公共 `project.config.json` 固定使用 `touristappid`，成员在被 Git 忽略的 `project.private.config.json` 配置个人测试 AppID。正式版必须使用 HTTPS，并在微信公众平台登记 `request` 合法域名。
- 用户 Web、Flutter 与小程序均提供本人登录设备列表、指定设备退出和其他设备退出；当前设备只能依据服务端签发的 Access Token 设备标识判断。撤销当前设备成功后，各端只清理本地会话，不再重复调用普通退出接口。

## 6. API 与类型约定

1. 开发页面前先在 `docs/api/` 增加或确认模块接口契约。
2. 接口统一返回 `{ code, message, data }`；分页使用 `{ items, total, page, pageSize }`。
3. ID、角色和数据范围以服务端响应为准，不从 URL、本地存储或表单字段推断当前主体；HTTP 接口中的 Java `Long` 统一按字符串处理，避免 JavaScript 精度丢失，并保持四端模型一致。
4. 前端只保存页面所需类型；跨模块共享类型需要先确认契约，禁止直接导入其他模块内部类型。
5. 错误提示通过公共解析器处理；表单可针对明确业务码提供更具体的中文提示。
6. 密码、Token、密钥、完整手机号和完整邮箱不得写入日志、截图、测试夹具或仓库文件。

## 7. 成员交付流程

1. 从最新 `develop` 创建本人 `feature/*` 分支。
2. 确认后端接口、权限编码和数据范围，更新 `docs/api/`。
3. 只修改本人后端包、数据库迁移和对应前端模块目录；公共层变更提前通知项目管理员。
4. 增加模块路由和菜单贡献，使用真实权限编码。
5. 完成后端测试、对应前端生产构建和至少一条真实接口联调。
6. 在 `docs/progress.md` 记录完成内容、验证结果、风险和下一步，由项目管理员集成到 `develop`。

## 8. 接入检查清单

- [ ] 未重复实现登录、Token 刷新或 Axios/Dio 客户端。
- [ ] 路由和菜单均位于本人模块注册文件中。
- [ ] 菜单权限与后端 `@PreAuthorize` 或等效校验一致。
- [ ] 服务端不信任客户端提交的用户、商家或店铺数据范围。
- [ ] 页面没有提前假设其他模块尚未确认的数据结构。
- [ ] API 文档、类型、构建结果和联调结果已同步。
