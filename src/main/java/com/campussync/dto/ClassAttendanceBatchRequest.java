package com.campussync.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record ClassAttendanceBatchRequest(
        @NotNull List<@Valid AttendanceEntryRequest> entries
) {
}
