package com.campussync.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import java.time.LocalTime;

public record ClassSessionRequest(
        @NotNull @Positive Long subjectId,
        @NotNull LocalDate sessionDate,
        LocalTime startTime,
        LocalTime endTime
) {
}
