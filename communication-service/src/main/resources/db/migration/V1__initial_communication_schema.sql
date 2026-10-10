-- ====================================================================
-- SUCCESS ACADEMY ERP - COMMUNICATION SERVICE BASELINE MIGRATION (V1)
-- ====================================================================

CREATE TABLE IF NOT EXISTS conversations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    type VARCHAR(30) NOT NULL DEFAULT 'DIRECT',
    title VARCHAR(150),
    target_class VARCHAR(20),
    target_section VARCHAR(10),
    target_role VARCHAR(30),
    created_by BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    INDEX idx_conv_class_sec (target_class, target_section),
    INDEX idx_conv_type (type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS conversation_participants (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    conversation_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    user_role VARCHAR(30) NOT NULL,
    username VARCHAR(100) NOT NULL,
    joined_at DATETIME NOT NULL,
    last_read_at DATETIME,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_participant UNIQUE (conversation_id, user_id),
    INDEX idx_part_user (user_id),
    INDEX idx_part_conv (conversation_id),
    CONSTRAINT fk_part_conv FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    conversation_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    sender_role VARCHAR(30) NOT NULL,
    sender_username VARCHAR(100) NOT NULL,
    content VARCHAR(4000) NOT NULL,
    message_type VARCHAR(30) NOT NULL DEFAULT 'TEXT',
    attachment_url VARCHAR(500),
    attachment_name VARCHAR(200),
    created_at DATETIME NOT NULL,
    edited_at DATETIME,
    deleted_at DATETIME,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    INDEX idx_msg_conv (conversation_id),
    INDEX idx_msg_conv_created (conversation_id, created_at),
    CONSTRAINT fk_msg_conv FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS communication_audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    actor_user_id BIGINT,
    actor_role VARCHAR(30),
    action VARCHAR(50) NOT NULL,
    details VARCHAR(1000),
    timestamp DATETIME NOT NULL,
    INDEX idx_audit_time (timestamp)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL DEFAULT 'default',
    user_id BIGINT NOT NULL,
    username VARCHAR(100),
    user_role VARCHAR(30),
    title VARCHAR(255) NOT NULL,
    message VARCHAR(2000) NOT NULL,
    type VARCHAR(50) NOT NULL DEFAULT 'GENERAL',
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    entity_type VARCHAR(50),
    entity_id VARCHAR(100),
    action_url VARCHAR(255),
    created_at DATETIME NOT NULL,
    read_at DATETIME,
    INDEX idx_notif_user_read (user_id, is_read, created_at),
    INDEX idx_notif_tenant_user (tenant_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
