-- =========================================================================
-- SUCCESS ACADEMY ERP - PHASE 6: USER MANAGEMENT & LIFECYCLE SCHEMA
-- =========================================================================

-- 1. Extend users table with lifecycle and profile columns
ALTER TABLE users 
    ADD COLUMN IF NOT EXISTS phone VARCHAR(30) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS first_name VARCHAR(50) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS last_name VARCHAR(50) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS display_name VARCHAR(100) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS user_type VARCHAR(50) DEFAULT 'STAFF',
    ADD COLUMN IF NOT EXISTS must_change_password BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS last_login_at TIMESTAMP NULL DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS created_by VARCHAR(100) DEFAULT 'SYSTEM',
    ADD COLUMN IF NOT EXISTS updated_by VARCHAR(100) DEFAULT 'SYSTEM';

-- Backfill display_name and user_type where null
UPDATE users SET display_name = username WHERE display_name IS NULL;
UPDATE users SET user_type = 
    CASE 
        WHEN role = 'ADMIN' THEN 'TENANT_ADMIN'
        WHEN role = 'TEACHER' THEN 'TEACHER'
        WHEN role = 'STUDENT' THEN 'STUDENT'
        ELSE 'STAFF'
    END
WHERE user_type IS NULL OR user_type = 'STAFF';

-- 2. User Invitations Table
CREATE TABLE IF NOT EXISTS user_invitations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL DEFAULT 'default',
    user_id BIGINT NOT NULL,
    email VARCHAR(150) NOT NULL,
    role_id BIGINT NULL,
    token_hash VARCHAR(128) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP NULL DEFAULT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) DEFAULT 'SYSTEM',
    CONSTRAINT fk_inv_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    INDEX idx_inv_token (token_hash),
    INDEX idx_inv_user (user_id),
    INDEX idx_inv_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Password Reset Tokens Table
CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL DEFAULT 'default',
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(128) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP NULL DEFAULT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_prt_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    INDEX idx_prt_token (token_hash),
    INDEX idx_prt_user (user_id),
    INDEX idx_prt_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. User Audit Logs Table
CREATE TABLE IF NOT EXISTS user_audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL DEFAULT 'default',
    actor_user_id BIGINT NULL,
    actor_username VARCHAR(100) NOT NULL,
    actor_role VARCHAR(50) NOT NULL,
    target_user_id BIGINT NOT NULL,
    target_username VARCHAR(100) NOT NULL,
    action VARCHAR(50) NOT NULL,
    details TEXT NULL,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_ual_tenant (tenant_id),
    INDEX idx_ual_target (target_user_id),
    INDEX idx_ual_action (action)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. Seed Granular User Management Permissions
INSERT IGNORE INTO permissions (permission_key, resource_name, action_name, description, category, is_system, is_protected) VALUES
('user.view', 'USER', 'VIEW', 'View tenant users, profiles, and status', 'SECURITY', TRUE, FALSE),
('user.create', 'USER', 'CREATE', 'Create and register new tenant users', 'SECURITY', TRUE, FALSE),
('user.update', 'USER', 'UPDATE', 'Update user profile and contact details', 'SECURITY', TRUE, FALSE),
('user.activate', 'USER', 'ACTIVATE', 'Activate user account', 'SECURITY', TRUE, FALSE),
('user.deactivate', 'USER', 'DEACTIVATE', 'Deactivate user account', 'SECURITY', TRUE, FALSE),
('user.suspend', 'USER', 'SUSPEND', 'Suspend user account', 'SECURITY', TRUE, FALSE),
('user.role.assign', 'USER', 'ASSIGN_ROLE', 'Assign roles to tenant users', 'SECURITY', TRUE, FALSE),
('user.role.revoke', 'USER', 'REVOKE_ROLE', 'Revoke roles from tenant users', 'SECURITY', TRUE, FALSE),
('user.password.reset', 'USER', 'RESET_PASSWORD', 'Reset or force-reset user passwords', 'SECURITY', TRUE, FALSE),
('user.invite', 'USER', 'INVITE', 'Send and manage user invitations', 'SECURITY', TRUE, FALSE);

-- 6. Link permissions to TENANT_ADMIN and ADMIN_DELEGATE
-- TENANT_ADMIN receives all user management permissions
INSERT IGNORE INTO role_permissions (role_id, permission_id, default_scope)
SELECT r.id, p.id, 'TENANT'
FROM roles r
CROSS JOIN permissions p
WHERE r.role_code = 'TENANT_ADMIN'
AND p.permission_key IN (
    'user.view', 'user.create', 'user.update', 'user.activate',
    'user.deactivate', 'user.suspend', 'user.role.assign', 'user.role.revoke',
    'user.password.reset', 'user.invite'
);

-- ADMIN_DELEGATE receives operational user permissions
INSERT IGNORE INTO role_permissions (role_id, permission_id, default_scope)
SELECT r.id, p.id, 'TENANT'
FROM roles r
CROSS JOIN permissions p
WHERE r.role_code = 'ADMIN_DELEGATE'
AND p.permission_key IN (
    'user.view', 'user.create', 'user.update', 'user.activate',
    'user.deactivate', 'user.role.assign', 'user.password.reset', 'user.invite'
);

-- PLATFORM_ADMIN receives all user permissions with ALL scope
INSERT IGNORE INTO role_permissions (role_id, permission_id, default_scope)
SELECT r.id, p.id, 'ALL'
FROM roles r
CROSS JOIN permissions p
WHERE r.role_code = 'PLATFORM_ADMIN'
AND p.permission_key IN (
    'user.view', 'user.create', 'user.update', 'user.activate',
    'user.deactivate', 'user.suspend', 'user.role.assign', 'user.role.revoke',
    'user.password.reset', 'user.invite'
);
