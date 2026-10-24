package com.campussync.repository;

import com.campussync.entity.NotificationRecipient;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRecipientRepository extends JpaRepository<NotificationRecipient, Long> {
    Page<NotificationRecipient> findByUserIdOrderByNotificationCreatedAtDesc(Long userId, Pageable pageable);
    Page<NotificationRecipient> findByUserIdAndReadOrderByNotificationCreatedAtDesc(Long userId, boolean read, Pageable pageable);
    Page<NotificationRecipient> findByUserIdAndNotificationCategoryIgnoreCaseOrderByNotificationCreatedAtDesc(Long userId, String category, Pageable pageable);
    Page<NotificationRecipient> findByUserIdAndReadAndNotificationCategoryIgnoreCaseOrderByNotificationCreatedAtDesc(Long userId, boolean read, String category, Pageable pageable);
    Optional<NotificationRecipient> findByNotificationIdAndUserId(Long notificationId, Long userId);
    long countByUserIdAndReadFalse(Long userId);

    long countByUserIdAndReadFalseAndNotificationCategoryIgnoreCase(Long userId, String category);

    java.util.List<NotificationRecipient> findByUserIdAndReadFalse(Long userId);
}
