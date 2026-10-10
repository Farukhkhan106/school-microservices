-- ====================================================================
-- SUCCESS ACADEMY ERP - ACADEMIC SERVICE BASELINE MIGRATION (V1)
-- ====================================================================

CREATE TABLE IF NOT EXISTS academic_sessions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT FALSE,
    description VARCHAR(500),
    created_at DATETIME NOT NULL,
    updated_at DATETIME
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS grading_schemes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    is_default BOOLEAN NOT NULL DEFAULT FALSE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS grading_rules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    grading_scheme_id BIGINT NOT NULL,
    grade VARCHAR(10) NOT NULL,
    min_percentage DECIMAL(5,2) NOT NULL,
    max_percentage DECIMAL(5,2) NOT NULL,
    grade_point DECIMAL(4,2),
    remarks VARCHAR(100),
    CONSTRAINT fk_grading_rule_scheme FOREIGN KEY (grading_scheme_id) REFERENCES grading_schemes(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS assessments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id BIGINT NOT NULL,
    name VARCHAR(150) NOT NULL,
    assessment_type VARCHAR(50) NOT NULL,
    term VARCHAR(50),
    start_date DATE,
    end_date DATE,
    grading_scheme_id BIGINT,
    is_rank_visible BOOLEAN NOT NULL DEFAULT TRUE,
    status VARCHAR(30) NOT NULL DEFAULT 'SCHEDULED',
    created_by BIGINT,
    description VARCHAR(500),
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    CONSTRAINT fk_assessment_session FOREIGN KEY (session_id) REFERENCES academic_sessions(id),
    CONSTRAINT fk_assessment_grading_scheme FOREIGN KEY (grading_scheme_id) REFERENCES grading_schemes(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS assessment_schedules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    assessment_id BIGINT NOT NULL,
    student_class VARCHAR(20) NOT NULL,
    section VARCHAR(10) NOT NULL,
    subject VARCHAR(100) NOT NULL,
    exam_date DATE NOT NULL,
    start_time TIME,
    end_time TIME,
    max_marks DECIMAL(5,2) NOT NULL,
    pass_marks DECIMAL(5,2) NOT NULL,
    assigned_teacher_id BIGINT,
    is_marks_submitted BOOLEAN NOT NULL DEFAULT FALSE,
    is_marks_verified BOOLEAN NOT NULL DEFAULT FALSE,
    is_locked BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_schedule_assessment FOREIGN KEY (assessment_id) REFERENCES assessments(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS student_marks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    schedule_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    student_name VARCHAR(100),
    roll_no VARCHAR(50),
    marks_obtained DECIMAL(5,2),
    is_absent BOOLEAN NOT NULL DEFAULT FALSE,
    grade VARCHAR(10),
    grade_point DECIMAL(4,2),
    is_passing BOOLEAN NOT NULL DEFAULT FALSE,
    remarks VARCHAR(500),
    entered_by BIGINT,
    entered_at DATETIME,
    verified_by BIGINT,
    verified_at DATETIME,
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    CONSTRAINT uk_mark_schedule_student UNIQUE (schedule_id, student_id),
    CONSTRAINT fk_mark_schedule FOREIGN KEY (schedule_id) REFERENCES assessment_schedules(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS student_result_summaries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    assessment_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    student_name VARCHAR(100),
    roll_no VARCHAR(50),
    student_class VARCHAR(20) NOT NULL,
    section VARCHAR(10) NOT NULL,
    total_max_marks DECIMAL(6,2),
    total_obtained_marks DECIMAL(6,2),
    overall_percentage DECIMAL(5,2),
    overall_grade VARCHAR(10),
    overall_grade_point DECIMAL(4,2),
    is_passed BOOLEAN NOT NULL DEFAULT FALSE,
    class_rank INT,
    section_rank INT,
    generated_at DATETIME NOT NULL,
    CONSTRAINT uk_summary_assessment_student UNIQUE (assessment_id, student_id),
    CONSTRAINT fk_summary_assessment FOREIGN KEY (assessment_id) REFERENCES assessments(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS academic_activities (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(150) NOT NULL,
    description VARCHAR(1000),
    activity_type VARCHAR(50) NOT NULL,
    student_class VARCHAR(20) NOT NULL,
    section VARCHAR(10) NOT NULL,
    subject VARCHAR(100) NOT NULL,
    teacher_id BIGINT NOT NULL,
    teacher_name VARCHAR(100),
    assigned_date DATE NOT NULL,
    due_date DATE NOT NULL,
    max_marks DECIMAL(5,2) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    instructions VARCHAR(2000),
    is_graded BOOLEAN NOT NULL DEFAULT TRUE,
    submission_required BOOLEAN NOT NULL DEFAULT TRUE,
    created_by_username VARCHAR(100),
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    INDEX idx_activity_teacher_status (teacher_id, status),
    INDEX idx_activity_class_sec_status (student_class, section, status),
    INDEX idx_activity_class_sec_subj (student_class, section, subject),
    INDEX idx_activity_due_date (due_date),
    INDEX idx_activity_type (activity_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS activity_submissions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    activity_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    student_name VARCHAR(100),
    student_admission_no VARCHAR(50),
    submission_text VARCHAR(5000),
    attachment_url VARCHAR(500),
    attachment_name VARCHAR(255),
    attachment_size BIGINT,
    attachment_type VARCHAR(100),
    submitted_at DATETIME NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'SUBMITTED',
    attempt_number INT NOT NULL DEFAULT 1,
    teacher_feedback VARCHAR(2000),
    evaluated_at DATETIME,
    evaluated_by BIGINT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    CONSTRAINT uk_submission_activity_student UNIQUE (activity_id, student_id),
    INDEX idx_submission_activity_student (activity_id, student_id),
    INDEX idx_submission_student (student_id),
    INDEX idx_submission_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS activity_results (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    activity_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    student_name VARCHAR(100),
    student_admission_no VARCHAR(50),
    obtained_marks DECIMAL(5,2),
    max_marks DECIMAL(5,2) NOT NULL,
    percentage DECIMAL(5,2),
    is_absent BOOLEAN NOT NULL DEFAULT FALSE,
    grade VARCHAR(10),
    teacher_feedback VARCHAR(2000),
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    evaluated_by BIGINT,
    evaluated_at DATETIME,
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    CONSTRAINT uk_result_activity_student UNIQUE (activity_id, student_id),
    INDEX idx_result_activity_student (activity_id, student_id),
    INDEX idx_result_student (student_id),
    INDEX idx_result_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS academic_audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    actor_user_id BIGINT,
    actor_role VARCHAR(50),
    action VARCHAR(100) NOT NULL,
    entity_name VARCHAR(100),
    entity_id BIGINT,
    old_value VARCHAR(1000),
    new_value VARCHAR(1000),
    description VARCHAR(2000),
    timestamp DATETIME NOT NULL,
    INDEX idx_audit_entity (entity_name, entity_id),
    INDEX idx_audit_timestamp (timestamp)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
