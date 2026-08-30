-- Statistics exact-query permissions and supporting indexes.
-- No aggregate table is introduced: MySQL authority tables remain the source of truth.

INSERT INTO permission (code, name, resource, action, description, status) VALUES
    ('statistics:platform:view', '查看平台统计', 'statistics:platform', 'view', '查看平台精确统计概览与日趋势', 'ACTIVE'),
    ('statistics:shop:view', '查看本店统计', 'statistics:shop', 'view', '查看当前商家账号所属店铺的精确统计概览与日趋势', 'ACTIVE')
AS incoming
ON DUPLICATE KEY UPDATE
    name = incoming.name,
    description = incoming.description,
    status = incoming.status;

INSERT IGNORE INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM `role` r
JOIN permission p ON p.code = 'statistics:platform:view'
WHERE r.code = 'SUPER_ADMIN';

INSERT IGNORE INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM `role` r
JOIN permission p ON p.code = 'statistics:shop:view'
WHERE r.code = 'MERCHANT_OWNER';

ALTER TABLE `user`
    ADD KEY idx_user_created_at (created_at);

ALTER TABLE payment_order
    ADD KEY idx_payment_status_pay_time (status, pay_time);

ALTER TABLE refund_order
    ADD KEY idx_refund_status_refund_time (status, refund_time);

ALTER TABLE spu
    ADD KEY idx_spu_shop_status (shop_id, status);

ALTER TABLE review
    ADD KEY idx_review_status_created_at (status, created_at);
