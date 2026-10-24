package com.campussync.dto;

import com.campussync.entity.Event;
import java.time.LocalDateTime;

public record EventResponse(Long id, Long committeeId, String title, String description, String category,
                            LocalDateTime startsAt, LocalDateTime endsAt, LocalDateTime registrationDeadline, String venue,
                            Integer capacity, String status, String posterPath, String cancellationReason,
                            Long createdByUserId, LocalDateTime createdAt, LocalDateTime updatedAt, Integer remainingSeats) {
    public static EventResponse from(Event e) {
        return from(e, e.getCapacity());
    }
    public static EventResponse from(Event e, int remainingSeats) {
        return new EventResponse(e.getId(), e.getCommittee().getId(), e.getTitle(), e.getDescription(), e.getCategory(),
                e.getStartsAt(), e.getEndsAt(), e.getRegistrationDeadline(), e.getVenue(), e.getCapacity(), e.getStatus(),
                e.getPosterPath(), e.getCancellationReason(), e.getCreatedBy() == null ? null : e.getCreatedBy().getId(),
                e.getCreatedAt(), e.getUpdatedAt(), Math.max(0, remainingSeats));
    }
}
