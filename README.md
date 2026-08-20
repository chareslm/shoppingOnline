# 电商平台课程设计

本项目为五人合作开发的综合电商平台课程设计。

## 项目组成

- `backend`：后端项目。
- `frontend-web`：用户 Web 商城。
- `frontend-app`：Flutter Android App。
- `frontend-miniapp`：微信原生小程序。
- `frontend-admin`：管理员及商家管理端。
- `deploy`：Docker Compose 及 MySQL、Redis、Elasticsearch、Kibana、Filebeat 部署配置。
- `database`：数据库建表、初始化和迁移脚本。
- `docs`：需求、架构、接口和协作文档。

## Git 分支

- `main`：稳定版本。
- `develop`：开发集成版本。
- `feature/*`：功能开发分支。
- `fix/*`：Bug 修复分支。

日常开发从 `develop` 创建功能分支，并通过 Pull Request 合并回 `develop`。详细规则见 [docs/git-workflow.md](docs/git-workflow.md)。

微信小程序的导入、API 地址和本地联调说明见 [frontend-miniapp/README.md](frontend-miniapp/README.md)。

统计模块的指标口径、权限、数据范围、事件和聚合边界见 [docs/architecture/statistics-foundation.md](docs/architecture/statistics-foundation.md)。
