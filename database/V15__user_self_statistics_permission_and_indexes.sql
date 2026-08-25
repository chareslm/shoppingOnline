-- User self-statistics permission and supporting indexes.
-- The endpoint always derives user_id from the authenticated subject.

INSERT INTO permission (code, name, resource, action, description, status) VALUES
    ('statistics:self:view', '查看本人统计', 'statistics:self', 'view', '查看当前认证用户本人的消费统计概览', 'ACTIVE')
AS incoming
ON DUPLICATE KEY UPDATE
    name = incoming.name,
    description = incoming.description,
    status = incoming.status;

INSERT IGNORE INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM `role` r
JOIN permission p ON p.code = 'statistics:self:view'
WHERE r.code = 'USER';

ALTER TABLE payment_order
    ADD KEY idx_payment_user_status_pay_time (user_id, status, pay_time);

ALTER TABLE refund_order
    ADD KEY idx_refund_user_status_refund_time (user_id, status, refund_time);

ALTER TABLE review
    ADD KEY idx_review_user_status_created_at (user_id, status, created_at);
