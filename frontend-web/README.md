# 用户 Web

普通消费者使用的 Vue 3 + TypeScript Web 客户端。当前提供注册、登录、Token 自动刷新、个人资料、收货地址和通知偏好。

```powershell
npm install
npm run dev
```

默认访问 `http://127.0.0.1:5174`，后端地址为 `http://localhost:8080`；可复制 `.env.example` 并通过 `VITE_API_BASE_URL` 覆盖。

店铺关注、商品、购物车和订单尚未接入，需等待对应后端模块契约稳定。
