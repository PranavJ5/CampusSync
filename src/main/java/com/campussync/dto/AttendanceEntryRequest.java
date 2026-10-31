package com.campussync.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record AttendanceEntryRequest(
        @NotNull @Positive Long studentId,
        @NotNull @Size(max = 20) String status
) {
}
