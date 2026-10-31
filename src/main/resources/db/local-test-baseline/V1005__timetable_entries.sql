CREATE TABLE timetable_entry (
    timetable_entry_id BIGINT NOT NULL AUTO_INCREMENT,
    subject_id BIGINT NULL,
    faculty_id BIGINT NULL,
    department_id BIGINT NOT NULL,
    semester INT NOT NULL,
    division VARCHAR(20) NOT NULL,
    day_of_week VARCHAR(10) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    venue VARCHAR(255) NULL,
    class_type VARCHAR(20) NOT NULL,
    label VARCHAR(150) NULL,
    academic_year VARCHAR(20) NOT NULL,
    effective_from DATE NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (timetable_entry_id),
    CONSTRAINT uk_timetable_slot UNIQUE
        (department_id, semester, division, academic_year, effective_from, day_of_week, start_time),
    INDEX idx_timetable_student_group (department_id, semester, division, effective_from),
    INDEX idx_timetable_faculty_date (faculty_id, effective_from),
    CONSTRAINT fk_timetable_subject FOREIGN KEY (subject_id) REFERENCES subject (subject_id),
    CONSTRAINT fk_timetable_faculty FOREIGN KEY (faculty_id) REFERENCES faculty (faculty_id),
    CONSTRAINT fk_timetable_department FOREIGN KEY (department_id) REFERENCES department (department_id)
);

ALTER TABLE class_session
    ADD COLUMN timetable_entry_id BIGINT NULL,
    ADD CONSTRAINT uk_class_session_timetable_date UNIQUE (timetable_entry_id, session_date),
    ADD CONSTRAINT fk_class_session_timetable_entry
        FOREIGN KEY (timetable_entry_id) REFERENCES timetable_entry (timetable_entry_id);
