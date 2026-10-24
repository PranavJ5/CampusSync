package com.campussync.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

public record DepartmentRequest(
        @NotBlank @Size(max = 50) String code,
        @NotBlank @Size(max = 150) String name,
        @Email @Size(max = 255) String contactEmail,
        @Min(1800) @Max(9999) Integer establishedYear,
        @Size(max = 150) String hodName,
        Boolean active) { }
