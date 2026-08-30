# Flutter Android App

综合电商平台 Android 客户端，Android 包名为 `com.chareslm.shopping`，使用 Flutter、Riverpod、go_router 和 Dio。当前已完成统一认证、用户/商家门户、账号安全、用户中心，以及商品、交易、商家和消息的 REST 页面。

## 本地运行

```powershell
flutter pub get
flutter run --dart-define=API_BASE_URL=http://10.0.2.2:8080
```

`API_BASE_URL` 未指定时默认使用 Android 模拟器访问宿主机的地址 `http://10.0.2.2:8080`。真机联调时应改为开发机可被手机访问的局域网地址，例如：

```powershell
flutter run --dart-define=API_BASE_URL=http://192.168.1.10:8080
```

如果 Windows 的 Hyper-V/WSL 保留了 8080，后端会在没有可见监听进程时仍报告端口被占用。可先用 `netsh interface ipv4 show excludedportrange protocol=tcp` 检查，再让本地后端临时监听未保留端口，例如 9080，并同步运行：

```powershell
flutter run --dart-define=API_BASE_URL=http://10.0.2.2:9080
```

Debug 构建允许访问本地 HTTP 后端；Release 构建禁止明文 HTTP，发布环境必须传入 HTTPS 地址。

## 目录边界

```text
lib/
├── app/                         # 应用装配、依赖、路由和中央模块注册表
├── core/                        # 配置、网络、安全会话、设备标识
└── features/
    ├── account/                 # 认证、账号安全、设备、资料、地址、偏好与消费统计
    ├── merchant/                # 入驻申请、店铺首页、客服账号、经营统计
    ├── product/                 # 搜索/详情、商家商品列表与创建
    ├── trade/                   # 购物车、结算、订单、模拟支付与退款
    └── message/                 # 客服会话、通知、商家收件箱（REST）
```

`app/module_registry.dart` 已统一注册五个模块，各模块在自己的 `module.dart` 中贡献路由。业务模块必须复用 `apiClientProvider` 和 `core` 认证能力，不得自行创建 Dio 客户端或信任客户端提交的数据范围。

## 认证行为

- 登录固定提交 `deviceType: ANDROID`，稳定设备 ID 存放于 Android 安全存储。登录须选择用户或商家身份。
- Access Token 自动注入 Bearer 请求头；并发 401 共用一次 Refresh Token 轮换。
- 刷新失败会清除本地会话并跳转登录页。
- 用户门户要求 `USER`；商家门户要求 `MERCHANT_OWNER` / `MERCHANT_STAFF` / `CUSTOMER_SERVICE`（客服仅用户沟通）。不满足所选身份时进入无权限页。该限制用于改善客户端交互，服务端鉴权仍是安全边界。
- `mustChangePassword` 时仅允许 `/change-password?forced=1`。
- 注册和修改密码遵循后端 12–64 位强密码规则；修改成功后立即清除本地会话并要求使用新密码登录。注册页可切换商家入驻（multipart 申请）。
- 登录、注册、当前用户、改密、设备列表、会话撤销和退出接口以 `docs/api/auth.md` 为准。
- Token、密码和完整隐私数据不得写入日志或测试文件。

## 用户中心

- 个人资料：查询和更新昵称、头像地址、真实姓名、性别、生日和个人简介。
- 收货地址：列表、新增、编辑、设为默认和删除；用户 ID 始终由服务端认证上下文取得。
- 偏好设置：查询和更新营销、订单及系统通知开关，并原样保留后端返回的扩展偏好。
- 登录设备：展示当前及历史设备、脱敏 IP 和会话有效期，支持退出指定设备或其他全部设备。
- 消费统计：按最多 31 个自然日查询本人支付订单、支付总额、成功退款和有效评价，不接受客户端用户 ID。
- 所有接口复用统一 Dio 客户端和 Token 自动刷新逻辑，契约以 `docs/api/user.md` 为准。

## 成员接入

1. 只修改本人 `features/<module>/` 目录，并在该目录的 `module.dart` 中增加稳定契约对应的 `GoRoute`。
2. 数据模型、接口封装和页面分别放入模块自己的 `domain/`、`data/` 和 `presentation/`；跨模块类型需先确认共享契约。
3. 通过 Riverpod 复用 `apiClientProvider`，不要修改 `app/module_registry.dart`，也不要复制登录、Token 刷新或安全存储实现。
4. 没有后端接口文档时保持空注册点；新增页面后执行静态检查、测试、Debug APK 构建和至少一条真实接口联调。

## 验证

```powershell
flutter analyze
flutter test
flutter build apk --debug
```

Debug APK 输出到 `build/app/outputs/flutter-apk/app-debug.apk`，构建目录不提交仓库。

Android 16 / API 36 模拟器已验证注册、登录、Token 自动刷新、资料、地址、偏好、修改密码、新密码重新登录和退出完整闭环；设备管理接口已完成真实 HTTP 多设备撤销联调，页面待下一次模拟器回归时复核视觉与交互。
