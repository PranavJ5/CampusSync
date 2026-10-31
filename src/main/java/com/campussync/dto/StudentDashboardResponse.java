package com.campussync.dto;

import java.util.List;

public record StudentDashboardResponse(
        List<StudentDashboardEventResponse> upcomingEvents,
        List<RegistrationResponse> registrations,
        List<NotificationResponse> notifications,
        long unreadNotifications,
        long activeRegistrations,
        boolean timetableAvailable
) {
}
