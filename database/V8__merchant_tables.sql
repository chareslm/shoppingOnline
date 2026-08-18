ALTER TABLE `user`
    ADD COLUMN must_change_password TINYINT(1) NOT NULL DEFAULT 0 AFTER password_hash;

CREATE TABLE merchant_application (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    merchant_type VARCHAR(32) NOT NULL,
    shop_name VARCHAR(128) NOT NULL,
    subject_name VARCHAR(128) NULL,
    unified_social_credit_code VARCHAR(32) NULL,
    responsible_person_name VARCHAR(64) NOT NULL,
    identity_document_type VARCHAR(32) NOT NULL,
    identity_document_number VARCHAR(64) NOT NULL,
    contact_phone VARCHAR(32) NOT NULL,
    contact_email VARCHAR(254) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'SUBMITTED',
    rejection_reason VARCHAR(500) NULL,
    account_user_id BIGINT UNSIGNED NULL,
    account_reused TINYINT(1) NULL,
    email_delivery_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    pending_contact_email VARCHAR(254) GENERATED ALWAYS AS (
        CASE WHEN status IN ('SUBMITTED', 'QUALIFICATION_APPROVED') THEN contact_email ELSE NULL END
    ) STORED,
    qualification_audited_by BIGINT UNSIGNED NULL,
    qualification_audited_at DATETIME(3) NULL,
    account_audited_by BIGINT UNSIGNED NULL,
    account_audited_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_merchant_application_status_created (status, created_at),
    KEY idx_merchant_application_email_status (contact_email, status),
    UNIQUE KEY uk_merchant_application_pending_email (pending_contact_email),
    CONSTRAINT fk_merchant_application_account FOREIGN KEY (account_user_id) REFERENCES `user` (id),
    CONSTRAINT fk_merchant_application_qualification_auditor FOREIGN KEY (qualification_audited_by) REFERENCES `user` (id),
    CONSTRAINT fk_merchant_application_account_auditor FOREIGN KEY (account_audited_by) REFERENCES `user` (id),
    CONSTRAINT chk_merchant_application_type CHECK (merchant_type IN ('ENTERPRISE', 'SOLE_PROPRIETOR', 'INDIVIDUAL')),
    CONSTRAINT chk_merchant_application_status CHECK (status IN ('SUBMITTED', 'QUALIFICATION_APPROVED', 'ACCOUNT_APPROVED', 'REJECTED')),
    CONSTRAINT chk_merchant_application_email_status CHECK (email_delivery_status IN ('PENDING', 'SENT', 'MAIL_FAILED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商家入驻申请';

CREATE TABLE shop (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    owner_user_id BIGINT UNSIGNED NOT NULL,
    application_id BIGINT UNSIGNED NOT NULL,
    name VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'OPEN',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_shop_owner_user (owner_user_id),
    UNIQUE KEY uk_shop_application (application_id),
    CONSTRAINT fk_shop_owner FOREIGN KEY (owner_user_id) REFERENCES `user` (id),
    CONSTRAINT fk_shop_application FOREIGN KEY (application_id) REFERENCES merchant_application (id),
    CONSTRAINT chk_shop_status CHECK (status IN ('OPEN', 'SUSPENDED', 'FROZEN', 'CLOSED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='店铺';

CREATE TABLE merchant_qualification_file (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    application_id BIGINT UNSIGNED NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    storage_key VARCHAR(512) NOT NULL,
    content_type VARCHAR(64) NOT NULL,
    file_size BIGINT UNSIGNED NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_merchant_qualification_storage_key (storage_key),
    KEY idx_merchant_qualification_application (application_id),
    CONSTRAINT fk_merchant_qualification_application FOREIGN KEY (application_id) REFERENCES merchant_application (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商家资质私有文件';

INSERT INTO permission (code, name, resource, action, description, status) VALUES
    ('merchant:qualification:audit', '审核商家资质与账号', 'merchant:qualification', 'audit', '查看商家申请敏感详情、审核、下载资质及重试开通邮件', 'ACTIVE')
ON DUPLICATE KEY UPDATE name = VALUES(name), description = VALUES(description), status = VALUES(status);

INSERT IGNORE INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM `role` r
JOIN permission p ON p.code = 'merchant:qualification:audit'
WHERE r.code IN ('ADMIN', 'SUPER_ADMIN');
