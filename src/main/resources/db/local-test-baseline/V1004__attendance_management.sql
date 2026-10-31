ALTER TABLE student
    ADD COLUMN division VARCHAR(20) NULL;

CREATE INDEX idx_student_attendance_group
    ON student (department_id, current_semester, division, academic_status);

CREATE TABLE class_session (
    session_id BIGINT NOT NULL AUTO_INCREMENT,
    subject_id BIGINT NOT NULL,
    faculty_id BIGINT NOT NULL,
    department_id BIGINT NOT NULL,
    semester INT NOT NULL,
    division VARCHAR(20) NOT NULL,
    session_date DATE NOT NULL,
    start_time TIME NULL,
    end_time TIME NULL,
    session_status VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
    PRIMARY KEY (session_id),
    INDEX idx_class_session_subject_date (subject_id, session_date),
    INDEX idx_class_session_faculty_date (faculty_id, session_date),
    INDEX idx_class_session_group (department_id, semester, division),
    CONSTRAINT fk_class_session_subject FOREIGN KEY (subject_id) REFERENCES subject (subject_id),
    CONSTRAINT fk_class_session_faculty FOREIGN KEY (faculty_id) REFERENCES faculty (faculty_id),
    CONSTRAINT fk_class_session_department FOREIGN KEY (department_id) REFERENCES department (department_id)
);

CREATE TABLE attendance_record (
    attendance_id BIGINT NOT NULL AUTO_INCREMENT,
    session_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    attendance_status VARCHAR(20) NOT NULL,
    recorded_by_faculty_id BIGINT NOT NULL,
    recorded_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (attendance_id),
    CONSTRAINT uk_attendance_session_student UNIQUE (session_id, student_id),
    INDEX idx_attendance_student_session (student_id, session_id),
    CONSTRAINT fk_attendance_record_session FOREIGN KEY (session_id) REFERENCES class_session (session_id),
    CONSTRAINT fk_attendance_record_student FOREIGN KEY (student_id) REFERENCES student (student_id),
    CONSTRAINT fk_attendance_record_faculty FOREIGN KEY (recorded_by_faculty_id) REFERENCES faculty (faculty_id)
);

CREATE TABLE attendance_change (
    attendance_change_id BIGINT NOT NULL AUTO_INCREMENT,
    attendance_id BIGINT NOT NULL,
    previous_status VARCHAR(20) NULL,
    new_status VARCHAR(20) NOT NULL,
    changed_by_faculty_id BIGINT NOT NULL,
    changed_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (attendance_change_id),
    INDEX idx_attendance_change_recorded_at (attendance_id, changed_at),
    CONSTRAINT fk_attendance_change_record FOREIGN KEY (attendance_id) REFERENCES attendance_record (attendance_id),
    CONSTRAINT fk_attendance_change_faculty FOREIGN KEY (changed_by_faculty_id) REFERENCES faculty (faculty_id)
);
