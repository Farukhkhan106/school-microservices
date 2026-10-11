-- =========================================================================
-- SUCCESS ACADEMY ERP - PHASE 4: GENERIC WORKFLOW & APPROVAL ENGINE SCHEMA
-- =========================================================================

-- 1. Workflow Definitions Table
CREATE TABLE IF NOT EXISTS workflow_definitions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id VARCHAR(64) DEFAULT NULL,
    workflow_key VARCHAR(100) NOT NULL,
    name VARCHAR(150) NOT NULL,
    description VARCHAR(500),
    resource_type VARCHAR(100) NOT NULL,
    feature_key VARCHAR(100),
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    version INT NOT NULL DEFAULT 1,
    is_system BOOLEAN NOT NULL DEFAULT FALSE,
    created_by VARCHAR(100) DEFAULT 'SYSTEM',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_wf_def (tenant_id, workflow_key, version),
    INDEX idx_wf_tenant (tenant_id),
    INDEX idx_wf_key_status (workflow_key, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Workflow States Table
CREATE TABLE IF NOT EXISTS workflow_states (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    workflow_definition_id BIGINT NOT NULL,
    state_key VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    state_type VARCHAR(30) NOT NULL DEFAULT 'INTERMEDIATE',
    display_order INT NOT NULL DEFAULT 0,
    is_initial BOOLEAN NOT NULL DEFAULT FALSE,
    is_terminal BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_wfs_def FOREIGN KEY (workflow_definition_id) REFERENCES workflow_definitions(id) ON DELETE CASCADE,
    UNIQUE KEY uk_wfs_def_state (workflow_definition_id, state_key),
    INDEX idx_wfs_def (workflow_definition_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Workflow Transitions Table
CREATE TABLE IF NOT EXISTS workflow_transitions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    workflow_definition_id BIGINT NOT NULL,
    from_state VARCHAR(50) NOT NULL,
    to_state VARCHAR(50) NOT NULL,
    action_key VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    permission_key VARCHAR(100) NOT NULL,
    required_scope VARCHAR(50) DEFAULT 'TENANT',
    approval_required BOOLEAN NOT NULL DEFAULT FALSE,
    approval_level INT NOT NULL DEFAULT 1,
    maker_checker_required BOOLEAN NOT NULL DEFAULT FALSE,
    requires_comment BOOLEAN NOT NULL DEFAULT FALSE,
    requires_rejection_reason BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_wft_def FOREIGN KEY (workflow_definition_id) REFERENCES workflow_definitions(id) ON DELETE CASCADE,
    UNIQUE KEY uk_wft_def_trans (workflow_definition_id, from_state, action_key),
    INDEX idx_wft_def (workflow_definition_id),
    INDEX idx_wft_action (action_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Workflow Instances Table
CREATE TABLE IF NOT EXISTS workflow_instances (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    workflow_definition_id BIGINT NOT NULL,
    workflow_version INT NOT NULL,
    resource_type VARCHAR(100) NOT NULL,
    resource_id VARCHAR(100) NOT NULL,
    current_state VARCHAR(50) NOT NULL,
    created_by_user_id BIGINT NOT NULL,
    created_by_username VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_wfi_def FOREIGN KEY (workflow_definition_id) REFERENCES workflow_definitions(id),
    UNIQUE KEY uk_wfi_tenant_res (tenant_id, resource_type, resource_id),
    INDEX idx_wfi_tenant_state (tenant_id, current_state),
    INDEX idx_wfi_created_by (tenant_id, created_by_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. Workflow Actions Table (Audit of State Changes + Idempotency)
CREATE TABLE IF NOT EXISTS workflow_actions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    workflow_instance_id BIGINT NOT NULL,
    action_key VARCHAR(50) NOT NULL,
    actor_user_id BIGINT NOT NULL,
    actor_username VARCHAR(100),
    from_state VARCHAR(50) NOT NULL,
    to_state VARCHAR(50) NOT NULL,
    comment TEXT,
    reason TEXT,
    idempotency_key VARCHAR(128),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_wfa_inst FOREIGN KEY (workflow_instance_id) REFERENCES workflow_instances(id) ON DELETE CASCADE,
    INDEX idx_wfa_inst (workflow_instance_id),
    INDEX idx_wfa_idem (idempotency_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. Approval Tasks Table
CREATE TABLE IF NOT EXISTS approval_tasks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    workflow_instance_id BIGINT NOT NULL,
    transition_id BIGINT,
    level INT NOT NULL DEFAULT 1,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    assigned_role VARCHAR(100),
    assigned_to BIGINT,
    assigned_scope VARCHAR(50) DEFAULT 'TENANT',
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP NULL,
    completed_by BIGINT,
    completed_by_username VARCHAR(100),
    comment TEXT,
    CONSTRAINT fk_apt_inst FOREIGN KEY (workflow_instance_id) REFERENCES workflow_instances(id) ON DELETE CASCADE,
    INDEX idx_apt_tenant_status (tenant_id, status),
    INDEX idx_apt_inst (workflow_instance_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7. Workflow Audits Table
CREATE TABLE IF NOT EXISTS workflow_audits (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    workflow_instance_id BIGINT NOT NULL,
    action VARCHAR(50) NOT NULL,
    from_state VARCHAR(50) NOT NULL,
    to_state VARCHAR(50) NOT NULL,
    actor_user_id BIGINT NOT NULL,
    actor_username VARCHAR(100),
    metadata TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_wfau_inst FOREIGN KEY (workflow_instance_id) REFERENCES workflow_instances(id) ON DELETE CASCADE,
    INDEX idx_wfau_tenant (tenant_id),
    INDEX idx_wfau_inst (workflow_instance_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
