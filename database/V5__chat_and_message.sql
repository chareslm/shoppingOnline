-- ============================================================
-- 模块5：聊天与消息通知 建表迁移
-- 文件: database/V5__chat_and_message.sql
-- 说明: 包含客服聊天会话/消息、消息中心模板/通知
--       新环境：CREATE TABLE IF NOT EXISTS 完整建表（含审计列）
--       依赖: 引用成员1 user 表、成员2 shop 表的 ID（不建外键）
-- ============================================================

-- ---------- 客服会话 ----------
CREATE TABLE IF NOT EXISTS `chat_session` (
  `id`                BIGINT       NOT NULL                COMMENT '雪花ID(应用层生成)',
  `user_id`           BIGINT       NOT NULL                COMMENT '发起用户ID(引用成员1 user表)',
  `shop_id`           BIGINT       NULL                    COMMENT '所属商家ID(引用成员2 shop表, 可为空=平台客服)',
  `cs_user_id`        BIGINT       NULL                    COMMENT '分配的客服用户ID(引用成员1 user表)',
  `subject`           VARCHAR(255) NULL                    COMMENT '会话主题/关联订单号',
  `last_message`      VARCHAR(500) NULL                    COMMENT '最后一条消息内容(冗余)',
  `last_message_time` DATETIME     NULL                    COMMENT '最后消息时间(冗余)',
  `status`            TINYINT      NOT NULL DEFAULT 0      COMMENT '0进行中/1已结束',
  `priority`          TINYINT      NOT NULL DEFAULT 0      COMMENT '0普通/1优先',
  `created_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by`        BIGINT       NULL                    COMMENT '创建人ID',
  `updated_by`        BIGINT       NULL                    COMMENT '更新人ID',
  `version`           INT          NOT NULL DEFAULT 0      COMMENT '乐观锁版本',
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`),
  KEY `idx_cs_user` (`cs_user_id`),
  KEY `idx_shop` (`shop_id`),
  KEY `idx_status` (`status`),
  KEY `idx_last_time` (`last_message_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客服会话';

-- ---------- 聊天消息 ----------
CREATE TABLE IF NOT EXISTS `chat_message` (
  `id`          BIGINT       NOT NULL                COMMENT '雪花ID(应用层生成)',
  `session_id`  BIGINT       NOT NULL                COMMENT '所属会话ID',
  `sender_id`   BIGINT       NOT NULL                COMMENT '发送方用户ID',
  `sender_type` TINYINT      NOT NULL                COMMENT '1用户/2客服/3系统',
  `content`     TEXT         NOT NULL                COMMENT '消息内容',
  `msg_type`    TINYINT      NOT NULL DEFAULT 1      COMMENT '1文本/2图片/3商品卡片/4系统通知',
  `extra_data`  JSON         NULL                    COMMENT '扩展数据(如商品卡片JSON)',
  `is_read`     TINYINT      NOT NULL DEFAULT 0      COMMENT '0未读/1已读',
  `read_time`   DATETIME     NULL                    COMMENT '已读时间',
  `status`      TINYINT      NOT NULL DEFAULT 1      COMMENT '1正常/0已撤回',
  `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by`  BIGINT       NULL                    COMMENT '创建人ID',
  `updated_by`  BIGINT       NULL                    COMMENT '更新人ID',
  `version`     INT          NOT NULL DEFAULT 0      COMMENT '乐观锁版本',
  PRIMARY KEY (`id`),
  KEY `idx_session` (`session_id`, `created_at`),
  KEY `idx_sender` (`sender_id`),
  KEY `idx_read` (`session_id`, `is_read`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='聊天消息';

-- ---------- 消息模板 ----------
CREATE TABLE IF NOT EXISTS `message_template` (
  `id`             BIGINT       NOT NULL                COMMENT '雪花ID(应用层生成)',
  `template_code`  VARCHAR(64)  NOT NULL                COMMENT '模板编码(业务唯一, 如 ORDER_PAID)',
  `title`          VARCHAR(255) NOT NULL                COMMENT '模板标题',
  `content`        TEXT         NOT NULL                COMMENT '模板内容(支持变量替换, 如 {orderNo})',
  `category`       TINYINT      NOT NULL                COMMENT '1系统/2订单/3营销/4客服',
  `push_enabled`   TINYINT      NOT NULL DEFAULT 1      COMMENT '1启用推送/0不推送',
  `status`         TINYINT      NOT NULL DEFAULT 1      COMMENT '1启用/0禁用',
  `created_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by`     BIGINT       NULL                    COMMENT '创建人ID',
  `updated_by`     BIGINT       NULL                    COMMENT '更新人ID',
  `version`        INT          NOT NULL DEFAULT 0      COMMENT '乐观锁版本',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_template_code` (`template_code`),
  KEY `idx_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息模板';

-- ---------- 用户通知（站内信） ----------
CREATE TABLE IF NOT EXISTS `user_notification` (
  `id`              BIGINT       NOT NULL                COMMENT '雪花ID(应用层生成)',
  `user_id`         BIGINT       NOT NULL                COMMENT '接收用户ID',
  `template_id`     BIGINT       NULL                    COMMENT '关联模板ID',
  `template_code`   VARCHAR(64)  NULL                    COMMENT '模板编码(冗余)',
  `title`           VARCHAR(255) NOT NULL                COMMENT '通知标题',
  `content`         TEXT         NOT NULL                COMMENT '通知内容(已渲染变量)',
  `category`        TINYINT      NOT NULL                COMMENT '1系统/2订单/3营销/4客服',
  `biz_type`        VARCHAR(50)  NULL                    COMMENT '关联业务类型(如 ORDER)',
  `biz_id`          VARCHAR(64)  NULL                    COMMENT '关联业务ID',
  `is_read`         TINYINT      NOT NULL DEFAULT 0      COMMENT '0未读/1已读',
  `read_time`       DATETIME     NULL                    COMMENT '已读时间',
  `push_status`     TINYINT      NOT NULL DEFAULT 0      COMMENT '0未推送/1推送成功/2推送失败',
  `push_time`       DATETIME     NULL                    COMMENT '推送时间',
  `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by`      BIGINT       NULL                    COMMENT '创建人ID',
  `updated_by`      BIGINT       NULL                    COMMENT '更新人ID',
  `version`         INT          NOT NULL DEFAULT 0      COMMENT '乐观锁版本',
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`, `is_read`, `created_at`),
  KEY `idx_category` (`user_id`, `category`),
  KEY `idx_biz` (`biz_type`, `biz_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户通知(站内信)';

-- ---------- 用户通知偏好 ----------
CREATE TABLE IF NOT EXISTS `notification_preference` (
  `id`                   BIGINT  NOT NULL                COMMENT '雪花ID(应用层生成)',
  `user_id`              BIGINT  NOT NULL                COMMENT '用户ID',
  `system_enabled`       TINYINT NOT NULL DEFAULT 1      COMMENT '系统通知开关',
  `order_enabled`        TINYINT NOT NULL DEFAULT 1      COMMENT '订单通知开关',
  `marketing_enabled`    TINYINT NOT NULL DEFAULT 0      COMMENT '营销通知开关',
  `service_enabled`      TINYINT NOT NULL DEFAULT 1      COMMENT '客服消息开关',
  `created_at`           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by`           BIGINT  NULL                    COMMENT '创建人ID',
  `updated_by`           BIGINT  NULL                    COMMENT '更新人ID',
  `version`              INT     NOT NULL DEFAULT 0      COMMENT '乐观锁版本',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户通知偏好';

-- ---------- 初始化消息模板 ----------
INSERT INTO `message_template` (`id`, `template_code`, `title`, `content`, `category`, `push_enabled`, `status`) VALUES
(1, 'ORDER_PAID', '订单支付成功', '您的订单 {orderNo} 已支付成功, 金额 {amount} 元, 感谢您的购买!', 2, 1, 1),
(2, 'ORDER_SHIPPED', '订单已发货', '您的订单 {orderNo} 已发货, 预计 {days} 天内送达, 请保持电话畅通。', 2, 1, 1),
(3, 'ORDER_COMPLETED', '订单已完成', '您的订单 {orderNo} 已完成, 欢迎再次光临!', 2, 0, 1),
(4, 'ORDER_CANCELLED', '订单已取消', '您的订单 {orderNo} 已取消, 如有疑问请联系客服。', 2, 1, 1),
(5, 'REFUND_PROCESSED', '退款已到账', '您的订单 {orderNo} 退款 {amount} 元已到账, 请注意查收。', 2, 1, 1),
(6, 'SYSTEM_MAINTENANCE', '系统维护通知', '尊敬的用户, 系统将于 {time} 进行维护, 届时部分功能可能无法使用。', 1, 1, 1),
(7, 'CS_MESSAGE', '客服消息', '您有一条新的客服消息, 请及时查看。', 4, 1, 1),
(8, 'MARKETING_PROMOTION', '限时优惠', '{content}', 3, 1, 1);