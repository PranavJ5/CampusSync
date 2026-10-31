package com.campussync.service;

import com.campussync.dto.AdminDashboardResponse;
import com.campussync.dto.CoordinatorDashboardResponse;
import com.campussync.dto.EventResponse;
import com.campussync.dto.NotificationResponse;
import com.campussync.dto.RegistrationResponse;
import com.campussync.dto.StudentDashboardEventResponse;
import com.campussync.dto.StudentDashboardResponse;
import com.campussync.repository.CommitteeRepository;
import com.campussync.repository.DepartmentRepository;
import com.campussync.repository.EventRegistrationRepository;
import com.campussync.repository.EventRepository;
import com.campussync.repository.CommitteeMemberRepository;
import com.campussync.repository.FacultyRepository;
import com.campussync.repository.NotificationRecipientRepository;
import com.campussync.repository.UserRepository;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {
    private static final List<String> PUBLISHED_STATUSES = List.of("PUBLISHED", "REGISTRATION_CLOSED");

    private final AuthorizationService authorization;
    private final UserRepository users;
    private final DepartmentRepository departments;
    private final CommitteeRepository committees;
    private final EventRepository events;
    private final EventRegistrationRepository registrations;
    private final NotificationRecipientRepository notificationRecipients;
    private final CommitteeMemberRepository memberships;
    private final FacultyRepository faculty;
    private final TimetableService timetables;

    public DashboardService(AuthorizationService authorization, UserRepository users,
                            DepartmentRepository departments, CommitteeRepository committees,
                            EventRepository events, EventRegistrationRepository registrations,
                            NotificationRecipientRepository notificationRecipients,
                            CommitteeMemberRepository memberships, FacultyRepository faculty,
                            TimetableService timetables) {
        this.authorization = authorization;
        this.users = users;
        this.departments = departments;
        this.committees = committees;
        this.events = events;
        this.registrations = registrations;
        this.notificationRecipients = notificationRecipients;
        this.memberships = memberships;
        this.faculty = faculty;
        this.timetables = timetables;
    }

    @Transactional(readOnly = true)
    public AdminDashboardResponse administrator(Principal principal) {
        authorization.requireRole(principal, "ADMINISTRATOR");
        List<EventResponse> upcoming = events.findByStatusInAndStartsAtGreaterThanEqualOrderByStartsAtAsc(
                        PUBLISHED_STATUSES, LocalDateTime.now()).stream()
                .limit(10)
                .map(this::eventSummary)
                .toList();
        List<AdminDashboardResponse.EventRegistrationSummary> counts = events.registrationCountsByEvent()
                .stream()
                .map(row -> new AdminDashboardResponse.EventRegistrationSummary(
                        row.getEventId(), row.getEventTitle(), row.getRegistrationCount()))
                .toList();
        return new AdminDashboardResponse(users.countByActiveTrue(), departments.count(), committees.count(),
                events.count(), events.countByStatuses(PUBLISHED_STATUSES),
                events.countByStatusIgnoreCase("DRAFT"), registrations.countConfirmed(), upcoming, counts);
    }

    @Transactional(readOnly = true)
    public StudentDashboardResponse student(Principal principal) {
        var student = authorization.requireRole(principal, "STUDENT");
        Long userId = student.getId();
        List<com.campussync.entity.Event> upcomingEvents = events.findUpcomingPublished(
                LocalDateTime.now(), PageRequest.of(0, 10));
        List<com.campussync.entity.EventRegistration> myRegistrationEntities =
                registrations.findTop5ByUserIdOrderByRegisteredAtDesc(userId);
        List<RegistrationResponse> myRegistrations = myRegistrationEntities.stream()
                .map(RegistrationResponse::from).toList();
        java.util.Map<Long, String> registrationStatuses = upcomingEvents.isEmpty()
                ? java.util.Map.of()
                : registrations.findByUserIdAndEventIdIn(userId, upcomingEvents.stream()
                                .map(com.campussync.entity.Event::getId).toList())
                        .stream().collect(java.util.stream.Collectors.toMap(
                                registration -> registration.getEvent().getId(),
                                com.campussync.entity.EventRegistration::getRegistrationStatus));
        java.util.Map<Long, Long> confirmedCounts = upcomingEvents.isEmpty()
                ? java.util.Map.of()
                : registrations.countConfirmedByEventIds(upcomingEvents.stream()
                                .map(com.campussync.entity.Event::getId).toList())
                        .stream().collect(java.util.stream.Collectors.toMap(
                                EventRegistrationRepository.EventRegistrationCountView::getEventId,
                                EventRegistrationRepository.EventRegistrationCountView::getRegistrationCount));
        List<StudentDashboardEventResponse> upcoming = upcomingEvents.stream()
                .map(event -> StudentDashboardEventResponse.from(event,
                        confirmedCounts.getOrDefault(event.getId(), 0L),
                        registrationStatuses.get(event.getId())))
                .toList();
        var notificationPage = notificationRecipients.findByUserIdOrderByNotificationCreatedAtDesc(
                userId, PageRequest.of(0, 5));
        List<NotificationResponse> notifications = notificationPage.getContent().stream()
                .map(NotificationResponse::from).toList();
        return new StudentDashboardResponse(upcoming, myRegistrations, notifications,
                notificationRecipients.countByUserIdAndReadFalse(userId),
                registrations.countByUserIdAndRegistrationStatusIgnoreCase(userId, "CONFIRMED"),
                timetables.studentDashboard(principal).timetableAvailable());
    }

    @Transactional(readOnly = true)
    public CoordinatorDashboardResponse coordinator(Principal principal) {
        var user = authorization.authenticatedUser(principal);
        String role = user.getRole() == null ? "" : user.getRole().getCode();
        List<com.campussync.entity.Event> scopedEvents;
        if ("COMMITTEE_COORDINATOR".equalsIgnoreCase(role)) {
            List<Long> committeeIds = memberships
                    .findByUserIdAndMembershipStatusIgnoreCaseOrderByJoinedOnDesc(user.getId(), "ACTIVE")
                    .stream()
                    .filter(member -> "COORDINATOR".equalsIgnoreCase(member.getMemberRole()))
                    .map(member -> member.getCommittee().getId())
                    .distinct()
                    .toList();
            scopedEvents = committeeIds.stream()
                    .flatMap(id -> events.findByCommitteeIdOrderByStartsAtAsc(id).stream())
                    .toList();
        } else if ("FACULTY".equalsIgnoreCase(role)) {
            var profile = faculty.findByUserId(user.getId())
                    .orElseThrow(() -> new java.util.NoSuchElementException("Faculty profile not found."));
            scopedEvents = events.browsePublicEvents(PUBLISHED_STATUSES, null, null,
                    profile.getDepartment().getId(), LocalDateTime.now(), null);
        } else {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN, "Faculty or committee coordinator role is required.");
        }

        long draftCount = scopedEvents.stream()
                .filter(event -> "DRAFT".equalsIgnoreCase(event.getStatus())).count();
        long participantCount = scopedEvents.stream()
                .mapToLong(event -> registrations.countActiveByEventId(event.getId())).sum();
        List<EventResponse> upcoming = scopedEvents.stream()
                .filter(event -> PUBLISHED_STATUSES.stream().anyMatch(status -> status.equalsIgnoreCase(event.getStatus())))
                .filter(event -> event.getStartsAt() != null && !event.getStartsAt().isBefore(LocalDateTime.now()))
                .sorted(java.util.Comparator.comparing(com.campussync.entity.Event::getStartsAt))
                .limit(10)
                .map(this::eventSummary)
                .toList();
        return new CoordinatorDashboardResponse(scopedEvents.size(), draftCount, participantCount, upcoming);
    }

    private EventResponse eventSummary(com.campussync.entity.Event event) {
        long confirmed = registrations.countActiveByEventId(event.getId());
        return EventResponse.from(event, event.getCapacity() - (int) confirmed, confirmed);
    }
}
