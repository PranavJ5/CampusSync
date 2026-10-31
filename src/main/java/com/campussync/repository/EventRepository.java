package com.campussync.repository;

import com.campussync.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import org.springframework.data.domain.Pageable;

public interface EventRepository extends JpaRepository<Event, Long> {

    List<Event> findByCommitteeIdOrderByStartsAtAsc(Long committeeId);

    List<Event> findByCommitteeIdInOrderByStartsAtAsc(List<Long> committeeIds);

    List<Event> findByStatusOrderByStartsAtAsc(String status);

    List<Event> findByStatusInOrderByStartsAtAsc(List<String> statuses);

    List<Event> findAllByOrderByStartsAtAsc();

    @Query("select e from Event e join e.committee c left join c.department d where upper(e.status) in :statuses "
            + "and (:category is null or upper(e.category) = upper(:category)) "
            + "and (:committeeId is null or c.id = :committeeId) "
            + "and (:departmentId is null or d.id = :departmentId) "
            + "and (:fromTime is null or e.startsAt >= :fromTime) "
            + "and (:toTime is null or e.startsAt <= :toTime) order by e.startsAt asc")
    List<Event> browsePublicEvents(@Param("statuses") List<String> statuses, @Param("category") String category,
                                  @Param("committeeId") Long committeeId, @Param("departmentId") Long departmentId,
                                  @Param("fromTime") java.time.LocalDateTime fromTime,
                                  @Param("toTime") java.time.LocalDateTime toTime);

    List<Event> findByStatusAndRegistrationDeadlineLessThanEqual(String status, java.time.LocalDateTime deadline);

    List<Event> findByStatusInAndStartsAtBetween(List<String> statuses, java.time.LocalDateTime from, java.time.LocalDateTime to);

    List<Event> findByStatusInAndStartsAtGreaterThanEqualOrderByStartsAtAsc(
            List<String> statuses, java.time.LocalDateTime startsAt);

    @Query("select e from Event e where upper(e.status) in ('PUBLISHED', 'REGISTRATION_CLOSED') "
            + "and e.startsAt >= :startsAt "
            + "order by e.startsAt asc")
    List<Event> findUpcomingPublished(@Param("startsAt") java.time.LocalDateTime startsAt, Pageable pageable);

    long countByStatusIgnoreCase(String status);

    @Query("select count(e) from Event e where upper(e.status) in :statuses")
    long countByStatuses(@Param("statuses") List<String> statuses);

    @Query("select e.id as eventId, e.title as eventTitle, count(r) as registrationCount "
            + "from Event e left join e.registrations r on upper(r.registrationStatus) = 'CONFIRMED' "
            + "group by e.id, e.title order by e.title")
    List<EventRegistrationCountView> registrationCountsByEvent();

    interface EventRegistrationCountView {
        Long getEventId();
        String getEventTitle();
        Long getRegistrationCount();
    }

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Event e where e.id = :id")
    Optional<Event> findByIdForUpdate(@Param("id") Long id);
}
