-- DEMO ONLY: core CampusSync schema for a newly created campussync_demo database.
-- This schema was derived from the JPA entity mappings and reviewed against, not copied blindly from,
-- db/local-test-baseline/V1000__campussync_local_test_baseline.sql.
-- No roles, departments, users, academic records, or other seed data are inserted here.
-- Apply only through the guarded DemoSchemaMigration entry point.

CREATE TABLE `role` (
    role_id BIGINT NOT NULL AUTO_INCREMENT,
    role_code VARCHAR(50) NOT NULL,
    role_name VARCHAR(100) NOT NULL,
    description VARCHAR(255) NULL,
    PRIMARY KEY (role_id),
    CONSTRAINT uk_role_code UNIQUE (role_code),
    CONSTRAINT uk_role_name UNIQUE (role_name)
) ENGINE=InnoDB;

CREATE TABLE department (
    department_id BIGINT NOT NULL AUTO_INCREMENT,
    department_code VARCHAR(50) NOT NULL,
    department_name VARCHAR(150) NOT NULL,
    contact_email VARCHAR(255) NULL,
    established_year INT NULL,
    hod_name VARCHAR(150) NULL,
    is_active TINYINT(1) NOT NULL,
    PRIMARY KEY (department_id),
    CONSTRAINT uk_department_code UNIQUE (department_code),
    CONSTRAINT uk_department_name UNIQUE (department_name)
) ENGINE=InnoDB;

