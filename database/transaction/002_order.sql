-- ============================================================
-- 交易模块 - 订单建表脚本
-- 文件: database/transaction/002_order.sql
-- 说明: order / order_item / stock_reservation / order_operation_log（4 张表）
-- 执行: mysql -uroot shopping < 002_order.sql
-- 依赖: 引用成员1 user 表、成员3 sku 表的 ID（不建外键）
-- ============================================================

-- 订单主表（按商家拆单: 一个订单只属于一个店铺）
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
  `version`          INT           NOT NULL DEFAULT 0      COMMENT '乐观锁版本',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_user` (`user_id`),
  KEY `idx_shop` (`shop_id`),
  KEY `idx_status` (`status`),
  CONSTRAINT `chk_order_status` CHECK (`status` BETWEEN 0 AND 7)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单主表';

-- 订单项
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
  `version`      INT           NOT NULL DEFAULT 0      COMMENT '乐观锁版本',
  PRIMARY KEY (`id`),
  KEY `idx_order` (`order_id`),
  KEY `idx_sku` (`sku_id`),
  CONSTRAINT `chk_order_item_quantity` CHECK (`quantity` > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单项';

-- 库存预占记录（下单预占 + 超时释放核心表）
-- 实际库存扣减由服务层调用成员3库存接口完成, 本表记录预占生命周期
CREATE TABLE IF NOT EXISTS `stock_reservation` (
  `id`          BIGINT    NOT NULL                COMMENT '雪花ID(应用层生成)',
  `order_id`    BIGINT    NOT NULL                COMMENT '关联订单',
  `sku_id`      BIGINT    NOT NULL                COMMENT 'SKU ID',
  `quantity`    INT       NOT NULL                COMMENT '预占数量',
  `status`      TINYINT   NOT NULL DEFAULT 0      COMMENT '0预占中/1已扣减/2已释放',
  `expire_time` DATETIME  NOT NULL                COMMENT '预占过期时间(=订单支付超时时间)',
  `created_at`  DATETIME  NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`  DATETIME  NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `version`     INT       NOT NULL DEFAULT 0      COMMENT '乐观锁版本',
  PRIMARY KEY (`id`),
  KEY `idx_order` (`order_id`),
  KEY `idx_expire` (`status`, `expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存预占记录';

-- 订单操作日志（审计: 高风险操作必须留痕）
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