package com.campussync.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record AttendanceRequest(
        @NotBlank @Pattern(regexp = "PRESENT|ABSENT", flags = Pattern.Flag.CASE_INSENSITIVE) String attendanceStatus) { }
