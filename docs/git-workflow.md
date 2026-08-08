# Git 协作规范

## 分支

- `main`：稳定、可演示或发布的版本。
- `develop`：团队日常集成分支。
- `feature/<module>`：功能开发分支，从 `develop` 创建并合并回 `develop`。
- `fix/<issue>`：常规缺陷修复分支。
- `hotfix/<issue>`：线上或演示阻塞问题，从 `main` 创建，修复后同时合并回 `main` 和 `develop`。

建议的模块分支：

- `feature/auth-admin`
- `feature/merchant`
- `feature/product`
- `feature/trade`
- `feature/chat-message`

## 日常流程

```bash
git switch develop
git pull origin develop
git switch -c feature/auth-admin

# 开发并自测后
git add <files>
git commit -m "feat(auth): add password login"
git push -u origin feature/auth-admin
```

在 GitHub 创建 Pull Request，目标分支选择 `develop`。通过审查并解决冲突后合并；合并完成后删除远程功能分支。

## 提交信息

格式：`<type>(<scope>): <summary>`。

- `feat`：新功能
- `fix`：缺陷修复
- `docs`：文档变更
- `refactor`：重构
- `test`：测试
- `chore`：工程、构建或配置

示例：

```text
feat(auth): implement refresh token rotation
fix(order): reject duplicate payment callback
docs(api): define login response contract
```

## 合并要求

1. 不直接向 `main` 或 `develop` 推送。
2. 每个 Pull Request 只处理一个清晰主题。
3. 合并前完成构建、测试和冲突检查。
4. 不提交密码、Token、私钥、生产配置、依赖目录或构建产物。
5. 接口、数据库迁移或共享模型变更必须同步更新 `docs/`。

## 发布

每个可验收版本由 `develop` 合并到 `main`，并打标签：

```bash
git switch main
git pull origin main
git merge --no-ff develop
git tag -a v0.1.0 -m "First project milestone"
git push origin main --tags
```
