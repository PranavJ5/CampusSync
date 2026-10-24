package com.campussync.repository;

import com.campussync.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    boolean existsByEventNotificationKey(String eventNotificationKey);
}
