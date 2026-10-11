-- ====================================================================
-- SUCCESS ACADEMY ERP - ACADEMIC SERVICE MIGRATION (V3)
-- Tenant-Aware Academic Configuration: Classes, Sections, Subjects,
-- and Session-Specific Academic Offerings
-- ====================================================================

-- 1. Academic Classes / Grades Master Configuration
CREATE TABLE IF NOT EXISTS academic_classes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id VARCHAR(50) NOT NULL DEFAULT 'default',
    class_code VARCHAR(30) NOT NULL,
    display_name VARCHAR(60) NOT NULL,
    display_order INT NOT NULL DEFAULT 0,
    category VARCHAR(30) NOT NULL DEFAULT 'PRIMARY', -- PRE_PRIMARY, PRIMARY, MIDDLE, SECONDARY, SENIOR_SECONDARY
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, INACTIVE, ARCHIVED
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_tenant_class_code UNIQUE (tenant_id, class_code),
    INDEX idx_classes_tenant_status (tenant_id, status),
    INDEX idx_classes_tenant_order (tenant_id, display_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Academic Sections Master Configuration
CREATE TABLE IF NOT EXISTS academic_sections (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id VARCHAR(50) NOT NULL DEFAULT 'default',
    section_code VARCHAR(20) NOT NULL,
    display_name VARCHAR(60) NOT NULL,
    display_order INT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, INACTIVE, ARCHIVED
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_tenant_section_code UNIQUE (tenant_id, section_code),
    INDEX idx_sections_tenant_status (tenant_id, status),
    INDEX idx_sections_tenant_order (tenant_id, display_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Academic Subjects Master Configuration
CREATE TABLE IF NOT EXISTS academic_subjects (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id VARCHAR(50) NOT NULL DEFAULT 'default',
    subject_code VARCHAR(50) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    subject_type VARCHAR(30) NOT NULL DEFAULT 'CORE', -- CORE, ELECTIVE, VOCATIONAL, CO_CURRICULAR
    display_order INT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, INACTIVE, ARCHIVED
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_tenant_subject_code UNIQUE (tenant_id, subject_code),
    INDEX idx_subjects_tenant_status (tenant_id, status),
    INDEX idx_subjects_tenant_order (tenant_id, display_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Session-Specific Class-Section Offerings
CREATE TABLE IF NOT EXISTS academic_class_sections (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id VARCHAR(50) NOT NULL DEFAULT 'default',
    session_id BIGINT NOT NULL,
    class_code VARCHAR(30) NOT NULL,
    section_code VARCHAR(20) NOT NULL,
    capacity INT NOT NULL DEFAULT 40,
    room_number VARCHAR(50) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, INACTIVE
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_acs_session FOREIGN KEY (session_id) REFERENCES academic_sessions(id) ON DELETE CASCADE,
    CONSTRAINT uk_acs_tenant_session_class_sec UNIQUE (tenant_id, session_id, class_code, section_code),
    INDEX idx_acs_tenant_session (tenant_id, session_id),
    INDEX idx_acs_class (tenant_id, class_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. Session-Specific Class-Subject Offerings
CREATE TABLE IF NOT EXISTS academic_class_subjects (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id VARCHAR(50) NOT NULL DEFAULT 'default',
    session_id BIGINT NOT NULL,
    class_code VARCHAR(30) NOT NULL,
    subject_code VARCHAR(50) NOT NULL,
    is_mandatory BOOLEAN NOT NULL DEFAULT TRUE,
    weekly_periods INT NOT NULL DEFAULT 5,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, INACTIVE
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_acsubj_session FOREIGN KEY (session_id) REFERENCES academic_sessions(id) ON DELETE CASCADE,
    CONSTRAINT uk_acsubj_tenant_session_class_subj UNIQUE (tenant_id, session_id, class_code, subject_code),
    INDEX idx_acsubj_tenant_session (tenant_id, session_id),
    INDEX idx_acsubj_class (tenant_id, class_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ====================================================================
-- SEED DATA FOR 'default' TENANT (Success Modern Academy: Nursery - 12)
-- ====================================================================

-- Classes for 'default'
INSERT IGNORE INTO academic_classes (tenant_id, class_code, display_name, display_order, category, status) VALUES
('default', 'NURSERY', 'Nursery', 1, 'PRE_PRIMARY', 'ACTIVE'),
('default', 'LKG', 'LKG', 2, 'PRE_PRIMARY', 'ACTIVE'),
('default', 'UKG', 'UKG', 3, 'PRE_PRIMARY', 'ACTIVE'),
('default', '1', 'Class 1', 4, 'PRIMARY', 'ACTIVE'),
('default', '2', 'Class 2', 5, 'PRIMARY', 'ACTIVE'),
('default', '3', 'Class 3', 6, 'PRIMARY', 'ACTIVE'),
('default', '4', 'Class 4', 7, 'PRIMARY', 'ACTIVE'),
('default', '5', 'Class 5', 8, 'PRIMARY', 'ACTIVE'),
('default', '6', 'Class 6', 9, 'MIDDLE', 'ACTIVE'),
('default', '7', 'Class 7', 10, 'MIDDLE', 'ACTIVE'),
('default', '8', 'Class 8', 11, 'MIDDLE', 'ACTIVE'),
('default', '9', 'Class 9', 12, 'SECONDARY', 'ACTIVE'),
('default', '10', 'Class 10', 13, 'SECONDARY', 'ACTIVE'),
('default', '11', 'Class 11', 14, 'SENIOR_SECONDARY', 'ACTIVE'),
('default', '12', 'Class 12', 15, 'SENIOR_SECONDARY', 'ACTIVE');

-- Sections for 'default'
INSERT IGNORE INTO academic_sections (tenant_id, section_code, display_name, display_order, status) VALUES
('default', 'A', 'Section A', 1, 'ACTIVE'),
('default', 'B', 'Section B', 2, 'ACTIVE'),
('default', 'C', 'Section C', 3, 'ACTIVE');

-- Subjects for 'default'
INSERT IGNORE INTO academic_subjects (tenant_id, subject_code, display_name, subject_type, display_order, status) VALUES
('default', 'ACCOUNTANCY', 'Accountancy', 'CORE', 1, 'ACTIVE'),
('default', 'ART_CRAFT', 'Art & Craft', 'CO_CURRICULAR', 2, 'ACTIVE'),
('default', 'BIOLOGY', 'Biology', 'CORE', 3, 'ACTIVE'),
('default', 'BUSINESS_STUDIES', 'Business Studies', 'CORE', 4, 'ACTIVE'),
('default', 'CHEMISTRY', 'Chemistry', 'CORE', 5, 'ACTIVE'),
('default', 'COMPUTER_SCIENCE', 'Computer Science', 'CORE', 6, 'ACTIVE'),
('default', 'ECONOMICS', 'Economics', 'CORE', 7, 'ACTIVE'),
('default', 'ENGLISH', 'English', 'CORE', 8, 'ACTIVE'),
('default', 'ENGLISH_CORE', 'English Core', 'CORE', 9, 'ACTIVE'),
('default', 'ENGLISH_LIT', 'English Language & Literature', 'CORE', 10, 'ACTIVE'),
('default', 'EVS', 'Environmental Studies', 'CORE', 11, 'ACTIVE'),
('default', 'GENERAL_AWARENESS', 'General Awareness', 'CO_CURRICULAR', 12, 'ACTIVE'),
('default', 'GEOGRAPHY', 'Geography', 'CORE', 13, 'ACTIVE'),
('default', 'HINDI', 'Hindi', 'CORE', 14, 'ACTIVE'),
('default', 'HINDI_COURSE_A', 'Hindi Course A', 'CORE', 15, 'ACTIVE'),
('default', 'HISTORY', 'History', 'CORE', 16, 'ACTIVE'),
('default', 'IT', 'Information Technology', 'VOCATIONAL', 17, 'ACTIVE'),
('default', 'MATHEMATICS', 'Mathematics', 'CORE', 18, 'ACTIVE'),
('default', 'PHYSICS', 'Physics', 'CORE', 19, 'ACTIVE'),
('default', 'POLITICAL_SCIENCE', 'Political Science', 'CORE', 20, 'ACTIVE'),
('default', 'RHYMES_CONVERSATION', 'Rhymes & Conversation', 'CO_CURRICULAR', 21, 'ACTIVE'),
('default', 'SCIENCE', 'Science', 'CORE', 22, 'ACTIVE'),
('default', 'SOCIAL_SCIENCE', 'Social Science', 'CORE', 23, 'ACTIVE');

-- Populate Class-Section offerings for active session 3 and session 2 from existing student enrollments
INSERT IGNORE INTO academic_class_sections (tenant_id, session_id, class_code, section_code, capacity, status)
SELECT DISTINCT tenant_id, session_id, UPPER(student_class), UPPER(section), 40, 'ACTIVE'
FROM student_enrollments
WHERE session_id IN (2, 3);

-- Populate Class-Subject offerings from assessment schedules for session 2 and 3
INSERT IGNORE INTO academic_class_subjects (tenant_id, session_id, class_code, subject_code, is_mandatory, weekly_periods, status)
SELECT DISTINCT 'default', a.session_id, UPPER(s.student_class), 
    CASE 
        WHEN UPPER(s.subject) LIKE '%ACCOUNTANCY%' THEN 'ACCOUNTANCY'
        WHEN UPPER(s.subject) LIKE '%ART%' THEN 'ART_CRAFT'
        WHEN UPPER(s.subject) LIKE '%BIOLOGY%' THEN 'BIOLOGY'
        WHEN UPPER(s.subject) LIKE '%BUSINESS%' THEN 'BUSINESS_STUDIES'
        WHEN UPPER(s.subject) LIKE '%CHEMISTRY%' THEN 'CHEMISTRY'
        WHEN UPPER(s.subject) LIKE '%COMPUTER%' THEN 'COMPUTER_SCIENCE'
        WHEN UPPER(s.subject) LIKE '%ECONOMICS%' THEN 'ECONOMICS'
        WHEN UPPER(s.subject) = 'ENGLISH' THEN 'ENGLISH'
        WHEN UPPER(s.subject) LIKE '%ENGLISH CORE%' THEN 'ENGLISH_CORE'
        WHEN UPPER(s.subject) LIKE '%ENGLISH LANGUAGE%' THEN 'ENGLISH_LIT'
        WHEN UPPER(s.subject) LIKE '%ENVIRONMENT%' THEN 'EVS'
        WHEN UPPER(s.subject) LIKE '%GENERAL%' THEN 'GENERAL_AWARENESS'
        WHEN UPPER(s.subject) LIKE '%GEOGRAPHY%' THEN 'GEOGRAPHY'
        WHEN UPPER(s.subject) = 'HINDI' THEN 'HINDI'
        WHEN UPPER(s.subject) LIKE '%HINDI COURSE%' THEN 'HINDI_COURSE_A'
        WHEN UPPER(s.subject) LIKE '%HISTORY%' THEN 'HISTORY'
        WHEN UPPER(s.subject) LIKE '%INFORMATION%' THEN 'IT'
        WHEN UPPER(s.subject) LIKE '%MATH%' THEN 'MATHEMATICS'
        WHEN UPPER(s.subject) LIKE '%PHYSICS%' THEN 'PHYSICS'
        WHEN UPPER(s.subject) LIKE '%POLITICAL%' THEN 'POLITICAL_SCIENCE'
        WHEN UPPER(s.subject) LIKE '%RHYMES%' THEN 'RHYMES_CONVERSATION'
        WHEN UPPER(s.subject) = 'SCIENCE' THEN 'SCIENCE'
        WHEN UPPER(s.subject) LIKE '%SOCIAL%' THEN 'SOCIAL_SCIENCE'
        ELSE UPPER(REPLACE(s.subject, ' ', '_'))
    END as subject_code,
    TRUE, 5, 'ACTIVE'
FROM assessment_schedules s
JOIN assessments a ON s.assessment_id = a.id
WHERE a.session_id IS NOT NULL;

-- ====================================================================
-- SEED DATA FOR 'school_b' TENANT (Example Secondary School: Class 1 - 10)
-- ====================================================================

INSERT IGNORE INTO academic_classes (tenant_id, class_code, display_name, display_order, category, status) VALUES
('school_b', '1', 'Class 1', 1, 'PRIMARY', 'ACTIVE'),
('school_b', '2', 'Class 2', 2, 'PRIMARY', 'ACTIVE'),
('school_b', '3', 'Class 3', 3, 'PRIMARY', 'ACTIVE'),
('school_b', '4', 'Class 4', 4, 'PRIMARY', 'ACTIVE'),
('school_b', '5', 'Class 5', 5, 'PRIMARY', 'ACTIVE'),
('school_b', '6', 'Class 6', 6, 'MIDDLE', 'ACTIVE'),
('school_b', '7', 'Class 7', 7, 'MIDDLE', 'ACTIVE'),
('school_b', '8', 'Class 8', 8, 'MIDDLE', 'ACTIVE'),
('school_b', '9', 'Class 9', 9, 'SECONDARY', 'ACTIVE'),
('school_b', '10', 'Class 10', 10, 'SECONDARY', 'ACTIVE');

INSERT IGNORE INTO academic_sections (tenant_id, section_code, display_name, display_order, status) VALUES
('school_b', 'A', 'Section A', 1, 'ACTIVE'),
('school_b', 'B', 'Section B', 2, 'ACTIVE');

INSERT IGNORE INTO academic_subjects (tenant_id, subject_code, display_name, subject_type, display_order, status) VALUES
('school_b', 'ENGLISH', 'English', 'CORE', 1, 'ACTIVE'),
('school_b', 'HINDI', 'Hindi', 'CORE', 2, 'ACTIVE'),
('school_b', 'MATHEMATICS', 'Mathematics', 'CORE', 3, 'ACTIVE'),
('school_b', 'SCIENCE', 'Science', 'CORE', 4, 'ACTIVE'),
('school_b', 'SOCIAL_STUDIES', 'Social Studies', 'CORE', 5, 'ACTIVE'),
('school_b', 'IT', 'Information Technology', 'VOCATIONAL', 6, 'ACTIVE');
