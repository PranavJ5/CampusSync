package com.campussync.dto;

import com.campussync.entity.AttendanceChange;
import java.time.LocalDateTime;

public record AttendanceChangeResponse(Long id, Long attendanceId, Long studentId, String studentName,
                                       String previousStatus, String newStatus, String changedBy,
                                       LocalDateTime changedAt) {
    public static AttendanceChangeResponse from(AttendanceChange change) {
        var student = change.getAttendance().getStudent();
        return new AttendanceChangeResponse(change.getId(), change.getAttendance().getId(), student.getId(),
                student.getUser().getFirstName() + " " + student.getUser().getLastName(),
                change.getPreviousStatus(), change.getNewStatus(),
                change.getChangedBy().getUser().getFirstName() + " " + change.getChangedBy().getUser().getLastName(),
                change.getChangedAt());
    }
}
