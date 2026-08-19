# 商家模块数据库设计

商家模块由 `database/V8__merchant_tables.sql` 一次建立入驻申请、资质文件、店铺和客服账号（含平台审核状态机）。

## 核心表

- `merchant_application`：入驻申请、主体和联系人信息、审核结果、邮件投递状态。
- `merchant_qualification_file`：资质文件元数据及私有存储键，不保存公开 URL。
- `shop`：审核通过后的店铺；`owner_user_id` 唯一关联商家主账号。
- `shop_staff`：店铺客服；`user_id` 全局唯一。状态 `PENDING_AUDIT` / `ACTIVE` / `REJECTED` / `REVOKED` / `DISABLED`，含 `audit_remark`。

V8 同时为 `user` 增加 `must_change_password`，用于新商家账号首次登录强制改密。

## 状态机

申请状态：

```text
SUBMITTED
├─ REJECTED
└─ QUALIFICATION_APPROVED（过渡态，资质通过后立即开通）
   ├─ REJECTED（存量账号审核驳回）
   └─ ACCOUNT_APPROVED
```

店铺经营权限：

```text
OPEN ←→ SUSPENDED（撤销 / 重新授予）
FROZEN / CLOSED 预留给后续店铺管理
```

邮件状态：

```text
PENDING → SENT
        └→ MAIL_FAILED → SENT
        └→ SKIPPED（系统 SMTP 关闭，不发信）
```

店铺状态与需求基线一致：

```text
OPEN / SUSPENDED / FROZEN / CLOSED
```

本期资质审核通过后直接创建 `OPEN` 店铺。撤销使用 `SUSPENDED`，重新授予恢复 `OPEN`。冻结和注销管理接口后续在店铺管理能力中实现。

## 数据保护

- 证件号与统一社会信用代码仅用于审核，管理列表不返回明文。
- 资质文件写入 `app.merchant.upload-dir`。本机默认 `D:/Project/data/shopping/uploads`，Compose 通过 `MERCHANT_UPLOAD_DIR=/data/uploads` 覆盖；禁止静态目录映射。
- 数据库只保存随机存储键、原始文件名、内容类型、大小和摘要。
- 商品图片写入同一上传根目录的 `product/` 前缀，通过 `GET /api/product-media/{id}` 公开读取；资质文件仍禁止静态映射。
- 账号密码只保存 BCrypt 哈希；临时明文密码仅在创建后交给邮件发送组件，不持久化。

## 权限

V8 新增：

- `merchant:qualification:audit`，授予 `ADMIN` 和 `SUPER_ADMIN`。资质审核（含开通）、材料预览下载、开通/撤销邮件重试、撤销与重新授予均使用该权限。
- `merchant:staff:manage`，仅授予 `MERCHANT_OWNER`，用于提交客服申请与重发已开通邮件。客服只授予 `CUSTOMER_SERVICE`。
- `merchant:staff:audit`，授予 `ADMIN` 和 `SUPER_ADMIN`，用于审核、撤销和重新授予客服账号。商家提交后账号保持禁用，平台通过后才可登录。

`MERCHANT_OWNER` 已由 V1 预置，审核通过时由服务端追加，不通过公共角色管理接口分配。

## 跨模块边界

- `merchant` 调用 `auth` 的账号、角色、资料和审计能力，不实现第二套认证。
- `message` 提供 SMTP 邮件投递；发送失败不回滚商家审核事务。
- `product`、`trade`、`review` 已使用 `shop_id`，后续应通过服务端商家上下文校验店铺归属，不信任客户端提交的数据范围。
