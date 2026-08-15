-- ============================================================
-- 搜索模块建表迁移（搜索日志，用于热词统计）
-- 文件: database/V7__search_tables.sql
-- 说明: 成员3 搜索模块。商品全文检索索引在 Elasticsearch（mall-product-v1），
--       搜索日志落在 MySQL，用于统计热词与搜索建议。
-- ============================================================

CREATE TABLE IF NOT EXISTS `search_log` (
  `id`         BIGINT       NOT NULL                COMMENT '雪花ID(应用层生成)',
  `keyword`    VARCHAR(128) NOT NULL                COMMENT '搜索关键词(小写归一)',
  `user_id`    BIGINT       NULL                    COMMENT '搜索用户ID, 未登录为NULL',
  `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '搜索时间',
  PRIMARY KEY (`id`),
  KEY `idx_keyword_created` (`keyword`, `created_at`),
  KEY `idx_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='搜索日志(热词统计)';
