package com.campussync.repository;

import com.campussync.entity.EventRegistration;
import com.campussync.entity.EventRegistrationId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventRegistrationRepository extends JpaRepository<EventRegistration, EventRegistrationId> {

    List<EventRegistration> findByEventId(Long eventId);

    List<EventRegistration> findByUserId(Long userId);

    List<EventRegistration> findByUserIdAndRegistrationStatusOrderByRegisteredAtAsc(Long userId, String registrationStatus);

    Optional<EventRegistration> findByEventIdAndUserId(Long eventId, Long userId);

    List<EventRegistration> findByEventIdOrderByRegisteredAtAsc(Long eventId);

    List<EventRegistration> findByEventIdAndRegistrationStatusOrderByRegisteredAtAsc(Long eventId, String registrationStatus);

    @Query("select count(r) from EventRegistration r where r.event.id = :eventId and upper(r.registrationStatus) = 'CONFIRMED'")
    long countActiveByEventId(@Param("eventId") Long eventId);
}
