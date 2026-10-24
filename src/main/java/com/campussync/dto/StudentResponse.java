package com.campussync.dto;

import com.campussync.entity.Student;
import java.time.LocalDate;

public record StudentResponse(Long id, Long userId, String username, String firstName, String lastName,
                              String institutionalEmail, Long departmentId, String enrollmentNumber,
                              LocalDate admissionDate, Integer admissionYear, Integer currentSemester,
                              String academicStatus, String contactNumber, String alternateEmail,
                              LocalDate dateOfBirth, String gender, String photoPath) {
    public static StudentResponse from(Student student) {
        return new StudentResponse(student.getId(), student.getUser().getId(), student.getUser().getUsername(),
                student.getUser().getFirstName(), student.getUser().getLastName(), student.getUser().getEmail(),
                student.getDepartment().getId(), student.getEnrollmentNumber(), student.getAdmissionDate(),
                student.getAdmissionYear(), student.getCurrentSemester(), student.getAcademicStatus(),
                student.getContactNumber(), student.getAlternateEmail(), student.getDateOfBirth(), student.getGender(), student.getPhotoPath());
    }
}
