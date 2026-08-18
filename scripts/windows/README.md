# Windows 本地部署与清理

从仓库根目录运行：

```powershell
# 首次运行会从 example 创建缺失的 .env、docker-compose.yml 和应用配置；填写必填 Secret 后再次运行
powershell -ExecutionPolicy Bypass -File scripts/windows/deploy-local.ps1

# 强制重建容器，并重新检查前端依赖
powershell -ExecutionPolicy Bypass -File scripts/windows/deploy-local.ps1 -Recreate -InstallDependencies

# 只启动 Docker 后端与中间件
powershell -ExecutionPolicy Bypass -File scripts/windows/deploy-local.ps1 -SkipFrontends
```

安全清理：

```powershell
# 推荐日常使用：保留数据库、上传文件、本地配置和 node_modules
powershell -ExecutionPolicy Bypass -File scripts/windows/clean-local.ps1

# 同时删除前端依赖
powershell -ExecutionPolicy Bypass -File scripts/windows/clean-local.ps1 -RemoveDependencies

# 删除本地配置；example 模板始终保留
powershell -ExecutionPolicy Bypass -File scripts/windows/clean-local.ps1 -RemoveLocalConfig

# 高风险：删除 D:\Project\data\shopping 下的全部项目持久化数据
powershell -ExecutionPolicy Bypass -File scripts/windows/clean-local.ps1 -PurgePersistentData
```

`-PurgePersistentData` 默认要求输入精确的 `PURGE`；CI 或明确无需交互时才同时传入 `-Force`。
