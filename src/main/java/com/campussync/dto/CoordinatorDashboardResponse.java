package com.campussync.dto;

import java.util.List;

public record CoordinatorDashboardResponse(
        long committeeEvents,
        long draftEvents,
        long participantCount,
        List<EventResponse> upcomingEvents
) {
}
