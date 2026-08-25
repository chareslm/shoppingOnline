# 微信原生小程序

本目录是综合电商平台的微信原生 TypeScript 用户端，当前覆盖统一账号认证、用户中心、设备与会话管理，以及交易（购物车/结算/支付/订单）能力。

## 本地开发

1. 执行 `npm install`、`npm run typecheck` 和 `npm test`。
2. 使用微信开发者工具导入本目录；仓库中的 `project.config.json` 固定使用 `touristappid`。
3. 复制 `project.private.config.json.example` 为被 Git 忽略的 `project.private.config.json`，把其中的 `appid` 换成本人的测试 AppID；本地联调时可在该文件关闭合法域名校验。不要把个人测试 AppID 写回公共配置。
4. 开发 API 默认是 `http://127.0.0.1:8080`。需要临时使用其他端口时，在小程序调试器 Console 执行以下命令，无需修改源码：

```js
wx.setStorageSync('shopping.apiBaseUrl.development', 'http://127.0.0.1:9080')
```

恢复仓库默认地址：

```js
wx.removeStorageSync('shopping.apiBaseUrl.development')
```

开发者工具访问本机后端时可使用回环地址；真机无法通过 `127.0.0.1` 访问电脑，需要使用同一局域网地址或已备案的 HTTPS 测试域名。正式版必须配置 HTTPS API 地址，并在微信公众平台登记 `request` 合法域名。

## 已实现范围

- 注册、密码登录、启动会话恢复、本人改密、当前设备退出、设备列表、指定设备退出和其他设备退出。
- Bearer Token 注入、并发 401 单次刷新、刷新失败清理本地会话。
- `USER` 角色访问限制。
- 个人资料、收货地址和偏好设置。
- 交易模块：购物车（分组/勾选/数量/删除）、结算下单（地址选择/备注）、收银台模拟支付、订单列表（状态筛选）与订单详情（去支付/取消/确认收货/申请退款）；因小程序暂无商品页，购物车内置"联调辅助"手动加购入口。
- `account/merchant/product/trade/message` 模块注册机制；merchant/product/message 模块当前为空注册点。

Token 和稳定设备 ID 使用微信小程序本地存储保存，不写日志。小程序没有等同 Android Keystore 的通用安全存储能力，因此服务端仍须依赖短期 Access Token、Refresh Token 轮换和设备撤销控制风险。
