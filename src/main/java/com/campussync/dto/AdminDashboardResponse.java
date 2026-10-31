package com.campussync.dto;

import java.util.List;

public record AdminDashboardResponse(
        long users,
        long departments,
        long committees,
        long events,
        long publishedEvents,
        long draftEvents,
        long registrations,
        List<EventResponse> upcomingEvents,
        List<EventRegistrationSummary> registrationCounts
) {
    public record EventRegistrationSummary(Long eventId, String eventTitle, Long registrationCount) {
    }
}
