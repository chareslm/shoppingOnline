-- System-admin SMTP settings and account-provisioning permissions.
-- V8 merchant tables and V3 user-view permissions must be applied first.

CREATE TABLE system_smtp_setting (
    id TINYINT UNSIGNED NOT NULL,
    host VARCHAR(255) NULL,
    port INT NOT NULL DEFAULT 587,
    username VARCHAR(254) NULL,
    password VARCHAR(512) NULL,
    from_address VARCHAR(254) NULL,
    smtp_auth TINYINT(1) NOT NULL DEFAULT 1,
    starttls_enabled TINYINT(1) NOT NULL DEFAULT 1,
    updated_by BIGINT UNSIGNED NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    CONSTRAINT chk_system_smtp_setting_singleton CHECK (id = 1),
    CONSTRAINT chk_system_smtp_setting_port CHECK (port BETWEEN 1 AND 65535),
    CONSTRAINT fk_system_smtp_setting_updated_by FOREIGN KEY (updated_by) REFERENCES `user` (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统管理员维护的运行时 SMTP 配置（单行）';

INSERT INTO system_smtp_setting (id, port, smtp_auth, starttls_enabled)
VALUES (1, 587, 1, 1);

INSERT INTO permission (code, name, resource, action, description, status) VALUES
    ('system:smtp:view', '查看 SMTP 配置', 'system:smtp', 'view', '查看运行时 SMTP 主机、端口、账号与发件人（不含密码）', 'ACTIVE'),
    ('system:smtp:update', '更新 SMTP 配置', 'system:smtp', 'update', '保存运行时 SMTP 配置；需二次输入当前管理员密码', 'ACTIVE'),
    ('system:user:create', '创建平台账号', 'system:user', 'create', '由系统管理员手动创建平台账号并发送首次临时密码', 'ACTIVE')
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    description = VALUES(description),
    status = VALUES(status);

INSERT IGNORE INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM `role` r
INNER JOIN permission p ON p.code IN ('system:smtp:view', 'system:smtp:update', 'system:user:create')
WHERE r.code = 'SUPER_ADMIN';
