package com.campussync.repository;

import com.campussync.entity.EventRegistration;
import com.campussync.entity.EventRegistrationId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EventRegistrationRepository extends JpaRepository<EventRegistration, EventRegistrationId> {

    List<EventRegistration> findByEventId(Long eventId);

    List<EventRegistration> findByUserId(Long userId);

    Optional<EventRegistration> findByEventIdAndUserId(Long eventId, Long userId);
}
