-- 商品公开图片；SKU 编码改为同一 SPU 内唯一，避免不同商品自动生成相同编码时插入失败。

CREATE TABLE IF NOT EXISTS `product_media` (
  `id`            BIGINT        NOT NULL COMMENT '雪花ID',
  `shop_id`       BIGINT        NOT NULL COMMENT '所属店铺',
  `storage_key`   VARCHAR(512)  NOT NULL COMMENT '相对存储键，必须以 product/ 开头',
  `content_type`  VARCHAR(64)   NOT NULL,
  `original_name` VARCHAR(255)  NOT NULL,
  `file_size`     BIGINT        NOT NULL,
  `created_at`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `created_by`    BIGINT        NULL,
  `updated_by`    BIGINT        NULL,
  `version`       INT           NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_product_media_storage_key` (`storage_key`),
  KEY `idx_product_media_shop` (`shop_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品公开图片';

ALTER TABLE `sku` DROP INDEX `uk_sku_code`;
ALTER TABLE `sku` ADD UNIQUE KEY `uk_spu_sku_code` (`spu_id`, `sku_code`);
