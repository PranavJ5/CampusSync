ALTER TABLE student
    ADD COLUMN lab_batch VARCHAR(10) NULL AFTER division,
    DROP INDEX idx_student_attendance_group,
    ADD INDEX idx_student_attendance_group
        (department_id, current_semester, division, lab_batch, academic_status);

ALTER TABLE timetable_entry
    ADD COLUMN lab_batch VARCHAR(10) NOT NULL DEFAULT 'ALL' AFTER division,
    DROP INDEX idx_timetable_student_group,
    ADD INDEX idx_timetable_student_group
        (department_id, semester, division, lab_batch, effective_from),
    DROP INDEX uk_timetable_slot,
    ADD CONSTRAINT uk_timetable_slot UNIQUE
        (department_id, semester, division, academic_year, effective_from,
         day_of_week, start_time, lab_batch);

ALTER TABLE class_session
    ADD COLUMN lab_batch VARCHAR(10) NULL AFTER division;
