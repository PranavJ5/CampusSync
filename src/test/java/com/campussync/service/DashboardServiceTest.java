package com.campussync.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campussync.entity.User;
import com.campussync.entity.Committee;
import com.campussync.entity.CommitteeMember;
import com.campussync.repository.CommitteeRepository;
import com.campussync.repository.CommitteeMemberRepository;
import com.campussync.repository.DepartmentRepository;
import com.campussync.repository.EventRegistrationRepository;
import com.campussync.repository.EventRepository;
import com.campussync.repository.FacultyRepository;
import com.campussync.repository.NotificationRecipientRepository;
import com.campussync.repository.UserRepository;
import java.security.Principal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {
    @Mock AuthorizationService authorization;
    @Mock UserRepository users;
    @Mock DepartmentRepository departments;
    @Mock CommitteeRepository committees;
    @Mock EventRepository events;
    @Mock EventRegistrationRepository registrations;
    @Mock NotificationRecipientRepository notifications;
    @Mock CommitteeMemberRepository memberships;
    @Mock FacultyRepository faculty;
    @Mock TimetableService timetables;
    private DashboardService service;

    @BeforeEach
    void setUp() {
        service = new DashboardService(authorization, users, departments, committees,
                events, registrations, notifications, memberships, faculty, timetables);
        lenient().when(timetables.studentDashboard(any())).thenReturn(
                new com.campussync.dto.TimetableDashboardResponse(null, null, List.of(), List.of(), false, false));
    }

    @Test
    void administratorDashboardUsesAggregateCountsAndRequiresAdministratorRole() {
        when(users.countByActiveTrue()).thenReturn(12L);
        when(departments.count()).thenReturn(4L);
        when(committees.count()).thenReturn(3L);
        when(events.count()).thenReturn(8L);
        when(events.countByStatuses(List.of("PUBLISHED", "REGISTRATION_CLOSED"))).thenReturn(5L);
        when(events.countByStatusIgnoreCase("DRAFT")).thenReturn(2L);
        when(registrations.countConfirmed()).thenReturn(17L);
        when(events.findByStatusInAndStartsAtGreaterThanEqualOrderByStartsAtAsc(any(), any())).thenReturn(List.of());
        when(events.registrationCountsByEvent()).thenReturn(List.of());

        var response = service.administrator((Principal) () -> "admin");

        assertEquals(8L, response.events());
        assertEquals(17L, response.registrations());
        assertEquals(0, response.upcomingEvents().size());
        verify(authorization).requireRole(any(), eq("ADMINISTRATOR"));
        verify(registrations).countConfirmed();
        verify(events).registrationCountsByEvent();
    }

    @Test
    void studentDashboardScopesRegistrationAndNotificationQueriesToPrincipalId() {
        User student = new User();
        student.setId(42L);
        when(authorization.requireRole(any(), eq("STUDENT"))).thenReturn(student);
        when(events.findUpcomingPublished(any(), any())).thenReturn(List.of());
        when(registrations.findTop5ByUserIdOrderByRegisteredAtDesc(42L)).thenReturn(List.of());
        when(registrations.countByUserIdAndRegistrationStatusIgnoreCase(42L, "CONFIRMED")).thenReturn(0L);
        when(notifications.findByUserIdOrderByNotificationCreatedAtDesc(eq(42L), any()))
                .thenReturn(new PageImpl<>(List.of()));
        when(notifications.countByUserIdAndReadFalse(42L)).thenReturn(0L);

        var response = service.student((Principal) () -> "student");

        assertEquals(List.of(), response.registrations());
        assertEquals(List.of(), response.notifications());
        assertEquals(0L, response.unreadNotifications());
        assertEquals(0L, response.activeRegistrations());
        assertEquals(List.of(), response.upcomingEvents());
        assertFalse(response.timetableAvailable());
        verify(events).findUpcomingPublished(any(), any());
        verify(registrations).findTop5ByUserIdOrderByRegisteredAtDesc(42L);
        verify(registrations).countByUserIdAndRegistrationStatusIgnoreCase(42L, "CONFIRMED");
        verify(notifications).findByUserIdOrderByNotificationCreatedAtDesc(eq(42L), any());
        verify(notifications).countByUserIdAndReadFalse(42L);
    }

    @Test
    void studentDashboardReturnsPublishedEventsWithAggregatedSeatsAndRegistrationStatus() {
        User student = new User();
        student.setId(42L);
        when(authorization.requireRole(any(), eq("STUDENT"))).thenReturn(student);
        var event = new com.campussync.entity.Event();
        event.setId(101L);
        event.setTitle("Campus Forum");
        event.setCategory("Community");
        event.setStartsAt(java.time.LocalDateTime.now().plusDays(2));
        event.setVenue("Main Hall");
        event.setCapacity(40);
        when(events.findUpcomingPublished(any(), any())).thenReturn(List.of(event));
        when(registrations.findTop5ByUserIdOrderByRegisteredAtDesc(42L)).thenReturn(List.of());
        when(registrations.findByUserIdAndEventIdIn(42L, List.of(101L))).thenReturn(List.of());
        when(registrations.countConfirmedByEventIds(List.of(101L))).thenReturn(List.of(
                new EventRegistrationRepository.EventRegistrationCountView() {
                    @Override public Long getEventId() { return 101L; }
                    @Override public Long getRegistrationCount() { return 13L; }
                }));
        when(registrations.countByUserIdAndRegistrationStatusIgnoreCase(42L, "CONFIRMED")).thenReturn(2L);
        when(notifications.findByUserIdOrderByNotificationCreatedAtDesc(eq(42L), any()))
                .thenReturn(new PageImpl<>(List.of()));
        when(notifications.countByUserIdAndReadFalse(42L)).thenReturn(3L);

        var response = service.student((Principal) () -> "student");

        assertEquals(1, response.upcomingEvents().size());
        assertEquals(27, response.upcomingEvents().get(0).remainingSeats());
        assertEquals(13L, response.upcomingEvents().get(0).registeredCount());
        assertNull(response.upcomingEvents().get(0).registrationStatus());
        assertEquals(2L, response.activeRegistrations());
        assertEquals(3L, response.unreadNotifications());
        verify(registrations).countConfirmedByEventIds(List.of(101L));
    }

    @Test
    void nonStudentCannotAccessStudentDashboardData() {
        Principal principal = () -> "administrator";
        when(authorization.requireRole(principal, "STUDENT"))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN));

        assertThrows(ResponseStatusException.class, () -> service.student(principal));

        verify(events, never()).findUpcomingPublished(any(), any());
        verify(registrations, never()).findTop5ByUserIdOrderByRegisteredAtDesc(any());
        verify(notifications, never()).findByUserIdOrderByNotificationCreatedAtDesc(any(), any());
    }

    @Test
    void coordinatorDashboardUsesOnlyActivelyAssignedCoordinatorCommittees() {
        User coordinator = new User();
        coordinator.setId(24L);
        var role = new com.campussync.entity.Role();
        role.setCode("COMMITTEE_COORDINATOR");
        coordinator.setRole(role);
        when(authorization.authenticatedUser(any())).thenReturn(coordinator);
        Committee committee = new Committee();
        committee.setId(17L);
        CommitteeMember active = new CommitteeMember();
        active.setCommittee(committee);
        active.setMemberRole("COORDINATOR");
        when(memberships.findByUserIdAndMembershipStatusIgnoreCaseOrderByJoinedOnDesc(24L, "ACTIVE"))
                .thenReturn(List.of(active));
        when(events.findByCommitteeIdOrderByStartsAtAsc(17L)).thenReturn(List.of());

        var response = service.coordinator((Principal) () -> "coordinator");

        assertEquals(0L, response.committeeEvents());
        assertEquals(0L, response.participantCount());
        verify(events).findByCommitteeIdOrderByStartsAtAsc(17L);
    }
}
