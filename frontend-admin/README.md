# Shopping 管理端

Vue 3 + Vite + TypeScript 的统一运营管理端，包含密码登录、JWT 会话续期、路由守卫和基于权限的菜单基础框架。

## 启动

```bash
npm install
Copy-Item .env.example .env
npm run dev
```

默认在 `http://localhost:5173` 启动，与后端默认 CORS 配置一致。可通过 `VITE_API_BASE_URL` 修改 API 地址。

认证接口与权限约定见仓库根目录的 `docs/api/auth.md`。
