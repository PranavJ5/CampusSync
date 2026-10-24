package com.campussync.dto;

import com.campussync.entity.Faculty;

public record FacultyResponse(Long id, Long userId, Long departmentId, String employeeNumber,
                              String designation, String employmentStatus, String qualification,
                              String contactNumber, java.time.LocalDate dateOfJoining,
                              String fullName, String institutionalEmail) {
    public static FacultyResponse from(Faculty faculty) {
        return new FacultyResponse(faculty.getId(), faculty.getUser().getId(), faculty.getDepartment().getId(),
                faculty.getEmployeeNumber(), faculty.getDesignation(), faculty.getEmploymentStatus(), faculty.getQualification(),
                faculty.getContactNumber(), faculty.getDateOfJoining(), faculty.getUser().getFirstName() + " " + faculty.getUser().getLastName(),
                faculty.getUser().getEmail());
    }
}
