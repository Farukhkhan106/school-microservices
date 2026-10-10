-- ====================================================================
-- SUCCESS ACADEMY ERP - ACADEMIC SERVICE MIGRATION (V2)
-- Academic Session Lifecycle, Multi-Tenancy, Student Enrollments,
-- Promotion History, and Year Transition
-- ====================================================================

-- 1. Upgrade academic_sessions table
ALTER TABLE academic_sessions
    ADD COLUMN tenant_id VARCHAR(50) NOT NULL DEFAULT 'default',
    ADD COLUMN status VARCHAR(30) NOT NULL DEFAULT 'UPCOMING',
    ADD COLUMN created_by BIGINT NULL,
    ADD COLUMN updated_by BIGINT NULL;

-- Ensure existing active session is marked with status = 'ACTIVE'
UPDATE academic_sessions SET status = 'ACTIVE' WHERE is_active = TRUE;
UPDATE academic_sessions SET status = 'CLOSED' WHERE is_active = FALSE AND end_date < CURDATE();

-- Multi-tenant unique index and status index for sessions
CREATE INDEX idx_sessions_tenant_status ON academic_sessions(tenant_id, status);
ALTER TABLE academic_sessions DROP INDEX session_code;
ALTER TABLE academic_sessions ADD CONSTRAINT uk_tenant_session_code UNIQUE (tenant_id, session_code);

-- 2. Create student_enrollments table
CREATE TABLE IF NOT EXISTS student_enrollments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id VARCHAR(50) NOT NULL DEFAULT 'default',
    session_id BIGINT NOT NULL,
    session_code VARCHAR(50) NOT NULL,
    student_id BIGINT NOT NULL,
    student_name VARCHAR(100) NULL,
    admission_no VARCHAR(50) NULL,
    student_class VARCHAR(20) NOT NULL,
    section VARCHAR(10) NOT NULL,
    roll_no VARCHAR(50) NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    enrollment_date DATE NULL,
    withdrawal_date DATE NULL,
    withdrawal_reason VARCHAR(255) NULL,
    remarks VARCHAR(500) NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NULL,
    CONSTRAINT fk_enrollment_session FOREIGN KEY (session_id) REFERENCES academic_sessions(id) ON DELETE CASCADE,
    CONSTRAINT uk_enrollment_tenant_session_student UNIQUE (tenant_id, session_id, student_id),
    INDEX idx_enrollment_session_class_sec (tenant_id, session_id, student_class, section),
    INDEX idx_enrollment_student (tenant_id, student_id),
    INDEX idx_enrollment_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Create academic_session_transitions table
CREATE TABLE IF NOT EXISTS academic_session_transitions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id VARCHAR(50) NOT NULL DEFAULT 'default',
    source_session_id BIGINT NOT NULL,
    source_session_code VARCHAR(50) NOT NULL,
    target_session_id BIGINT NOT NULL,
    target_session_code VARCHAR(50) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    total_students INT NOT NULL DEFAULT 0,
    promoted_count INT NOT NULL DEFAULT 0,
    retained_count INT NOT NULL DEFAULT 0,
    graduated_count INT NOT NULL DEFAULT 0,
    transferred_count INT NOT NULL DEFAULT 0,
    withdrawn_count INT NOT NULL DEFAULT 0,
    total_carry_forward_fees DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    fee_arrears_students_count INT NOT NULL DEFAULT 0,
    notes VARCHAR(1000) NULL,
    executed_by BIGINT NULL,
    executed_at DATETIME NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NULL,
    CONSTRAINT fk_transition_source_session FOREIGN KEY (source_session_id) REFERENCES academic_sessions(id),
    CONSTRAINT fk_transition_target_session FOREIGN KEY (target_session_id) REFERENCES academic_sessions(id),
    INDEX idx_transition_tenant_status (tenant_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Create student_promotions table
CREATE TABLE IF NOT EXISTS student_promotions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id VARCHAR(50) NOT NULL DEFAULT 'default',
    transition_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    student_name VARCHAR(100) NULL,
    admission_no VARCHAR(50) NULL,
    source_session_id BIGINT NOT NULL,
    target_session_id BIGINT NOT NULL,
    source_class VARCHAR(20) NOT NULL,
    source_section VARCHAR(10) NOT NULL,
    target_class VARCHAR(20) NULL,
    target_section VARCHAR(10) NULL,
    suggested_decision VARCHAR(30) NOT NULL DEFAULT 'PROMOTE',
    final_decision VARCHAR(30) NOT NULL DEFAULT 'PROMOTE',
    outstanding_fee DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    carry_forward_fee DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    reason VARCHAR(500) NULL,
    reviewed_by BIGINT NULL,
    reviewed_at DATETIME NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NULL,
    CONSTRAINT fk_promotion_transition FOREIGN KEY (transition_id) REFERENCES academic_session_transitions(id) ON DELETE CASCADE,
    CONSTRAINT uk_promotion_tenant_trans_student UNIQUE (tenant_id, transition_id, student_id),
    INDEX idx_promotion_transition_student (transition_id, student_id),
    INDEX idx_promotion_student (tenant_id, student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
