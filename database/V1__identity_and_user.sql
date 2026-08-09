-- Identity, RBAC, and user-domain baseline.
-- Apply this versioned migration to the `shopping` MySQL database before
-- starting the authentication API implementation.

CREATE TABLE `user` (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    username VARCHAR(64) NULL,
    email VARCHAR(254) NULL,
    phone VARCHAR(32) NULL,
    password_hash VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    failed_login_count INT UNSIGNED NOT NULL DEFAULT 0,
    locked_until DATETIME(3) NULL,
    last_login_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_username (username),
    UNIQUE KEY uk_user_email (email),
    UNIQUE KEY uk_user_phone (phone),
    CONSTRAINT chk_user_login_identifier CHECK (username IS NOT NULL OR email IS NOT NULL OR phone IS NOT NULL),
    CONSTRAINT chk_user_status CHECK (status IN ('ACTIVE', 'DISABLED', 'LOCKED', 'PENDING_VERIFICATION'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='统一账号';

CREATE TABLE `role` (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(64) NOT NULL,
    description VARCHAR(255) NULL,
    data_scope VARCHAR(32) NOT NULL DEFAULT 'SELF',
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    built_in TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_code (code),
    CONSTRAINT chk_role_data_scope CHECK (data_scope IN ('SELF', 'SHOP', 'ALL')),
    CONSTRAINT chk_role_status CHECK (status IN ('ACTIVE', 'DISABLED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色';

CREATE TABLE `permission` (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    code VARCHAR(128) NOT NULL,
    name VARCHAR(128) NOT NULL,
    resource VARCHAR(64) NOT NULL,
    action VARCHAR(64) NOT NULL,
    description VARCHAR(255) NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_permission_code (code),
    UNIQUE KEY uk_permission_resource_action (resource, action),
    CONSTRAINT chk_permission_status CHECK (status IN ('ACTIVE', 'DISABLED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='权限，编码为 resource:action';

CREATE TABLE user_role (
    user_id BIGINT UNSIGNED NOT NULL,
    role_id BIGINT UNSIGNED NOT NULL,
    granted_by BIGINT UNSIGNED NULL,
    granted_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (user_id, role_id),
    KEY idx_user_role_role_id (role_id),
    CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES `user` (id),
    CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) REFERENCES `role` (id),
    CONSTRAINT fk_user_role_granted_by FOREIGN KEY (granted_by) REFERENCES `user` (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户角色关联';

CREATE TABLE role_permission (
    role_id BIGINT UNSIGNED NOT NULL,
    permission_id BIGINT UNSIGNED NOT NULL,
    granted_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (role_id, permission_id),
    KEY idx_role_permission_permission_id (permission_id),
    CONSTRAINT fk_role_permission_role FOREIGN KEY (role_id) REFERENCES `role` (id),
    CONSTRAINT fk_role_permission_permission FOREIGN KEY (permission_id) REFERENCES `permission` (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色权限关联';

CREATE TABLE user_device (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    device_id VARCHAR(128) NOT NULL,
    device_type VARCHAR(32) NOT NULL,
    device_name VARCHAR(128) NULL,
    app_version VARCHAR(64) NULL,
    last_ip VARCHAR(45) NULL,
    last_active_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_device_user_device (user_id, device_id),
    KEY idx_user_device_last_active_at (user_id, last_active_at),
    CONSTRAINT fk_user_device_user FOREIGN KEY (user_id) REFERENCES `user` (id),
    CONSTRAINT chk_user_device_type CHECK (device_type IN ('WEB', 'ANDROID', 'MINIAPP', 'ADMIN_WEB')),
    CONSTRAINT chk_user_device_status CHECK (status IN ('ACTIVE', 'REVOKED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='登录设备';

CREATE TABLE refresh_token (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    device_id BIGINT UNSIGNED NULL,
    token_id CHAR(36) NOT NULL,
    token_hash CHAR(64) NOT NULL,
    issued_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    expires_at DATETIME(3) NOT NULL,
    revoked_at DATETIME(3) NULL,
    revoke_reason VARCHAR(64) NULL,
    replaced_by_token_id CHAR(36) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_refresh_token_token_id (token_id),
    UNIQUE KEY uk_refresh_token_hash (token_hash),
    KEY idx_refresh_token_user_status (user_id, revoked_at, expires_at),
    KEY idx_refresh_token_device_id (device_id),
    CONSTRAINT fk_refresh_token_user FOREIGN KEY (user_id) REFERENCES `user` (id),
    CONSTRAINT fk_refresh_token_device FOREIGN KEY (device_id) REFERENCES user_device (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='仅保存哈希值的刷新令牌';

CREATE TABLE audit_log (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    actor_user_id BIGINT UNSIGNED NULL,
    module VARCHAR(64) NOT NULL,
    action_code VARCHAR(128) NOT NULL,
    target_type VARCHAR(64) NULL,
    target_id VARCHAR(64) NULL,
    success TINYINT(1) NOT NULL,
    trace_id VARCHAR(64) NULL,
    request_method VARCHAR(16) NULL,
    request_path VARCHAR(255) NULL,
    client_ip VARCHAR(45) NULL,
    user_agent VARCHAR(512) NULL,
    detail JSON NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_audit_log_actor_created_at (actor_user_id, created_at),
    KEY idx_audit_log_module_action_created_at (module, action_code, created_at),
    KEY idx_audit_log_trace_id (trace_id),
    CONSTRAINT fk_audit_log_actor FOREIGN KEY (actor_user_id) REFERENCES `user` (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='只追加的审计日志，不记录密码或令牌明文';

CREATE TABLE user_profile (
    user_id BIGINT UNSIGNED NOT NULL,
    nickname VARCHAR(64) NULL,
    avatar_url VARCHAR(512) NULL,
    real_name VARCHAR(64) NULL,
    gender VARCHAR(16) NOT NULL DEFAULT 'UNKNOWN',
    birthday DATE NULL,
    bio VARCHAR(500) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (user_id),
    CONSTRAINT fk_user_profile_user FOREIGN KEY (user_id) REFERENCES `user` (id),
    CONSTRAINT chk_user_profile_gender CHECK (gender IN ('UNKNOWN', 'MALE', 'FEMALE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户资料';

CREATE TABLE user_address (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    recipient_name VARCHAR(64) NOT NULL,
    recipient_phone VARCHAR(32) NOT NULL,
    province_code VARCHAR(32) NULL,
    province_name VARCHAR(64) NOT NULL,
    city_code VARCHAR(32) NULL,
    city_name VARCHAR(64) NOT NULL,
    district_code VARCHAR(32) NULL,
    district_name VARCHAR(64) NOT NULL,
    detail_address VARCHAR(255) NOT NULL,
    postal_code VARCHAR(16) NULL,
    is_default TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_user_address_user_id (user_id),
    CONSTRAINT fk_user_address_user FOREIGN KEY (user_id) REFERENCES `user` (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='收货地址';

CREATE TABLE user_preference (
    user_id BIGINT UNSIGNED NOT NULL,
    marketing_enabled TINYINT(1) NOT NULL DEFAULT 1,
    order_notification_enabled TINYINT(1) NOT NULL DEFAULT 1,
    system_notification_enabled TINYINT(1) NOT NULL DEFAULT 1,
    extra_preferences JSON NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (user_id),
    CONSTRAINT fk_user_preference_user FOREIGN KEY (user_id) REFERENCES `user` (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户通知及展示偏好';

CREATE TABLE shop_follow (
    user_id BIGINT UNSIGNED NOT NULL,
    shop_id BIGINT UNSIGNED NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (user_id, shop_id),
    KEY idx_shop_follow_shop_id (shop_id),
    CONSTRAINT fk_shop_follow_user FOREIGN KEY (user_id) REFERENCES `user` (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='店铺关注；shop_id 由 merchant 模块维护';

INSERT INTO `role` (code, name, description, data_scope, status, built_in) VALUES
    ('USER', '普通用户', '商城消费者基础角色', 'SELF', 'ACTIVE', 1),
    ('MERCHANT_OWNER', '商家主账号', '店铺经营者', 'SHOP', 'ACTIVE', 1),
    ('MERCHANT_STAFF', '商家员工', '店铺员工账号', 'SHOP', 'ACTIVE', 1),
    ('CUSTOMER_SERVICE', '客服', '客服工作台账号', 'SHOP', 'ACTIVE', 1),
    ('ADMIN', '平台管理员', '平台运营和审核人员', 'ALL', 'ACTIVE', 1),
    ('SUPER_ADMIN', '超级管理员', '平台最高权限账号', 'ALL', 'ACTIVE', 1);
