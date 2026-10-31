package com.campussync.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;

public record StudentRequest(
        @NotNull @Positive Long userId,
        @NotNull @Positive Long departmentId,
        @NotBlank @Size(max = 100) String enrollmentNumber,
        @NotNull @PastOrPresent LocalDate admissionDate,
        @NotBlank @Size(max = 50) String academicStatus,
        @NotNull @Min(1) @Max(8) Integer currentSemester,
        @NotNull @Min(1900) @Max(2100) Integer admissionYear,
        @Pattern(regexp = "^$|^[+]?[0-9() .-]{7,20}$") String contactNumber,
        @Email @Size(max = 255) String alternateEmail,
        @PastOrPresent LocalDate dateOfBirth,
        @Size(max = 30) String gender,
        @Size(max = 500) String photoPath,
        @Size(max = 20) String division) { }
