-- ============================================================
-- 评价模块建表迁移（评价 / 商家回复）+ 评价权限
-- 文件: database/V6__review_tables.sql
-- 说明: 成员3 评价模块。评价必须基于可信交易资格（已完成订单项）。
-- 依赖: 引用成员1 user 表、成员2 shop 表、成员3 sku/spu 表、成员4 order/order_item 表（不建外键）。
-- ============================================================

-- ---------- 评价 ----------
CREATE TABLE IF NOT EXISTS `review` (
  `id`           BIGINT        NOT NULL                COMMENT '雪花ID(应用层生成)',
  `order_id`     BIGINT        NOT NULL                COMMENT '关联订单',
  `order_item_id` BIGINT       NOT NULL                COMMENT '关联订单项(评价资格, 一订单项一评价)',
  `spu_id`       BIGINT        NOT NULL                COMMENT '商品SPU',
  `sku_id`       BIGINT        NOT NULL                COMMENT '商品SKU',
  `user_id`      BIGINT        NOT NULL                COMMENT '评价用户(引用成员1 user表)',
  `shop_id`      BIGINT        NOT NULL                COMMENT '店铺ID(引用成员2 shop表)',
  `rating`       TINYINT       NOT NULL                COMMENT '评分 1~5',
  `content`      VARCHAR(1000) NULL                    COMMENT '评价内容',
  `images`       JSON          NULL                    COMMENT '晒图URL数组',
  `is_anonymous` TINYINT       NOT NULL DEFAULT 0      COMMENT '1匿名/0实名',
  `status`       VARCHAR(32)   NOT NULL DEFAULT 'DISPLAYED' COMMENT 'DISPLAYED显示/HIDDEN隐藏',
  `created_at`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by`   BIGINT        NULL                    COMMENT '创建人ID',
  `updated_by`   BIGINT        NULL                    COMMENT '更新人ID',
  `version`      INT           NOT NULL DEFAULT 0      COMMENT '乐观锁版本',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_review_order_item` (`order_item_id`),
  KEY `idx_spu` (`spu_id`),
  KEY `idx_sku` (`sku_id`),
  KEY `idx_user` (`user_id`),
  KEY `idx_shop_status` (`shop_id`, `status`),
  CONSTRAINT `chk_review_rating` CHECK (`rating` BETWEEN 1 AND 5),
  CONSTRAINT `chk_review_status` CHECK (`status` IN ('DISPLAYED', 'HIDDEN'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品评价';

-- ---------- 商家回复（一评价一回复） ----------
CREATE TABLE IF NOT EXISTS `review_reply` (
  `id`         BIGINT        NOT NULL                COMMENT '雪花ID(应用层生成)',
  `review_id`  BIGINT        NOT NULL                COMMENT '关联评价',
  `shop_id`    BIGINT        NOT NULL                COMMENT '店铺ID',
  `content`    VARCHAR(1000) NOT NULL                COMMENT '回复内容',
  `replied_by` BIGINT        NULL                    COMMENT '回复人ID(商家账号)',
  `created_at` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` BIGINT        NULL                    COMMENT '创建人ID',
  `updated_by` BIGINT        NULL                    COMMENT '更新人ID',
  `version`    INT           NOT NULL DEFAULT 0      COMMENT '乐观锁版本',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_reply_review` (`review_id`),
  KEY `idx_shop` (`shop_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商家回复';

-- ---------- 评价权限 ----------
INSERT INTO permission (code, name, resource, action, description, status) VALUES
    ('review:reply', '回复评价', 'review', 'reply', '商家回复本店评价', 'ACTIVE'),
    ('review:audit', '审核评价', 'review', 'audit', '管理员隐藏/恢复评价', 'ACTIVE')
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    description = VALUES(description),
    status = VALUES(status);

INSERT IGNORE INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM `role` r
INNER JOIN permission p ON p.code = 'review:reply'
WHERE r.code IN ('MERCHANT_OWNER', 'MERCHANT_STAFF');

INSERT IGNORE INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM `role` r
INNER JOIN permission p ON p.code IN ('review:reply', 'review:audit')
WHERE r.code IN ('ADMIN', 'SUPER_ADMIN');
