package com.campussync.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record FacultyAccountProvisionRequest(
        @NotBlank @Size(max = 100) String username,
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @NotNull @Positive Long departmentId,
        @NotBlank @Size(max = 100) String employeeNumber,
        @NotBlank @Size(max = 100) String designation,
        @NotBlank @Size(max = 50) String employmentStatus,
        @Size(max = 150) String qualification,
        @jakarta.validation.constraints.Pattern(regexp = "^$|^[+]?[0-9() .-]{7,20}$") String contactNumber,
        @PastOrPresent LocalDate dateOfJoining) {
}
