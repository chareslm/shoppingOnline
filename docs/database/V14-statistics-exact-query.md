# V14 统计精确查询权限与索引

迁移文件：`database/V14__statistics_exact_query_permissions_and_indexes.sql`。

V14 创建 `statistics:platform:view` 和 `statistics:shop:view` 两项独立权限，分别初始授予 `SUPER_ADMIN` 与 `MERCHANT_OWNER`。客服和普通平台管理员不默认获得经营或平台统计权限。

迁移同时为用户创建时间、支付成功时间、退款完成时间、店铺商品状态和评价状态／创建时间增加组合或范围查询索引。第一阶段不创建统计聚合表，金额、订单、退款和商品指标继续直接读取 MySQL 权威业务表。

本地 V13→V14 升级和临时空库 V1→V14 全量迁移均已通过。`EXPLAIN` 已确认支付、退款和评价区间查询命中新联合索引，商品查询能够识别 `idx_spu_shop_status`；本地数据量很小时优化器可能选择既有 `idx_shop`。上线前仍应在真实数据量上复核执行计划；后续索引调整必须通过更高版本迁移完成，不得修改已经执行的 V14。
