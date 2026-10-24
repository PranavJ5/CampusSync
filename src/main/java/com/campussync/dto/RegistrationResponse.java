package com.campussync.dto;

import com.campussync.entity.EventRegistration;
import java.time.LocalDateTime;

public record RegistrationResponse(Long eventId, Long userId, String eventTitle, String participantName,
                                   Long departmentId, Integer semester, LocalDateTime eventStartsAt,
                                   LocalDateTime registeredAt, String status, String attendanceStatus) {
    public static RegistrationResponse from(EventRegistration r) {
        var student = r.getUser().getStudent();
        return new RegistrationResponse(r.getEvent().getId(), r.getUser().getId(), r.getEvent().getTitle(),
                r.getUser().getFirstName() + " " + r.getUser().getLastName(),
                student == null || student.getDepartment() == null ? null : student.getDepartment().getId(),
                student == null ? null : student.getCurrentSemester(), r.getEvent().getStartsAt(), r.getRegisteredAt(),
                r.getRegistrationStatus(), r.getAttendanceStatus());
    }
}
