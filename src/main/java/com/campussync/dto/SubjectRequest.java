package com.campussync.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record SubjectRequest(
        @NotNull @Positive Long departmentId,
        @NotBlank @Size(max = 50) String code,
        @NotBlank @Size(max = 150) String name,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) @Digits(integer = 3, fraction = 1) BigDecimal creditValue,
        @NotNull @jakarta.validation.constraints.Min(1) @jakarta.validation.constraints.Max(8) Integer semester,
        @NotBlank @Size(max = 20) String academicYear,
        @jakarta.validation.constraints.Positive Long facultyId,
        @Size(max = 20) String division) { }
