package com.campussync.dto;

import com.campussync.entity.AttendanceRecord;
import com.campussync.entity.Student;

public record AttendanceRosterEntryResponse(Long studentId, String enrollmentNumber,
                                            String firstName, String lastName, String status) {
    public static AttendanceRosterEntryResponse from(Student student, AttendanceRecord record) {
        return new AttendanceRosterEntryResponse(student.getId(), student.getEnrollmentNumber(),
                student.getUser().getFirstName(), student.getUser().getLastName(),
                record == null ? null : record.getStatus());
    }
}
