-- =========================================================================
-- SUCCESS ACADEMY ERP - PHASE 7: CONFIGURATION GOVERNANCE SCHEMA
-- =========================================================================

-- 1. Extend configuration_versions table
ALTER TABLE configuration_versions 
    ADD COLUMN IF NOT EXISTS parent_version_id BIGINT NULL,
    ADD COLUMN IF NOT EXISTS rollback_from_version INT NULL,
    ADD COLUMN IF NOT EXISTS checksum VARCHAR(64) NULL,
    ADD COLUMN IF NOT EXISTS opt_lock_version BIGINT DEFAULT 0,
    ADD COLUMN IF NOT EXISTS updated_by VARCHAR(64) DEFAULT 'system',
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

-- 2. Extend configuration_audit_logs table
ALTER TABLE configuration_audit_logs
    ADD COLUMN IF NOT EXISTS target_version_number INT NULL,
    ADD COLUMN IF NOT EXISTS diff_summary TEXT NULL;
