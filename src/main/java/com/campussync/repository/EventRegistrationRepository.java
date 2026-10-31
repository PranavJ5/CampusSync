package com.campussync.repository;

import com.campussync.entity.EventRegistration;
import com.campussync.entity.EventRegistrationId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.EntityGraph;

public interface EventRegistrationRepository extends JpaRepository<EventRegistration, EventRegistrationId> {

    List<EventRegistration> findByEventId(Long eventId);

    List<EventRegistration> findByUserId(Long userId);

    List<EventRegistration> findByUserIdOrderByRegisteredAtDesc(Long userId);

    @EntityGraph(attributePaths = {"event", "user", "user.student", "user.student.department"})
    List<EventRegistration> findTop5ByUserIdOrderByRegisteredAtDesc(Long userId);

    List<EventRegistration> findByUserIdAndEventIdIn(Long userId, List<Long> eventIds);

    List<EventRegistration> findByUserIdAndRegistrationStatusOrderByRegisteredAtAsc(Long userId, String registrationStatus);

    Optional<EventRegistration> findByEventIdAndUserId(Long eventId, Long userId);

    @EntityGraph(attributePaths = {"event", "user", "user.student", "user.student.department"})
    List<EventRegistration> findByEventIdOrderByRegisteredAtAsc(Long eventId);

    List<EventRegistration> findByEventIdAndRegistrationStatusOrderByRegisteredAtAsc(Long eventId, String registrationStatus);

    @Query("select count(r) from EventRegistration r where r.event.id = :eventId and upper(r.registrationStatus) = 'CONFIRMED'")
    long countActiveByEventId(@Param("eventId") Long eventId);

    @Query("select count(r) from EventRegistration r where upper(r.registrationStatus) = 'CONFIRMED'")
    long countConfirmed();

    long countByUserIdAndRegistrationStatusIgnoreCase(Long userId, String registrationStatus);

    @Query("select r.event.id as eventId, count(r) as registrationCount "
            + "from EventRegistration r where r.event.id in :eventIds "
            + "and upper(r.registrationStatus) = 'CONFIRMED' group by r.event.id")
    List<EventRegistrationCountView> countConfirmedByEventIds(@Param("eventIds") List<Long> eventIds);

    interface EventRegistrationCountView {
        Long getEventId();
        Long getRegistrationCount();
    }
}
