# 商家入驻与资质审核 API

## 1. 业务流程

商家入驻采用一次资质审核。通过后立即开通账号与店铺，不再单独进行账号审核：

```text
提交申请 SUBMITTED
→ 资质审核
   ├─ REJECTED
   └─ 开通账号 ACCOUNT_APPROVED（创建或复用账号、授予 MERCHANT_OWNER、创建 OPEN 店铺）
→ 邮件通知（新账号包含一次性临时密码）
```

存量仍处于 `QUALIFICATION_APPROVED` 的申请出现在待审核队列，通过时走同一开通路径。

已开通商家可被撤销：店铺变为 `SUSPENDED`，收回 `MERCHANT_OWNER` 并作废刷新令牌，同时发送邮件。已撤销商家可重新授予：店铺恢复 `OPEN` 并补回角色。

新建账号首次登录返回 `mustChangePassword=true`。在完成本人改密前，服务端仅允许访问当前账号、改密和退出接口。

## 2. 提交申请

`POST /api/merchant/applications`

- 无需登录。
- `Content-Type: multipart/form-data`
- `application`：JSON 部分。
- `files`：1～5 个资质文件，仅允许 PDF、JPEG、PNG，单文件不超过 10 MB。

`application` 示例：

```json
{
  "merchantType": "ENTERPRISE",
  "shopName": "示例店铺",
  "subjectName": "示例科技有限公司",
  "unifiedSocialCreditCode": "91310000XXXXXXXXXX",
  "responsiblePersonName": "张三",
  "identityDocumentType": "MAINLAND_ID_CARD",
  "identityDocumentNumber": "310101XXXXXXXXXXXX",
  "contactPhone": "13800138000",
  "contactEmail": "merchant@example.com"
}
```

`merchantType`：

- `ENTERPRISE`：企业，主体名称、统一社会信用代码必填。
- `SOLE_PROPRIETOR`：个体工商户，主体名称、统一社会信用代码必填。
- `INDIVIDUAL`：个人商家，不要求统一社会信用代码。

成功返回申请编号和 `SUBMITTED` 状态。相同邮箱存在未结束申请时返回冲突。

## 3. 平台审核

以下接口均要求 `merchant:qualification:audit` 权限：

- `GET /api/admin/merchant/applications`：按状态分页查询。`status` 可为申请原状态，或队列别名 `PENDING`（待审核）、`APPROVED`（店铺 OPEN）、`REVOKED`（店铺 SUSPENDED）。
- `GET /api/admin/merchant/applications/{id}`：查看申请详情；证件号仅返回掩码。列表与详情包含 `shopStatus`。
- `GET /api/admin/merchant/applications/{id}/files/{fileId}`：鉴权获取资质文件。图片与 PDF 使用 inline，便于审核页预览。
- `POST /api/admin/merchant/applications/{id}/qualification-audit`：资质通过或驳回；通过时同时开通商家账号。
- `POST /api/admin/merchant/applications/{id}/account-audit`：仅用于存量 `QUALIFICATION_APPROVED` 申请的开通或驳回。
- `POST /api/admin/merchant/applications/{id}/credential-email/retry`：重试开通或撤销通知邮件。
- `POST /api/admin/merchant/applications/{id}/revoke`：撤销已开通商家。
- `POST /api/admin/merchant/applications/{id}/restore`：重新授予已撤销商家。

审核请求：

```json
{
  "approved": true,
  "reason": "材料真实且在有效期内"
}
```

状态转换使用数据库条件更新保证幂等，重复或越级审核返回状态冲突。

## 4. 账号开通规则

- 邮箱已有普通用户账号：复用账号并追加 `MERCHANT_OWNER`，不更改原密码，发送权限开通通知。
- 邮箱尚无账号：生成满足强密码策略的随机临时密码，创建账号并设置 `mustChangePassword=true`，邮件发送临时密码。
- 审核事务创建店铺并建立唯一店主关系。
- 撤销将店铺置为 `SUSPENDED` 并收回商家角色，邮件失败不回滚撤销结果。
- 重新授予将店铺恢复为 `OPEN` 并补回商家角色。
- SMTP 失败不回滚审核与账号；申请记录为 `MAIL_FAILED`，平台官员可重试。
- 临时密码、身份证件号码、SMTP 密码不得写入日志或审计详情。

## 5. 店铺客服账号

商家主账号（权限 `merchant:staff:manage`）在用户 Web 提交客服申请，平台管理员审核通过后才开通登录，规则与商家入驻一致：商家不能自行启用账号。客服只有 `CUSTOMER_SERVICE` 角色，不含 `USER`，不能登录管理端（`deviceType=ADMIN_WEB` 返回 `40301`）。客服须在用户 Web 选择商家身份登录，且只能访问「用户沟通」。

商家接口：

- `GET /api/merchant/shop`（`product:create` / `product:update` / `product:stock:adjust` / `merchant:staff:manage`）：当前账号所属已开通店铺 `{ id, name, status }`。
- `GET /api/merchant/staff`：本店客服列表，含 `shopId`、`shopName`。
- `POST /api/merchant/staff`：提交客服申请。请求 `{ email, displayName, username? }`。账号先为禁用，记录状态 `PENDING_AUDIT`，此时不发开通邮件。
- `POST /api/merchant/staff/{staffId}/credential-email/retry`：仅已通过且仍须改密时可重发临时密码。

平台审核接口均要求 `merchant:staff:audit`：

- `GET /api/admin/merchant/staff`：按 `status` 筛选（`PENDING_AUDIT` / `ACTIVE` / `REJECTED` / `REVOKED`）。每条含所属 `shopId`、`shopName`。
- `POST /api/admin/merchant/staff/{staffId}/audit`：`{ result: APPROVE|REJECT, remark? }`。通过后激活账号、发放临时密码并发送开通邮件（SMTP 关闭则为固定初始密码且 `SKIPPED`）。
- `POST /api/admin/merchant/staff/{staffId}/revoke`：撤销已通过客服，禁用登录。
- `POST /api/admin/merchant/staff/{staffId}/restore`：对已驳回或已撤销客服重新授予。
- `POST /api/admin/merchant/staff/{staffId}/credential-email/retry`：已通过且仍须改密时重发。

客服状态：`PENDING_AUDIT` → `ACTIVE`（通过）或 `REJECTED`（驳回）；`ACTIVE` → `REVOKED`；`REJECTED` / `REVOKED` → `ACTIVE`（重新授予）。

新建客服不得复用已有邮箱/用户名。

## 6. SMTP 环境变量

```text
MAIL_HOST
MAIL_PORT
MAIL_USERNAME
MAIL_PASSWORD
MAIL_FROM
MAIL_SMTP_AUTH
MAIL_SMTP_STARTTLS_ENABLED
```

生产环境必须通过密钥管理或部署环境注入，不得提交到仓库。
