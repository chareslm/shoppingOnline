-- ============================================================
-- 交易模块 - 购物车建表脚本
-- 文件: database/transaction/001_cart.sql
-- 说明: cart / cart_group / cart_item（3 张表）
-- 执行: mysql -uroot shopping < 001_cart.sql
-- 依赖: 引用成员1 user 表、成员2 shop 表、成员3 sku 表的 ID（不建外键）
-- ============================================================

-- 购物车（一用户一购物车）
CREATE TABLE IF NOT EXISTS `cart` (
  `id`         BIGINT       NOT NULL                COMMENT '雪花ID(应用层生成)',
  `user_id`    BIGINT       NOT NULL                COMMENT '用户ID(引用成员1 user表)',
  `status`     TINYINT      NOT NULL DEFAULT 1      COMMENT '1有效/0停用',
  `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `version`    INT          NOT NULL DEFAULT 0      COMMENT '乐观锁版本',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='购物车';

-- 购物车分组（按商家分组）
CREATE TABLE IF NOT EXISTS `cart_group` (
  `id`         BIGINT       NOT NULL                COMMENT '雪花ID(应用层生成)',
  `cart_id`    BIGINT       NOT NULL                COMMENT '所属购物车',
  `shop_id`    BIGINT       NOT NULL                COMMENT '商家ID(引用成员2 shop表)',
  `status`     TINYINT      NOT NULL DEFAULT 1      COMMENT '1有效/0停用',
  `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `version`    INT          NOT NULL DEFAULT 0      COMMENT '乐观锁版本',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_cart_shop` (`cart_id`, `shop_id`),
  KEY `idx_shop` (`shop_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='购物车分组(按商家)';

-- 购物项
CREATE TABLE IF NOT EXISTS `cart_item` (
  `id`             BIGINT       NOT NULL                COMMENT '雪花ID(应用层生成)',
  `cart_id`        BIGINT       NOT NULL                COMMENT '所属购物车',
  `group_id`       BIGINT       NOT NULL                COMMENT '所属分组',
  `sku_id`         BIGINT       NOT NULL                COMMENT 'SKU ID(引用成员3 sku表)',
  `quantity`       INT          NOT NULL DEFAULT 1      COMMENT '数量',
  `checked`        TINYINT      NOT NULL DEFAULT 1      COMMENT '1勾选结算/0未勾选',
  `price_snapshot` DECIMAL(10,2) NULL                    COMMENT '加入购物车时价格快照,结算时与最新价校验',
  `status`         TINYINT      NOT NULL DEFAULT 1      COMMENT '1有效/0已移除',
  `created_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `version`        INT          NOT NULL DEFAULT 0      COMMENT '乐观锁版本',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_cart_sku` (`cart_id`, `sku_id`),
  KEY `idx_group` (`group_id`),
  KEY `idx_sku` (`sku_id`),
  CONSTRAINT `chk_cart_item_quantity` CHECK (`quantity` > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='购物项';

-- 说明: 价格与有效性校验(上下架/库存)由服务层调用成员3 SKU接口完成,本表只存快照。