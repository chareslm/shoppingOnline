# 商家入驻与资质审核 API

## 1. 业务流程

商家入驻采用两阶段审核：

```text
提交申请 SUBMITTED
→ 资质审核 QUALIFICATION_APPROVED / REJECTED
→ 账号审核 ACCOUNT_APPROVED
→ 创建或复用账号、授予 MERCHANT_OWNER、创建店铺
→ 邮件通知（新账号包含一次性临时密码）
```

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

- `GET /api/admin/merchant/applications`：按状态、关键词分页查询。
- `GET /api/admin/merchant/applications/{id}`：查看申请详情；证件号仅返回掩码。
- `GET /api/admin/merchant/applications/{id}/files/{fileId}`：鉴权下载资质文件。
- `POST /api/admin/merchant/applications/{id}/qualification-audit`：资质通过或驳回。
- `POST /api/admin/merchant/applications/{id}/account-audit`：账号通过或驳回。
- `POST /api/admin/merchant/applications/{id}/credential-email/retry`：重试开通邮件。

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
- SMTP 失败不回滚审核与账号；申请记录为 `MAIL_FAILED`，平台官员可重试。
- 临时密码、身份证件号码、SMTP 密码不得写入日志或审计详情。

## 5. SMTP 环境变量

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
