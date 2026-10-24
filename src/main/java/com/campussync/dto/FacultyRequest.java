package com.campussync.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Positive;

public record FacultyRequest(
        @NotNull @Positive Long userId,
        @NotNull @Positive Long departmentId,
        @NotBlank @Size(max = 100) String employeeNumber,
        @NotBlank @Size(max = 100) String designation,
        @NotBlank @Size(max = 50) String employmentStatus,
        @Size(max = 150) String qualification,
        @jakarta.validation.constraints.Pattern(regexp = "^$|^[+]?[0-9() .-]{7,20}$") String contactNumber,
        @jakarta.validation.constraints.PastOrPresent java.time.LocalDate dateOfJoining) { }
