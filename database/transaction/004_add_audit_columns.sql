-- ============================================================
-- 交易模块 - 审计列增量脚本（方案 A）
-- 文件: database/transaction/004_add_audit_columns.sql
-- 说明: 8 张业务表增加 created_by / updated_by 列，与团队 BaseEntity 对齐
-- 执行: mysql -uroot -p shopping < 004_add_audit_columns.sql
-- 注意: 非幂等，重复执行会报 Duplicate column（可接受）
-- 例外: order_operation_log / payment_record（仅 created_at）、
--       reconciliation_record（无 version）不在此列，实体不继承 BaseEntity
-- ============================================================

ALTER TABLE `cart`
  ADD COLUMN `created_by` BIGINT NULL COMMENT '创建人ID' AFTER `updated_at`,
  ADD COLUMN `updated_by` BIGINT NULL COMMENT '更新人ID' AFTER `created_by`;

ALTER TABLE `cart_group`
  ADD COLUMN `created_by` BIGINT NULL COMMENT '创建人ID' AFTER `updated_at`,
  ADD COLUMN `updated_by` BIGINT NULL COMMENT '更新人ID' AFTER `created_by`;

ALTER TABLE `cart_item`
  ADD COLUMN `created_by` BIGINT NULL COMMENT '创建人ID' AFTER `updated_at`,
  ADD COLUMN `updated_by` BIGINT NULL COMMENT '更新人ID' AFTER `created_by`;

ALTER TABLE `order`
  ADD COLUMN `created_by` BIGINT NULL COMMENT '创建人ID' AFTER `updated_at`,
  ADD COLUMN `updated_by` BIGINT NULL COMMENT '更新人ID' AFTER `created_by`;

ALTER TABLE `order_item`
  ADD COLUMN `created_by` BIGINT NULL COMMENT '创建人ID' AFTER `updated_at`,
  ADD COLUMN `updated_by` BIGINT NULL COMMENT '更新人ID' AFTER `created_by`;

ALTER TABLE `stock_reservation`
  ADD COLUMN `created_by` BIGINT NULL COMMENT '创建人ID' AFTER `updated_at`,
  ADD COLUMN `updated_by` BIGINT NULL COMMENT '更新人ID' AFTER `created_by`;

ALTER TABLE `payment_order`
  ADD COLUMN `created_by` BIGINT NULL COMMENT '创建人ID' AFTER `updated_at`,
  ADD COLUMN `updated_by` BIGINT NULL COMMENT '更新人ID' AFTER `created_by`;

ALTER TABLE `refund_order`
  ADD COLUMN `created_by` BIGINT NULL COMMENT '创建人ID' AFTER `updated_at`,
  ADD COLUMN `updated_by` BIGINT NULL COMMENT '更新人ID' AFTER `created_by`;