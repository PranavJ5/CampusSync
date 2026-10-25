package com.campussync.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import java.util.List;

public record AttendanceBatchRequest(@Positive Long eventId, @NotEmpty List<@Valid AttendanceEntry> participants) { }
