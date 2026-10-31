package com.campussync.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record ScheduledClassSessionRequest(@NotNull LocalDate sessionDate) {
}
