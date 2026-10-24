package com.campussync.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record EventRequest(
        @NotNull @Positive Long committeeId,
        @NotBlank @Size(max = 200) String title,
        @Size(max = 65535) String description,
        @NotBlank @Size(max = 50) String category,
        @NotNull LocalDateTime startsAt,
        @NotNull LocalDateTime endsAt,
        @NotNull LocalDateTime registrationDeadline,
        @NotBlank @Size(max = 255) String venue,
        @NotNull @Positive Integer capacity) { }
