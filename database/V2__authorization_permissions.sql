-- Authorization-management permissions. V1__identity_and_user.sql must be applied first.

INSERT INTO permission (code, name, resource, action, description, status) VALUES
    ('system:role:view', '查看角色', 'system:role', 'view', '查看角色及数据范围', 'ACTIVE'),
    ('system:permission:view', '查看权限', 'system:permission', 'view', '查看权限编码与状态', 'ACTIVE'),
    ('system:user:role:assign', '分配用户角色', 'system:user:role', 'assign', '为用户分配或撤销角色；需二次验证', 'ACTIVE')
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    description = VALUES(description),
    status = VALUES(status);

INSERT IGNORE INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM `role` r
INNER JOIN permission p ON p.code IN ('system:role:view', 'system:permission:view', 'system:user:role:assign')
WHERE r.code = 'SUPER_ADMIN';
