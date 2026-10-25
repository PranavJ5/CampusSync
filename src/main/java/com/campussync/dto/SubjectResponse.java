package com.campussync.dto;

import com.campussync.entity.Subject;
import java.math.BigDecimal;

public record SubjectResponse(Long id, Long departmentId, String code, String name, BigDecimal creditValue,
                              Integer semester, String academicYear, Long facultyId, String division) {
    public static SubjectResponse from(Subject subject) {
        return new SubjectResponse(subject.getId(), subject.getDepartment().getId(), subject.getCode(), subject.getName(),
                subject.getCreditValue(), subject.getSemester(), subject.getAcademicYear(),
                subject.getFaculty() == null ? null : subject.getFaculty().getId(), subject.getDivision());
    }
}
