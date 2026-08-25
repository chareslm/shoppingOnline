# V15 用户本人统计权限与索引

迁移文件：`database/V15__user_self_statistics_permission_and_indexes.sql`。

V15 创建 `statistics:self:view` 权限并初始授予 `USER`。本人统计接口不接收 `userId`，服务端只能从已认证主体取得用户范围；客户端菜单和时间参数不能扩大数据范围。

迁移为支付、退款和评价分别增加 `(user_id, status, 业务时间)` 联合索引，以支持按本人、状态和 `[startAt, endAt)` 区间执行精确聚合。V15 不创建统计业务表、缓存或汇总表，MySQL 权威业务表仍是唯一金额来源。

迁移上线后，迁移前已签发的短期 Access Token 可能暂不包含新权限；用户重新登录或完成 Token 刷新后会取得最新权限。后续不得修改已执行的 V15，索引或授权调整必须使用更高版本迁移。
