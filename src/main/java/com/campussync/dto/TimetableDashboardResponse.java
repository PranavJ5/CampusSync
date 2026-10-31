package com.campussync.dto;

import java.time.LocalDateTime;
import java.util.List;

public record TimetableDashboardResponse(
        TimetableEntryResponse nextClass,
        LocalDateTime nextClassStartsAt,
        List<TimetableEntryResponse> today,
        List<TimetableEntryResponse> weekly,
        boolean timetableAvailable,
        boolean labBatchAssignmentRequired) {
}
