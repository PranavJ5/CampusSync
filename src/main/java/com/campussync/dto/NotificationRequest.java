package com.campussync.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record NotificationRequest(@NotBlank @Size(max = 200) String title,
                                  @NotBlank @Size(max = 2000) String message,
                                  @NotBlank String category,
                                  @NotBlank String audience,
                                  String targetRole,
                                  @Positive Long departmentId,
                                  @jakarta.validation.constraints.Min(1) @jakarta.validation.constraints.Max(8) Integer semester,
                                  @Positive Long committeeId) { }
