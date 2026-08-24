# 用户 Web

普通消费者使用的 Vue 3 + TypeScript Web 客户端。当前提供注册、登录、Token 自动刷新、个人资料、收货地址、通知偏好、登录设备管理，以及已接入的商品和交易页面。

```powershell
npm install
npm run dev
```

默认访问 `http://127.0.0.1:5174`，后端地址为 `http://localhost:8080`；可复制 `.env.example` 并通过 `VITE_API_BASE_URL` 覆盖。

登录设备页面支持查看当前及历史设备、脱敏 IP 和会话有效期，并可退出指定设备或其他全部设备。商家与消息模块仍等待对应负责人提供稳定契约。
