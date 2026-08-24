-- Audit log directory permission. Applied after V10 because V8 is already merchant tables.
-- V2 authorization permissions must be applied first.

INSERT INTO permission (code, name, resource, action, description, status) VALUES
    ('system:audit:view', '查看审计日志', 'system:audit', 'view', '查询平台安全审计日志；敏感字段按规则脱敏', 'ACTIVE')
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    description = VALUES(description),
    status = VALUES(status);

INSERT IGNORE INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM `role` r
INNER JOIN permission p ON p.code = 'system:audit:view'
WHERE r.code = 'SUPER_ADMIN';
