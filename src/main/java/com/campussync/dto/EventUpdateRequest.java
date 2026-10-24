package com.campussync.dto;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDateTime;

public record EventUpdateRequest(@Size(max = 200) @Pattern(regexp = "(?s).*\\S.*") String title,
                                 @Size(max = 65535) String description,
                                 @Size(max = 50) @Pattern(regexp = "(?s).*\\S.*") String category,
                                 LocalDateTime startsAt,
                                 LocalDateTime endsAt,
                                 LocalDateTime registrationDeadline,
                                 @Size(max = 255) @Pattern(regexp = "(?s).*\\S.*") String venue,
                                 @Positive Integer capacity) { }