CREATE TABLE `user` (
    user_id BIGINT NOT NULL AUTO_INCREMENT,
    role_id BIGINT NOT NULL,
    username VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    is_active TINYINT(1) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (user_id),
    CONSTRAINT uk_user_username UNIQUE (username),
    CONSTRAINT uk_user_email UNIQUE (email),
    KEY idx_user_role_id (role_id),
    CONSTRAINT fk_user_role FOREIGN KEY (role_id)
        REFERENCES `role` (role_id) ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE student (
    student_id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    department_id BIGINT NOT NULL,
    enrollment_number VARCHAR(100) NOT NULL,
    admission_date DATE NOT NULL,
    current_semester INT NULL,
    admission_year INT NULL,
    date_of_birth DATE NULL,
    contact_number VARCHAR(20) NULL,
    alternate_email VARCHAR(255) NULL,
    gender VARCHAR(30) NULL,
    photo_path VARCHAR(500) NULL,
    academic_status VARCHAR(50) NOT NULL,
    PRIMARY KEY (student_id),
    CONSTRAINT uk_student_user UNIQUE (user_id),
    CONSTRAINT uk_student_enrollment UNIQUE (enrollment_number),
    KEY idx_student_department_id (department_id),
    CONSTRAINT fk_student_user FOREIGN KEY (user_id)
        REFERENCES `user` (user_id) ON DELETE RESTRICT,
    CONSTRAINT fk_student_department FOREIGN KEY (department_id)
        REFERENCES department (department_id) ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE faculty (
    faculty_id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    department_id BIGINT NOT NULL,
    employee_number VARCHAR(100) NOT NULL,
    designation VARCHAR(100) NOT NULL,
    employment_status VARCHAR(50) NOT NULL,
    qualification VARCHAR(150) NULL,
    contact_number VARCHAR(20) NULL,
    date_of_joining DATE NULL,
    PRIMARY KEY (faculty_id),
    CONSTRAINT uk_faculty_user UNIQUE (user_id),
    CONSTRAINT uk_faculty_employee_number UNIQUE (employee_number),
    KEY idx_faculty_department_id (department_id),
    CONSTRAINT fk_faculty_user FOREIGN KEY (user_id)
        REFERENCES `user` (user_id) ON DELETE RESTRICT,
    CONSTRAINT fk_faculty_department FOREIGN KEY (department_id)
        REFERENCES department (department_id) ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE committee (
    committee_id BIGINT NOT NULL AUTO_INCREMENT,
    department_id BIGINT NULL,
    committee_code VARCHAR(50) NOT NULL,
    committee_name VARCHAR(150) NOT NULL,
    description TEXT NULL,
    category VARCHAR(50) NOT NULL,
    established_year INT NULL,
    faculty_coordinator_id BIGINT NULL,
    logo_path VARCHAR(500) NULL,
    is_active TINYINT(1) NOT NULL,
    status VARCHAR(50) NOT NULL,
    PRIMARY KEY (committee_id),
    CONSTRAINT uk_committee_code UNIQUE (committee_code),
    KEY idx_committee_department_id (department_id),
    KEY idx_committee_faculty_coordinator_id (faculty_coordinator_id),
    CONSTRAINT fk_committee_department FOREIGN KEY (department_id)
        REFERENCES department (department_id) ON DELETE RESTRICT,
    CONSTRAINT fk_committee_faculty_coordinator FOREIGN KEY (faculty_coordinator_id)
        REFERENCES faculty (faculty_id) ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE subject (
    subject_id BIGINT NOT NULL AUTO_INCREMENT,
    department_id BIGINT NOT NULL,
    subject_code VARCHAR(50) NOT NULL,
    subject_name VARCHAR(150) NOT NULL,
    credit_value DECIMAL(4,1) NOT NULL,
    semester INT NULL,
    academic_year VARCHAR(20) NULL,
    faculty_id BIGINT NULL,
    division VARCHAR(20) NULL,
    PRIMARY KEY (subject_id),
    CONSTRAINT uk_subject_code UNIQUE (subject_code),
    KEY idx_subject_department_id (department_id),
    KEY idx_subject_faculty_id (faculty_id),
    CONSTRAINT fk_subject_department FOREIGN KEY (department_id)
        REFERENCES department (department_id) ON DELETE RESTRICT,
    CONSTRAINT fk_subject_faculty FOREIGN KEY (faculty_id)
        REFERENCES faculty (faculty_id) ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE committee_member (
    member_id BIGINT NOT NULL AUTO_INCREMENT,
    committee_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    member_role VARCHAR(100) NOT NULL,
    joined_on DATE NOT NULL,
    left_on DATE NULL,
    membership_status VARCHAR(50) NOT NULL,
    active_user_id BIGINT GENERATED ALWAYS AS (
        CASE
            WHEN UPPER(TRIM(membership_status)) = 'ACTIVE' THEN user_id
            ELSE NULL
        END
    ) STORED,
    PRIMARY KEY (member_id),
    UNIQUE KEY uk_committee_member_one_active (committee_id, active_user_id),
    KEY idx_committee_member_committee_status (committee_id, membership_status),
    KEY idx_committee_member_user_id (user_id),
    CONSTRAINT fk_committee_member_committee FOREIGN KEY (committee_id)
        REFERENCES committee (committee_id) ON DELETE RESTRICT,
    CONSTRAINT fk_committee_member_user FOREIGN KEY (user_id)
        REFERENCES `user` (user_id) ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE `event` (
    event_id BIGINT NOT NULL AUTO_INCREMENT,
    committee_id BIGINT NOT NULL,
    event_title VARCHAR(200) NOT NULL,
    description TEXT NULL,
    category VARCHAR(50) NOT NULL,
    starts_at DATETIME(6) NOT NULL,
    ends_at DATETIME(6) NOT NULL,
    registration_deadline DATETIME(6) NOT NULL,
    venue VARCHAR(255) NOT NULL,
    capacity INT NOT NULL,
    event_status VARCHAR(50) NOT NULL,
    poster_path VARCHAR(500) NULL,
    cancellation_reason VARCHAR(1000) NULL,
    created_by BIGINT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (event_id),
    KEY idx_event_committee_starts_at (committee_id, starts_at),
    KEY idx_event_starts_at (starts_at),
    KEY idx_event_created_by (created_by),
    CONSTRAINT fk_event_committee FOREIGN KEY (committee_id)
        REFERENCES committee (committee_id) ON DELETE RESTRICT,
    CONSTRAINT fk_event_created_by FOREIGN KEY (created_by)
        REFERENCES `user` (user_id) ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE event_registration (
    event_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    registered_at DATETIME(6) NOT NULL,
    registration_status VARCHAR(50) NOT NULL,
    attendance_status VARCHAR(50) NOT NULL,
    PRIMARY KEY (event_id, user_id),
    KEY idx_event_registration_user_id (user_id),
    KEY idx_event_registration_event_status (event_id, registration_status),
    CONSTRAINT fk_event_registration_event FOREIGN KEY (event_id)
        REFERENCES `event` (event_id) ON DELETE RESTRICT,
    CONSTRAINT fk_event_registration_user FOREIGN KEY (user_id)
        REFERENCES `user` (user_id) ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE notification (
    notification_id BIGINT NOT NULL AUTO_INCREMENT,
    title VARCHAR(200) NOT NULL,
    message VARCHAR(2000) NOT NULL,
    category VARCHAR(50) NOT NULL,
    created_by BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    target_role VARCHAR(50) NULL,
    target_department_id BIGINT NULL,
    related_event_id BIGINT NULL,
    event_notification_key VARCHAR(120) NULL,
    PRIMARY KEY (notification_id),
    UNIQUE KEY uk_notification_event_key (event_notification_key),
    KEY idx_notification_created_at (created_at),
    KEY idx_notification_created_by (created_by),
    KEY idx_notification_target_department_id (target_department_id),
    KEY idx_notification_related_event_id (related_event_id),
    CONSTRAINT fk_notification_created_by FOREIGN KEY (created_by)
        REFERENCES `user` (user_id) ON DELETE RESTRICT,
    CONSTRAINT fk_notification_target_department FOREIGN KEY (target_department_id)
        REFERENCES department (department_id) ON DELETE RESTRICT,
    CONSTRAINT fk_notification_related_event FOREIGN KEY (related_event_id)
        REFERENCES `event` (event_id) ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE notification_recipient (
    recipient_id BIGINT NOT NULL AUTO_INCREMENT,
    notification_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    is_read TINYINT(1) NOT NULL,
    read_at DATETIME(6) NULL,
    PRIMARY KEY (recipient_id),
    CONSTRAINT uk_notification_recipient_notification_user
        UNIQUE (notification_id, user_id),
    KEY idx_notification_recipient_user_read (user_id, is_read),
    CONSTRAINT fk_notification_recipient_notification FOREIGN KEY (notification_id)
        REFERENCES notification (notification_id) ON DELETE RESTRICT,
    CONSTRAINT fk_notification_recipient_user FOREIGN KEY (user_id)
        REFERENCES `user` (user_id) ON DELETE RESTRICT
) ENGINE=InnoDB;
