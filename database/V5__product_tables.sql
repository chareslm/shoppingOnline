-- ============================================================
-- 商品模块建表迁移（类目 / SPU / SKU / 状态流转日志）+ 商品权限
-- 文件: database/V5__product_tables.sql
-- 说明: 成员3 商品模块。SPU/SKU 使用雪花 ID（应用层生成），与交易模块
--       引用的 sku_id 保持一致（BIGINT 有符号）。
-- 依赖: 引用成员2 shop 表的 shop_id（不建外键）；权限引用 V1 的 role/permission。
-- ============================================================

-- ---------- 类目 ----------
CREATE TABLE IF NOT EXISTS `category` (
  `id`         BIGINT       NOT NULL                COMMENT '雪花ID(应用层生成)',
  `parent_id`  BIGINT       NOT NULL DEFAULT 0      COMMENT '父类目ID, 0为根',
  `name`       VARCHAR(64)  NOT NULL                COMMENT '类目名称',
  `level`      INT          NOT NULL DEFAULT 1      COMMENT '层级: 1/2/3',
  `sort_order` INT          NOT NULL DEFAULT 0      COMMENT '排序值, 越小越靠前',
  `icon`       VARCHAR(512) NULL                    COMMENT '类目图标URL',
  `status`     TINYINT      NOT NULL DEFAULT 1      COMMENT '1启用/0停用',
  `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` BIGINT       NULL                    COMMENT '创建人ID',
  `updated_by` BIGINT       NULL                    COMMENT '更新人ID',
  `version`    INT          NOT NULL DEFAULT 0      COMMENT '乐观锁版本',
  PRIMARY KEY (`id`),
  KEY `idx_parent` (`parent_id`),
  KEY `idx_level_sort` (`level`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品类目';

-- ---------- SPU（商品，标准化产品单元） ----------
CREATE TABLE IF NOT EXISTS `spu` (
  `id`            BIGINT         NOT NULL                COMMENT '雪花ID(应用层生成)',
  `shop_id`       BIGINT         NOT NULL                COMMENT '店铺ID(引用成员2 shop表)',
  `category_id`   BIGINT         NOT NULL                COMMENT '类目ID',
  `brand`         VARCHAR(64)    NULL                    COMMENT '品牌',
  `name`          VARCHAR(255)   NOT NULL                COMMENT '商品名称',
  `subtitle`      VARCHAR(255)   NULL                    COMMENT '副标题/卖点',
  `main_image`    VARCHAR(512)   NULL                    COMMENT '主图URL',
  `images`        JSON           NULL                    COMMENT '轮播图URL数组',
  `detail`        TEXT           NULL                    COMMENT '图文详情',
  `price_min`     DECIMAL(10,2)  NULL                    COMMENT '最低SKU价格(冗余)',
  `price_max`     DECIMAL(10,2)  NULL                    COMMENT '最高SKU价格(冗余)',
  `sales`         INT            NOT NULL DEFAULT 0      COMMENT '累计销量',
  `rating`        DECIMAL(3,2)   NOT NULL DEFAULT 0      COMMENT '平均评分(评价模块回写)',
  `status`        VARCHAR(32)    NOT NULL DEFAULT 'DRAFT' COMMENT '状态机: DRAFT/PENDING_AUDIT/AUDIT_APPROVED/AUDIT_REJECTED/ON_SALE/OFF_SALE',
  `audit_remark`  VARCHAR(255)   NULL                    COMMENT '审核备注/驳回原因',
  `created_at`    DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`    DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by`    BIGINT         NULL                    COMMENT '创建人ID',
  `updated_by`    BIGINT         NULL                    COMMENT '更新人ID',
  `version`       INT            NOT NULL DEFAULT 0      COMMENT '乐观锁版本',
  PRIMARY KEY (`id`),
  KEY `idx_shop` (`shop_id`),
  KEY `idx_category` (`category_id`),
  KEY `idx_status` (`status`),
  KEY `idx_name` (`name`(64)),
  CONSTRAINT `chk_spu_status` CHECK (`status` IN ('DRAFT','PENDING_AUDIT','AUDIT_APPROVED','AUDIT_REJECTED','ON_SALE','OFF_SALE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品SPU';

-- ---------- SKU（库存保有单位） ----------
-- 库存权威在 MySQL：available_stock / reserved_stock / sold_stock。
-- 下单预占 reserved_stock+?，支付成功扣减 available_stock-?/sold_stock+?，超时释放 reserved_stock-?。
CREATE TABLE IF NOT EXISTS `sku` (
  `id`              BIGINT        NOT NULL                COMMENT '雪花ID(应用层生成)',
  `spu_id`          BIGINT        NOT NULL                COMMENT '所属SPU',
  `sku_code`        VARCHAR(64)   NULL                    COMMENT '商家自定义SKU编码',
  `attributes`      JSON          NULL                    COMMENT '规格属性, 如 {"颜色":"黑色","内存":"256GB"}',
  `image`           VARCHAR(512)  NULL                    COMMENT 'SKU图URL',
  `price`           DECIMAL(10,2) NOT NULL                COMMENT '销售价',
  `available_stock` INT           NOT NULL DEFAULT 0      COMMENT '可售库存',
  `reserved_stock`  INT           NOT NULL DEFAULT 0      COMMENT '预占库存',
  `sold_stock`      INT           NOT NULL DEFAULT 0      COMMENT '已售库存',
  `status`          TINYINT       NOT NULL DEFAULT 1      COMMENT '1启用/0停用',
  `created_at`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by`      BIGINT        NULL                    COMMENT '创建人ID',
  `updated_by`      BIGINT        NULL                    COMMENT '更新人ID',
  `version`         INT           NOT NULL DEFAULT 0      COMMENT '乐观锁版本',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sku_code` (`sku_code`),
  KEY `idx_spu` (`spu_id`),
  KEY `idx_status` (`status`),
  CONSTRAINT `chk_sku_price` CHECK (`price` >= 0),
  CONSTRAINT `chk_sku_stock` CHECK (`available_stock` >= 0 AND `reserved_stock` >= 0 AND `sold_stock` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品SKU';

-- ---------- 商品状态流转日志（审计） ----------
CREATE TABLE IF NOT EXISTS `product_status_log` (
  `id`          BIGINT       NOT NULL                COMMENT '雪花ID(应用层生成)',
  `spu_id`      BIGINT       NOT NULL                COMMENT '关联SPU',
  `operator_id` BIGINT       NULL                    COMMENT '操作者ID',
  `action`      VARCHAR(32)  NOT NULL                COMMENT 'SUBMIT/APPROVE/REJECT/PUBLISH/OFF_SHELF',
  `from_status` VARCHAR(32)  NULL                    COMMENT '变更前状态',
  `to_status`   VARCHAR(32)  NULL                    COMMENT '变更后状态',
  `remark`      VARCHAR(255) NULL                    COMMENT '备注',
  `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`id`),
  KEY `idx_spu` (`spu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品状态流转日志';

-- ---------- 商品与评价权限 ----------
INSERT INTO permission (code, name, resource, action, description, status) VALUES
    ('category:manage', '类目管理', 'category', 'manage', '类目新增、编辑、启停', 'ACTIVE'),
    ('product:create', '创建商品', 'product', 'create', '商家创建 SPU/SKU', 'ACTIVE'),
    ('product:update', '编辑商品', 'product', 'update', '商家编辑 SPU/SKU', 'ACTIVE'),
    ('product:stock:adjust', '调整库存', 'product:stock', 'adjust', '商家调整 SKU 库存', 'ACTIVE'),
    ('product:audit', '审核商品', 'product', 'audit', '管理员审核商品上下架', 'ACTIVE')
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    description = VALUES(description),
    status = VALUES(status);

-- 商家主账号/员工：商品创建、编辑、库存调整
INSERT IGNORE INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM `role` r
INNER JOIN permission p ON p.code IN ('product:create', 'product:update', 'product:stock:adjust')
WHERE r.code IN ('MERCHANT_OWNER', 'MERCHANT_STAFF');

-- 平台管理员/超级管理员：类目管理、商品审核（含商家全部商品能力）
INSERT IGNORE INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM `role` r
INNER JOIN permission p ON p.code IN ('category:manage', 'product:audit', 'product:create', 'product:update', 'product:stock:adjust')
WHERE r.code IN ('ADMIN', 'SUPER_ADMIN');
