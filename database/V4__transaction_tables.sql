-- ============================================================
-- 交易模块建表迁移（购物车 / 订单 / 支付）
-- 文件: database/V4__transaction_tables.sql
-- 说明: 合并 database/transaction/001-004 为 Flyway 版本化迁移。
--       新环境：CREATE TABLE IF NOT EXISTS 完整建表（含审计列与唯一约束）。
--       旧环境（已手动执行 001-004）：建表跳过，幂等 ALTER 补齐 payment_record 唯一约束。
-- 依赖: 引用成员1 user 表、成员2 shop 表、成员3 sku 表的 ID（不建外键）
-- ============================================================

-- ---------- 购物车（一用户一购物车） ----------
CREATE TABLE IF NOT EXISTS `cart` (
  `id`         BIGINT       NOT NULL                COMMENT '雪花ID(应用层生成)',
  `user_id`    BIGINT       NOT NULL                COMMENT '用户ID(引用成员1 user表)',
  `status`     TINYINT      NOT NULL DEFAULT 1      COMMENT '1有效/0停用',
  `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` BIGINT       NULL                    COMMENT '创建人ID',
  `updated_by` BIGINT       NULL                    COMMENT '更新人ID',
  `version`    INT          NOT NULL DEFAULT 0      COMMENT '乐观锁版本',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='购物车';

-- ---------- 购物车分组（按商家分组） ----------
CREATE TABLE IF NOT EXISTS `cart_group` (
  `id`         BIGINT       NOT NULL                COMMENT '雪花ID(应用层生成)',
  `cart_id`    BIGINT       NOT NULL                COMMENT '所属购物车',
  `shop_id`    BIGINT       NOT NULL                COMMENT '商家ID(引用成员2 shop表)',
  `status`     TINYINT      NOT NULL DEFAULT 1      COMMENT '1有效/0停用',
  `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` BIGINT       NULL                    COMMENT '创建人ID',
  `updated_by` BIGINT       NULL                    COMMENT '更新人ID',
  `version`    INT          NOT NULL DEFAULT 0      COMMENT '乐观锁版本',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_cart_shop` (`cart_id`, `shop_id`),
  KEY `idx_shop` (`shop_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='购物车分组(按商家)';

-- ---------- 购物项 ----------
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
  `created_by`     BIGINT       NULL                    COMMENT '创建人ID',
  `updated_by`     BIGINT       NULL                    COMMENT '更新人ID',
  `version`        INT          NOT NULL DEFAULT 0      COMMENT '乐观锁版本',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_cart_sku` (`cart_id`, `sku_id`),
  KEY `idx_group` (`group_id`),
  KEY `idx_sku` (`sku_id`),
  CONSTRAINT `chk_cart_item_quantity` CHECK (`quantity` > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='购物项';

-- ---------- 订单主表（按商家拆单） ----------
-- 状态机: 0待支付->1已支付->2已发货->3已完成; 0->4已取消; 0->5已关闭(超时); 1/2->6退款中->7已退款
CREATE TABLE IF NOT EXISTS `order` (
  `id`               BIGINT        NOT NULL                COMMENT '雪花ID(应用层生成)',
  `order_no`         VARCHAR(32)   NOT NULL                COMMENT '业务订单号',
  `user_id`          BIGINT        NOT NULL                COMMENT '下单用户(引用成员1 user表)',
  `shop_id`          BIGINT        NOT NULL                COMMENT '店铺ID(按商家拆单)',
  `status`           TINYINT       NOT NULL DEFAULT 0      COMMENT '状态机: 0待支付/1已支付/2已发货/3已完成/4已取消/5已关闭/6退款中/7已退款',
  `total_amount`     DECIMAL(10,2) NOT NULL                COMMENT '商品总额',
  `freight_amount`   DECIMAL(10,2) NOT NULL DEFAULT 0      COMMENT '运费',
  `discount_amount`  DECIMAL(10,2) NOT NULL DEFAULT 0      COMMENT '优惠金额',
  `pay_amount`       DECIMAL(10,2) NOT NULL                COMMENT '实付金额=total+freight-discount',
  `receiver_name`    VARCHAR(50)   NULL                    COMMENT '收货人快照',
  `receiver_phone`   VARCHAR(20)   NULL                    COMMENT '收货电话快照',
  `receiver_address` VARCHAR(255)  NULL                    COMMENT '收货地址快照',
  `remark`           VARCHAR(255)  NULL                    COMMENT '订单备注',
  `pay_time`         DATETIME      NULL                    COMMENT '支付时间',
  `close_time`       DATETIME      NULL                    COMMENT '超时关闭时间',
  `finish_time`      DATETIME      NULL                    COMMENT '完成时间',
  `cancel_reason`    VARCHAR(255)  NULL                    COMMENT '取消原因',
  `created_at`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by`       BIGINT        NULL                    COMMENT '创建人ID',
  `updated_by`       BIGINT        NULL                    COMMENT '更新人ID',
  `version`          INT           NOT NULL DEFAULT 0      COMMENT '乐观锁版本',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_user` (`user_id`),
  KEY `idx_shop` (`shop_id`),
  KEY `idx_status` (`status`),
  CONSTRAINT `chk_order_status` CHECK (`status` BETWEEN 0 AND 7)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单主表';

-- ---------- 订单项 ----------
CREATE TABLE IF NOT EXISTS `order_item` (
  `id`           BIGINT        NOT NULL                COMMENT '雪花ID(应用层生成)',
  `order_id`     BIGINT        NOT NULL                COMMENT '所属订单',
  `sku_id`       BIGINT        NOT NULL                COMMENT 'SKU ID(引用成员3 sku表)',
  `sku_name`     VARCHAR(255)  NULL                    COMMENT '商品名快照',
  `sku_image`    VARCHAR(255)  NULL                    COMMENT '商品图快照',
  `price`        DECIMAL(10,2) NULL                    COMMENT '成交单价快照',
  `quantity`     INT           NOT NULL                COMMENT '数量',
  `total_amount` DECIMAL(10,2) NULL                    COMMENT '小计=price*quantity',
  `status`       TINYINT       NOT NULL DEFAULT 0      COMMENT '0正常/1退款中/2已退款',
  `created_at`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by`   BIGINT        NULL                    COMMENT '创建人ID',
  `updated_by`   BIGINT        NULL                    COMMENT '更新人ID',
  `version`      INT           NOT NULL DEFAULT 0      COMMENT '乐观锁版本',
  PRIMARY KEY (`id`),
  KEY `idx_order` (`order_id`),
  KEY `idx_sku` (`sku_id`),
  CONSTRAINT `chk_order_item_quantity` CHECK (`quantity` > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单项';

-- ---------- 库存预占记录 ----------
CREATE TABLE IF NOT EXISTS `stock_reservation` (
  `id`          BIGINT    NOT NULL                COMMENT '雪花ID(应用层生成)',
  `order_id`    BIGINT    NOT NULL                COMMENT '关联订单',
  `sku_id`      BIGINT    NOT NULL                COMMENT 'SKU ID',
  `quantity`    INT       NOT NULL                COMMENT '预占数量',
  `status`      TINYINT   NOT NULL DEFAULT 0      COMMENT '0预占中/1已扣减/2已释放',
  `expire_time` DATETIME  NOT NULL                COMMENT '预占过期时间(=订单支付超时时间)',
  `created_at`  DATETIME  NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`  DATETIME  NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by`  BIGINT    NULL                    COMMENT '创建人ID',
  `updated_by`  BIGINT    NULL                    COMMENT '更新人ID',
  `version`     INT       NOT NULL DEFAULT 0      COMMENT '乐观锁版本',
  PRIMARY KEY (`id`),
  KEY `idx_order` (`order_id`),
  KEY `idx_expire` (`status`, `expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存预占记录';

-- ---------- 订单操作日志（审计） ----------
CREATE TABLE IF NOT EXISTS `order_operation_log` (
  `id`            BIGINT      NOT NULL                COMMENT '雪花ID(应用层生成)',
  `order_id`      BIGINT      NOT NULL                COMMENT '关联订单',
  `operator_type` TINYINT     NOT NULL                COMMENT '1用户/2系统/3管理员',
  `operator_id`   BIGINT      NULL                    COMMENT '操作者ID',
  `action`        VARCHAR(50) NOT NULL                COMMENT 'CREATE/PAY/CANCEL/CLOSE/SHIP/COMPLETE/REFUND',
  `from_status`   TINYINT     NULL                    COMMENT '变更前状态',
  `to_status`     TINYINT     NULL                    COMMENT '变更后状态',
  `remark`        VARCHAR(255) NULL                   COMMENT '备注',
  `created_at`    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`id`),
  KEY `idx_order` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单操作日志';

-- ---------- 支付单（一单一付） ----------
CREATE TABLE IF NOT EXISTS `payment_order` (
  `id`             BIGINT        NOT NULL                COMMENT '雪花ID(应用层生成)',
  `payment_no`     VARCHAR(32)   NOT NULL                COMMENT '支付单号(幂等键)',
  `order_id`       BIGINT        NOT NULL                COMMENT '关联订单(一单一付)',
  `user_id`        BIGINT        NOT NULL                COMMENT '支付用户(引用成员1 user表)',
  `amount`         DECIMAL(10,2) NOT NULL                COMMENT '支付金额',
  `status`         TINYINT       NOT NULL DEFAULT 0      COMMENT '0待支付/1成功/2失败/3已关闭/4已退款',
  `pay_channel`    VARCHAR(20)   NOT NULL DEFAULT 'MOCK_WECHAT' COMMENT '支付渠道(模拟微信)',
  `out_trade_no`   VARCHAR(64)   NULL                    COMMENT '微信商户订单号(模拟=payment_no,预留)',
  `transaction_id` VARCHAR(64)   NULL                    COMMENT '微信支付单号(模拟生成,预留)',
  `prepay_id`      VARCHAR(64)   NULL                    COMMENT '微信预支付ID(预留)',
  `pay_time`       DATETIME      NULL                    COMMENT '支付成功时间',
  `expire_time`    DATETIME      NOT NULL                COMMENT '支付超时时间',
  `callback_time`  DATETIME      NULL                    COMMENT '回调到达时间',
  `callback_raw`   TEXT          NULL                    COMMENT '回调原始报文',
  `created_at`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by`     BIGINT        NULL                    COMMENT '创建人ID',
  `updated_by`     BIGINT        NULL                    COMMENT '更新人ID',
  `version`        INT           NOT NULL DEFAULT 0      COMMENT '乐观锁版本',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_no` (`payment_no`),
  UNIQUE KEY `uk_order` (`order_id`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付单';

-- ---------- 支付回调记录（幂等: 唯一约束保证并发下只有一条处理记录） ----------
CREATE TABLE IF NOT EXISTS `payment_record` (
  `id`               BIGINT       NOT NULL                COMMENT '雪花ID(应用层生成)',
  `payment_order_id` BIGINT       NOT NULL                COMMENT '关联支付单',
  `callback_type`    VARCHAR(20)  NOT NULL                COMMENT 'PAY/REFUND',
  `raw_data`         TEXT         NULL                    COMMENT '回调原始数据',
  `status`           TINYINT      NOT NULL DEFAULT 0      COMMENT '0待处理/1已处理/2重复',
  `process_result`   VARCHAR(255) NULL                    COMMENT '处理结果说明',
  `created_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '回调时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_callback` (`payment_order_id`, `callback_type`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付回调记录';

-- ---------- 退款单 ----------
CREATE TABLE IF NOT EXISTS `refund_order` (
  `id`                BIGINT        NOT NULL                COMMENT '雪花ID(应用层生成)',
  `refund_no`         VARCHAR(32)   NOT NULL                COMMENT '退款单号',
  `payment_order_id`  BIGINT        NOT NULL                COMMENT '关联支付单',
  `order_id`          BIGINT        NOT NULL                COMMENT '关联订单',
  `user_id`           BIGINT        NOT NULL                COMMENT '退款用户',
  `amount`            DECIMAL(10,2) NOT NULL                COMMENT '退款金额',
  `reason`            VARCHAR(255)  NULL                    COMMENT '退款原因',
  `status`            TINYINT       NOT NULL DEFAULT 0      COMMENT '0待处理/1已退款/2失败/3已拒绝',
  `channel_refund_id` VARCHAR(64)   NULL                    COMMENT '渠道退款单号(预留)',
  `refund_time`       DATETIME      NULL                    COMMENT '退款完成时间',
  `created_at`        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by`        BIGINT        NULL                    COMMENT '创建人ID',
  `updated_by`        BIGINT        NULL                    COMMENT '更新人ID',
  `version`           INT           NOT NULL DEFAULT 0      COMMENT '乐观锁版本',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_refund_no` (`refund_no`),
  KEY `idx_order` (`order_id`),
  KEY `idx_payment` (`payment_order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='退款单';

-- ---------- 对账记录（按日对账） ----------
CREATE TABLE IF NOT EXISTS `reconciliation_record` (
  `id`           BIGINT        NOT NULL                COMMENT '雪花ID(应用层生成)',
  `biz_date`     DATE          NOT NULL                COMMENT '对账日期',
  `channel`      VARCHAR(20)   NOT NULL DEFAULT 'MOCK_WECHAT' COMMENT '渠道',
  `total_amount` DECIMAL(12,2) NULL                    COMMENT '渠道侧总金额',
  `total_count`  INT           NULL                    COMMENT '渠道侧总笔数',
  `diff_count`   INT           NOT NULL DEFAULT 0      COMMENT '差异笔数',
  `diff_amount`  DECIMAL(12,2) NOT NULL DEFAULT 0      COMMENT '差异金额',
  `status`       TINYINT       NOT NULL DEFAULT 0      COMMENT '0待对账/1一致/2有差异',
  `detail`       JSON          NULL                    COMMENT '差异明细',
  `created_at`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_biz_date_channel` (`biz_date`, `channel`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对账记录';

-- ---------- 幂等 ALTER：旧环境（已手动执行 001-004）补齐 payment_record 唯一约束 ----------
SET @has_uk = (SELECT COUNT(*) FROM information_schema.STATISTICS
               WHERE table_schema = DATABASE() AND table_name = 'payment_record'
                 AND index_name = 'uk_payment_callback');
SET @ddl = IF(@has_uk = 0,
    'ALTER TABLE `payment_record` ADD UNIQUE KEY `uk_payment_callback` (`payment_order_id`, `callback_type`, `status`)',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;