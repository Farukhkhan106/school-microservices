-- =========================================================================
-- SUCCESS ACADEMY ERP - PHASE 7 & 7.1: CONFIGURATION GOVERNANCE SCHEMA
-- Idempotent & Safe Schema Migration for authdb
-- =========================================================================

-- 1. Ensure Table configuration_versions has Phase 7 Governance Columns
-- (Using conditional procedures to ensure compatibility with MySQL 8.0+)

DELIMITER $$

DROP PROCEDURE IF EXISTS upgrade_phase7_configuration_governance$$
CREATE PROCEDURE upgrade_phase7_configuration_governance()
BEGIN
    -- 1.1 Column parent_version_id
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'configuration_versions' AND COLUMN_NAME = 'parent_version_id'
    ) THEN
        ALTER TABLE configuration_versions ADD COLUMN parent_version_id BIGINT NULL;
    END IF;

    -- 1.2 Column rollback_from_version
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'configuration_versions' AND COLUMN_NAME = 'rollback_from_version'
    ) THEN
        ALTER TABLE configuration_versions ADD COLUMN rollback_from_version INT NULL;
    END IF;

    -- 1.3 Column checksum
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'configuration_versions' AND COLUMN_NAME = 'checksum'
    ) THEN
        ALTER TABLE configuration_versions ADD COLUMN checksum VARCHAR(64) NULL;
    END IF;

    -- 1.4 Column opt_lock_version
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'configuration_versions' AND COLUMN_NAME = 'opt_lock_version'
    ) THEN
        ALTER TABLE configuration_versions ADD COLUMN opt_lock_version BIGINT NOT NULL DEFAULT 0;
    ELSE
        -- Ensure existing rows are backfilled and column is NOT NULL DEFAULT 0
        UPDATE configuration_versions SET opt_lock_version = 0 WHERE opt_lock_version IS NULL;
        ALTER TABLE configuration_versions MODIFY COLUMN opt_lock_version BIGINT NOT NULL DEFAULT 0;
    END IF;

    -- 1.5 Column updated_by
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'configuration_versions' AND COLUMN_NAME = 'updated_by'
    ) THEN
        ALTER TABLE configuration_versions ADD COLUMN updated_by VARCHAR(64) DEFAULT 'system';
    END IF;

    -- 1.6 Column updated_at
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'configuration_versions' AND COLUMN_NAME = 'updated_at'
    ) THEN
        ALTER TABLE configuration_versions ADD COLUMN updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;
    END IF;

    -- 1.7 Index on version_number
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.STATISTICS 
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'configuration_versions' AND INDEX_NAME = 'idx_cfg_ver_version'
    ) THEN
        CREATE INDEX idx_cfg_ver_version ON configuration_versions(version_number);
    END IF;

    -- 2.1 Column target_version_number on configuration_audit_logs
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'configuration_audit_logs' AND COLUMN_NAME = 'target_version_number'
    ) THEN
        ALTER TABLE configuration_audit_logs ADD COLUMN target_version_number INT NULL;
    END IF;

    -- 2.2 Column diff_summary on configuration_audit_logs
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'configuration_audit_logs' AND COLUMN_NAME = 'diff_summary'
    ) THEN
        ALTER TABLE configuration_audit_logs ADD COLUMN diff_summary VARCHAR(500) NULL;
    ELSE
        ALTER TABLE configuration_audit_logs MODIFY COLUMN diff_summary VARCHAR(500) NULL;
    END IF;

    -- 2.3 Index on target_version_number
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.STATISTICS 
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'configuration_audit_logs' AND INDEX_NAME = 'idx_cal_target_version'
    ) THEN
        CREATE INDEX idx_cal_target_version ON configuration_audit_logs(target_version_number);
    END IF;

    -- 2.4 Composite Index on tenant_key + timestamp
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.STATISTICS 
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'configuration_audit_logs' AND INDEX_NAME = 'idx_cal_tenant_timestamp'
    ) THEN
        CREATE INDEX idx_cal_tenant_timestamp ON configuration_audit_logs(tenant_key, timestamp);
    END IF;

END$$

DELIMITER ;

CALL upgrade_phase7_configuration_governance();
DROP PROCEDURE IF EXISTS upgrade_phase7_configuration_governance;
