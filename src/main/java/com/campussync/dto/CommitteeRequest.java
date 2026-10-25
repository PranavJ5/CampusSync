package com.campussync.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

public record CommitteeRequest(@jakarta.validation.constraints.Positive Long departmentId,
                               @NotBlank @Size(max = 50) String code,
                               @NotBlank @Size(max = 150) String name,
                               @Size(max = 65535) String description,
                               @NotBlank @Size(max = 50) String status,
                               @NotBlank @Size(max = 50) String category,
                               @Min(1800) @Max(9999) Integer establishedYear,
                               @jakarta.validation.constraints.Positive Long facultyCoordinatorId,
                               @Size(max = 500) String logoPath,
                               Boolean active) { }
