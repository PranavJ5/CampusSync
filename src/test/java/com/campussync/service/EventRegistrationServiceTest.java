package com.campussync.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campussync.entity.Event;
import com.campussync.entity.EventRegistration;
import com.campussync.entity.Committee;
import com.campussync.entity.Role;
import com.campussync.entity.Student;
import com.campussync.entity.User;
import com.campussync.dto.RegistrationResponse;
import com.campussync.dto.AttendanceBatchRequest;
import com.campussync.dto.AttendanceEntry;
import com.campussync.repository.EventRegistrationRepository;
import com.campussync.repository.EventRepository;
import com.campussync.repository.StudentRepository;
import com.campussync.repository.UserRepository;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.security.Principal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EventRegistrationServiceTest {
    @Mock EventRepository events;
    @Mock EventRegistrationRepository registrations;
    @Mock StudentRepository students;
    @Mock UserRepository users;
    @Mock NotificationService notifications;
    @Mock AuthorizationService authorization;
    EventRegistrationService service;
    Event event;
    User user;

    @BeforeEach void setup() {
        service = new EventRegistrationService(events, registrations, students, users, notifications, authorization);
        event = new Event(); event.setId(2L); event.setStatus("PUBLISHED");
        event.setStartsAt(LocalDateTime.now().plusDays(1)); event.setRegistrationDeadline(LocalDateTime.now().plusHours(12)); event.setCapacity(1);
        user = new User(); user.setId(8L); user.setActive(true);
        Role role = new Role(); role.setCode("STUDENT"); user.setRole(role);
    }

    void startRequest() {
        when(events.findByIdForUpdate(2L)).thenReturn(Optional.of(event));
    }

    void addUser() {
        when(users.findById(8L)).thenReturn(Optional.of(user));
    }

    void eligibleStudent() {
        startRequest();
        addUser();
        Student student = new Student(); student.setAcademicStatus("ACTIVE");
        when(students.findByUserId(8L)).thenReturn(Optional.of(student));
        when(registrations.findByEventIdAndUserId(2L, 8L)).thenReturn(Optional.empty());
        when(registrations.countActiveByEventId(2L)).thenReturn(0L);
        when(registrations.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test void registersEligibleStudentAndReturnsRegistration() {
        eligibleStudent();
        EventRegistration result = service.register(2L, 8L);
        assertEquals("CONFIRMED", result.getRegistrationStatus());
        assertEquals("NOT_MARKED", result.getAttendanceStatus());
        verify(registrations).saveAndFlush(any(EventRegistration.class));
        verify(notifications).createRegistrationConfirmation(event, user);
    }

    @Test void rejectsUnpublishedEvent() {
        event.setStatus("DRAFT"); startRequest();
        assertThrows(IllegalStateException.class, () -> service.register(2L, 8L));
    }

    @Test void rejectsDuplicateRegistration() {
        startRequest(); addUser();
        Student student = new Student(); student.setAcademicStatus("ACTIVE");
        when(students.findByUserId(8L)).thenReturn(Optional.of(student));
        when(registrations.findByEventIdAndUserId(2L, 8L)).thenReturn(Optional.of(new EventRegistration()));
        assertThrows(IllegalStateException.class, () -> service.register(2L, 8L));
    }

    @Test void rejectsFullEvent() {
        startRequest();
        addUser();
        Student student = new Student(); student.setAcademicStatus("ACTIVE");
        when(students.findByUserId(8L)).thenReturn(Optional.of(student));
        when(registrations.findByEventIdAndUserId(2L, 8L)).thenReturn(Optional.empty());
        when(registrations.countActiveByEventId(2L)).thenReturn(1L);
        assertThrows(IllegalStateException.class, () -> service.register(2L, 8L));
    }

    @Test void rejectsFacultyAccount() {
        user.getRole().setCode("FACULTY");
        startRequest();
        addUser();
        assertThrows(IllegalStateException.class, () -> service.register(2L, 8L));
    }

    @Test void rejectsRegistrationAfterDeadline() {
        event.setRegistrationDeadline(LocalDateTime.now().minusSeconds(1)); startRequest();
        assertThrows(IllegalStateException.class, () -> service.register(2L, 8L));
    }

    @Test void cancellationReleasesSeatAndCreatesOwnNotificationBeforeDeadline() {
        startRequest();
        EventRegistration registration = new EventRegistration(); registration.setEvent(event); registration.setUser(user);
        registration.setRegistrationStatus("CONFIRMED");
        when(registrations.findByEventIdAndUserId(2L, 8L)).thenReturn(Optional.of(registration));

        EventRegistration result = service.cancel(2L, 8L);

        assertEquals("CANCELLED", result.getRegistrationStatus());
        verify(notifications).createRegistrationCancellation(event, user);
    }

    @Test void cancellationDoesNotReopenClosedEventIfDeadlinePassesDuringProcessing() {
        event.setStatus("REGISTRATION_CLOSED");
        startRequest();
        EventRegistration registration = new EventRegistration(); registration.setEvent(event); registration.setUser(user);
        registration.setRegistrationStatus("CONFIRMED");
        when(registrations.findByEventIdAndUserId(2L, 8L)).thenReturn(Optional.of(registration));
        doAnswer(invocation -> {
            event.setRegistrationDeadline(LocalDateTime.now().minusSeconds(1));
            return null;
        }).when(notifications).createRegistrationCancellation(event, user);

        EventRegistration result = service.cancel(2L, 8L);

        assertEquals("CANCELLED", result.getRegistrationStatus());
        assertEquals("REGISTRATION_CLOSED", event.getStatus());
        verify(notifications).createRegistrationCancellation(event, user);
    }

    @Test void eligibilityDoesNotDependOnAnUnspecifiedAcademicStatusValue() {
        startRequest(); addUser();
        when(students.findByUserId(8L)).thenReturn(Optional.of(new Student()));
        when(registrations.findByEventIdAndUserId(2L, 8L)).thenReturn(Optional.empty());
        when(registrations.countActiveByEventId(2L)).thenReturn(0L);
        when(registrations.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        EventRegistration result = service.register(2L, 8L);

        assertEquals("CONFIRMED", result.getRegistrationStatus());
    }

    @Test void coordinatorCanMarkConfirmedParticipantAttendanceOnEventDate() {
        Committee committee = new Committee(); committee.setId(4L); event.setCommittee(committee);
        event.setStartsAt(LocalDate.now().atTime(23, 0));
        EventRegistration registration = new EventRegistration();
        registration.setEvent(event); registration.setUser(user); registration.setRegistrationStatus("CONFIRMED");
        when(events.findByIdForUpdate(2L)).thenReturn(Optional.of(event));
        when(registrations.findByEventIdAndUserId(2L, 8L)).thenReturn(Optional.of(registration));

        RegistrationResponse result = service.markAttendance(2L, 8L, "present", new Principal() {
            @Override public String getName() { return "coordinator"; }
        });

        assertEquals("PRESENT", result.attendanceStatus());
        verify(authorization).requireAdministratorOrCommitteeCoordinator(org.mockito.ArgumentMatchers.any(Principal.class), org.mockito.ArgumentMatchers.eq(4L));
    }

    @Test void batchAttendanceUpdatesConfirmedParticipantsInOneEventScopedOperation() {
        Committee committee = new Committee(); committee.setId(4L); event.setCommittee(committee);
        event.setStartsAt(LocalDate.now().atStartOfDay());
        User admin = new User(); Role adminRole = new Role(); adminRole.setCode("ADMIN"); admin.setRole(adminRole);
        when(events.findByIdForUpdate(2L)).thenReturn(Optional.of(event));
        when(authorization.requireAdministratorOrCommitteeCoordinator(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(4L)))
                .thenReturn(admin);
        EventRegistration first = new EventRegistration(); first.setEvent(event); first.setUser(user); first.setRegistrationStatus("CONFIRMED");
        User secondUser = new User(); secondUser.setId(10L);
        EventRegistration second = new EventRegistration(); second.setEvent(event); second.setUser(secondUser); second.setRegistrationStatus("CONFIRMED");
        when(registrations.findByEventIdAndUserId(2L, 8L)).thenReturn(Optional.of(first));
        when(registrations.findByEventIdAndUserId(2L, 10L)).thenReturn(Optional.of(second));
        var request = new AttendanceBatchRequest(2L, java.util.List.of(
                new AttendanceEntry(8L, "PRESENT"), new AttendanceEntry(10L, "ABSENT")));

        var result = service.markAttendanceBatch(2L, request, () -> "admin");

        assertEquals(2, result.size());
        assertEquals("PRESENT", first.getAttendanceStatus());
        assertEquals("ABSENT", second.getAttendanceStatus());
    }

    @Test void batchAttendanceRejectsDuplicateParticipantIds() {
        Committee committee = new Committee(); committee.setId(4L); event.setCommittee(committee);
        event.setStartsAt(LocalDate.now().atStartOfDay());
        User admin = new User(); Role adminRole = new Role(); adminRole.setCode("ADMIN"); admin.setRole(adminRole);
        when(events.findByIdForUpdate(2L)).thenReturn(Optional.of(event));
        when(authorization.requireAdministratorOrCommitteeCoordinator(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(4L)))
                .thenReturn(admin);
        assertThrows(IllegalArgumentException.class, () -> service.markAttendanceBatch(2L,
                new AttendanceBatchRequest(2L, java.util.List.of(
                        new AttendanceEntry(8L, "PRESENT"), new AttendanceEntry(8L, "ABSENT"))), null));
    }
}
