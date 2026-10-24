package com.campussync.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Pattern;

public record AttendanceEntry(@Positive Long userId,
                              @NotBlank @Pattern(regexp = "(?i)PRESENT|ABSENT") String attendanceStatus) { }
