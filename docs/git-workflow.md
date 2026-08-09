# Git 协作规范

## 分支

- `main`：稳定、可演示或发布的版本。
- `develop`：团队日常集成分支。
- `feature/<module>`：功能开发分支，从 `develop` 创建；完成自测和文档更新后由项目管理员直接合并或推送至 `develop`。
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

功能完成后，项目管理员确认构建、测试、冲突检查和文档更新均已完成，即可直接合并或推送至 `develop`；合并完成后删除远程功能分支。其他成员不得直接推送 `develop`。

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

1. 项目管理员可直接向 `develop` 推送或合并；其他成员须通过功能分支提交，由项目管理员集成。
2. 不得直接向 `main` 推送或合并。`develop` 合并至 `main` 必须通过 Pull Request，且至少 1 位非作者成员批准并解决全部讨论后方可合并。
3. 每个 Pull Request 只处理一个清晰主题。
4. 合并前完成构建、测试和冲突检查。
5. 不提交密码、Token、私钥、生产配置、依赖目录或构建产物。
6. 接口、数据库迁移或共享模型变更必须同步更新 `docs/`。

## 发布

每个可验收版本由 `develop` 通过经非作者批准的 Pull Request 合并到 `main`，并打标签。合并完成后执行：

```bash
git switch main
git pull origin main
git merge --no-ff develop
git tag -a v0.1.0 -m "First project milestone"
git push origin main --tags
```
