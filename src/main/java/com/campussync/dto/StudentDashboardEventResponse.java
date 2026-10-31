package com.campussync.dto;

import com.campussync.entity.Event;
import java.time.LocalDateTime;

public record StudentDashboardEventResponse(
        Long id,
        String title,
        String category,
        LocalDateTime startsAt,
        String venue,
        Integer capacity,
        Integer remainingSeats,
        Long registeredCount,
        String registrationStatus
) {
    public static StudentDashboardEventResponse from(
            Event event, long registeredCount, String registrationStatus) {
        int remainingSeats = Math.toIntExact(Math.max(0L, (long) event.getCapacity() - registeredCount));
        return new StudentDashboardEventResponse(
                event.getId(),
                event.getTitle(),
                event.getCategory(),
                event.getStartsAt(),
                event.getVenue(),
                event.getCapacity(),
                remainingSeats,
                registeredCount,
                registrationStatus);
    }
}
