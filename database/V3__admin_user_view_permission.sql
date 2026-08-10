-- Admin user directory permission. V2 authorization permissions must be applied first.

INSERT INTO permission (code, name, resource, action, description, status) VALUES
    ('system:user:view', '查看用户', 'system:user', 'view', '查询平台用户及其角色', 'ACTIVE')
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    description = VALUES(description),
    status = VALUES(status);

INSERT IGNORE INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM `role` r
INNER JOIN permission p ON p.code = 'system:user:view'
WHERE r.code = 'SUPER_ADMIN';
