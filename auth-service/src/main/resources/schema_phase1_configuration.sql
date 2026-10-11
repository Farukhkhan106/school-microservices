-- ==========================================================
-- PHASE 1: TENANT & CONFIGURATION FOUNDATION SCHEMA
-- Service: auth-service (authdb)
-- Backward Compatible, Safe, Non-Destructive
-- ==========================================================

-- 1. Ensure tenant_id column exists on users table
-- If tenant_id does not exist:
-- ALTER TABLE users ADD COLUMN tenant_id VARCHAR(64) DEFAULT 'default';
UPDATE users SET tenant_id = 'default' WHERE tenant_id IS NULL;

-- 2. Tenants Catalog Table
CREATE TABLE IF NOT EXISTS tenants (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_key VARCHAR(64) NOT NULL UNIQUE,
    display_name VARCHAR(255) NOT NULL,
    legal_name VARCHAR(255),
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    domain VARCHAR(128),
    contact_email VARCHAR(128),
    contact_phone VARCHAR(32),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_tenant_key (tenant_key),
    INDEX idx_tenant_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. Environments Table
CREATE TABLE IF NOT EXISTS environments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    environment_key VARCHAR(32) NOT NULL UNIQUE,
    display_name VARCHAR(64) NOT NULL,
    is_production BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_env_key (environment_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4. Configuration Versions Table
CREATE TABLE IF NOT EXISTS configuration_versions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_key VARCHAR(64) NOT NULL,
    environment_key VARCHAR(32),
    version_number INT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    change_summary VARCHAR(500),
    created_by VARCHAR(64) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_by VARCHAR(64),
    published_at DATETIME,
    archived_at DATETIME,
    INDEX idx_cfg_ver_tenant (tenant_key),
    INDEX idx_cfg_ver_env (environment_key),
    INDEX idx_cfg_ver_status (status),
    INDEX idx_cfg_ver_tenant_status (tenant_key, environment_key, status),
    UNIQUE KEY uk_tenant_env_version (tenant_key, environment_key, version_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 5. Tenant Configurations (Categories) Table
CREATE TABLE IF NOT EXISTS tenant_configurations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    version_id BIGINT NOT NULL,
    tenant_key VARCHAR(64) NOT NULL,
    environment_key VARCHAR(32),
    category VARCHAR(64) NOT NULL,
    config_data LONGTEXT NOT NULL,
    schema_version VARCHAR(16) NOT NULL DEFAULT 'v1',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_tc_version (version_id),
    INDEX idx_tc_tenant_cat (tenant_key, category),
    INDEX idx_tc_tenant_env_cat (tenant_key, environment_key, category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 6. Feature Entitlements Table
CREATE TABLE IF NOT EXISTS feature_entitlements (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_key VARCHAR(64) NOT NULL,
    environment_key VARCHAR(32),
    feature_key VARCHAR(64) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    description VARCHAR(255),
    updated_by VARCHAR(64),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_fe_tenant (tenant_key),
    INDEX idx_fe_tenant_env (tenant_key, environment_key),
    INDEX idx_fe_feature (feature_key),
    UNIQUE KEY uk_tenant_env_feature (tenant_key, environment_key, feature_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 7. Configuration Audit Logs Table
CREATE TABLE IF NOT EXISTS configuration_audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_key VARCHAR(64) NOT NULL,
    environment_key VARCHAR(32),
    actor_username VARCHAR(64) NOT NULL,
    actor_role VARCHAR(64),
    action VARCHAR(64) NOT NULL,
    details VARCHAR(1000),
    timestamp DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_cal_tenant (tenant_key),
    INDEX idx_cal_timestamp (timestamp)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
