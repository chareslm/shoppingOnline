-- ============================================================
-- 交易模块 - 支付建表脚本
-- 文件: database/transaction/003_payment.sql
-- 说明: payment_order / payment_record / refund_order / reconciliation_record（4 张表）
-- 执行: mysql -uroot shopping < 003_payment.sql
-- 依赖: 引用成员1 user 表的 ID（不建外键）
-- 说明: 模拟支付(MOCK_WECHAT), 预留微信支付字段(out_trade_no/transaction_id/prepay_id)
-- ============================================================

-- 支付单（一单一付）
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
  `version`        INT           NOT NULL DEFAULT 0      COMMENT '乐观锁版本',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_no` (`payment_no`),
  UNIQUE KEY `uk_order` (`order_id`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付单';

-- 支付回调记录（幂等: 同一支付单同一回调类型只处理一次）
CREATE TABLE IF NOT EXISTS `payment_record` (
  `id`               BIGINT       NOT NULL                COMMENT '雪花ID(应用层生成)',
  `payment_order_id` BIGINT       NOT NULL                COMMENT '关联支付单',
  `callback_type`    VARCHAR(20)  NOT NULL                COMMENT 'PAY/REFUND',
  `raw_data`         TEXT         NULL                    COMMENT '回调原始数据',
  `status`           TINYINT      NOT NULL DEFAULT 0      COMMENT '0待处理/1已处理/2重复',
  `process_result`   VARCHAR(255) NULL                    COMMENT '处理结果说明',
  `created_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '回调时间',
  PRIMARY KEY (`id`),
  KEY `idx_payment` (`payment_order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付回调记录';

-- 退款单
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
  `version`           INT           NOT NULL DEFAULT 0      COMMENT '乐观锁版本',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_refund_no` (`refund_no`),
  KEY `idx_order` (`order_id`),
  KEY `idx_payment` (`payment_order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='退款单';

-- 对账记录（按日对账）
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

-- 模拟支付说明: 支付成功时服务层生成 transaction_id(模拟), 回调写入 payment_record,
-- 幂等键 = payment_order_id + callback_type。