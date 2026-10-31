package com.campussync.dto;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

public record TimetableEntryRequest(
        @Positive Long subjectId,
        @Positive Long departmentId,
        @Min(1) @Max(8) Integer semester,
        @Size(max = 20) String division,
        @Pattern(regexp = "^(B[1-4]|b[1-4])$") String labBatch,
        @NotNull DayOfWeek dayOfWeek,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime,
        @Size(max = 255) String venue,
        @NotBlank @Size(max = 20) String classType,
        @Size(max = 150) String label,
        @NotBlank @Size(max = 20) String academicYear,
        @NotNull LocalDate effectiveFrom,
        Boolean active) {
}
