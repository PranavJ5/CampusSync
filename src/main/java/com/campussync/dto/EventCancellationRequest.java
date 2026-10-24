package com.campussync.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EventCancellationRequest(@NotBlank @Size(max = 1000) String reason) { }
