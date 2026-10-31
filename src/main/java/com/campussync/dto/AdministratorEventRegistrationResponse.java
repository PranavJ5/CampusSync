package com.campussync.dto;

import com.campussync.entity.EventRegistration;
import com.campussync.entity.Student;
import java.time.LocalDateTime;

public record AdministratorEventRegistrationResponse(
        Long eventId,
        Long userId,
        String eventName,
        String studentFullName,
        String emailAddress,
        String phoneNumber,
        String enrollmentNumber,
        Integer semester,
        String department,
        String registrationStatus,
        LocalDateTime registeredAt) {

    public static AdministratorEventRegistrationResponse from(EventRegistration registration) {
        var user = registration.getUser();
        Student student = user.getStudent();
        return new AdministratorEventRegistrationResponse(
                registration.getEvent().getId(),
                user.getId(),
                registration.getEvent().getTitle(),
                (user.getFirstName() + " " + user.getLastName()).trim(),
                user.getEmail(),
                student == null ? null : student.getContactNumber(),
                student == null ? null : student.getEnrollmentNumber(),
                student == null ? null : student.getCurrentSemester(),
                student == null || student.getDepartment() == null ? null : student.getDepartment().getName(),
                registration.getRegistrationStatus(),
                registration.getRegisteredAt());
    }
}
