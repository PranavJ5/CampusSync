package com.campussync.dto;

import com.campussync.entity.NotificationRecipient;
import java.time.LocalDateTime;

public record NotificationResponse(Long id, String title, String message, String category,
                                   LocalDateTime createdAt, Long eventId, Long originatorUserId,
                                   String originatorUsername, boolean read, LocalDateTime readAt) {
    public static NotificationResponse from(NotificationRecipient recipient) {
        var notification = recipient.getNotification();
        return new NotificationResponse(notification.getId(), notification.getTitle(), notification.getMessage(),
                notification.getCategory(), notification.getCreatedAt(),
                notification.getRelatedEvent() == null ? null : notification.getRelatedEvent().getId(),
                notification.getCreatedBy().getId(), notification.getCreatedBy().getUsername(),
                recipient.isRead(), recipient.getReadAt());
    }
}
