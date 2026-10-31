package com.campussync.service;

import com.campussync.entity.Event;
import com.campussync.entity.EventRegistration;
import com.campussync.entity.EventRegistrationId;
import com.campussync.entity.User;
import com.campussync.dto.RegistrationResponse;
import com.campussync.dto.AdministratorEventRegistrationResponse;
import com.campussync.dto.AttendanceBatchRequest;
import com.campussync.repository.EventRegistrationRepository;
import com.campussync.repository.EventRepository;
import com.campussync.repository.StudentRepository;
import com.campussync.repository.UserRepository;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.security.Principal;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
public class EventRegistrationService {
    private final EventRepository events;
    private final EventRegistrationRepository registrations;
    private final StudentRepository students;
    private final UserRepository users;
    private final NotificationService notifications;
    private final AuthorizationService authorization;

    public EventRegistrationService(EventRepository events, EventRegistrationRepository registrations,
                                    StudentRepository students, UserRepository users, NotificationService notifications,
                                    AuthorizationService authorization) {
        this.events = events; this.registrations = registrations; this.students = students; this.users = users;
        this.notifications = notifications;
        this.authorization = authorization;
    }

    @Transactional
    public EventRegistration register(Long eventId, Long userId) {
        Event event = events.findByIdForUpdate(eventId).orElseThrow(() -> new NoSuchElementException("Event not found."));
        LocalDateTime now = LocalDateTime.now();
        if (!"PUBLISHED".equalsIgnoreCase(event.getStatus())) throw new IllegalStateException("Event is not open for registration.");
        if (event.getRegistrationDeadline() == null || !now.isBefore(event.getRegistrationDeadline())) {
            throw new IllegalStateException("Registration deadline has passed.");
        }
        User user = users.findById(userId).orElseThrow(() -> new NoSuchElementException("User not found."));
        if (!user.isActive()) throw new IllegalStateException("Account is inactive.");
        if (user.getRole() == null || !"STUDENT".equalsIgnoreCase(user.getRole().getCode()))
            throw new IllegalStateException("Only student accounts may register.");
        students.findByUserId(userId).orElseThrow(() -> new IllegalStateException("Only student accounts may register."));
        EventRegistration existing = registrations.findByEventIdAndUserId(eventId, userId).orElse(null);
        if (existing != null && !"CANCELLED".equalsIgnoreCase(existing.getRegistrationStatus()))
            throw new IllegalStateException("Already registered for this event.");
        if (registrations.countActiveByEventId(eventId) >= event.getCapacity()) throw new IllegalStateException("Event is full.");
        EventRegistration registration = existing == null ? new EventRegistration() : existing;
        registration.setId(new EventRegistrationId(eventId, userId)); registration.setEvent(event); registration.setUser(user);
        registration.setRegistrationStatus("CONFIRMED"); registration.setAttendanceStatus("NOT_MARKED");
        if (existing != null) registration.setRegisteredAt(now);
        EventRegistration saved = registrations.saveAndFlush(registration);
        notifications.createRegistrationConfirmation(event, user);
        if (registrations.countActiveByEventId(eventId) >= event.getCapacity()) event.setStatus("REGISTRATION_CLOSED");
        return saved;
    }

    @Transactional
    public EventRegistration cancel(Long eventId, Long userId) {
        Event event = events.findByIdForUpdate(eventId).orElseThrow(() -> new NoSuchElementException("Event not found."));
        LocalDateTime now = LocalDateTime.now();
        EventRegistration registration = registrations.findByEventIdAndUserId(eventId, userId)
                .orElseThrow(() -> new NoSuchElementException("Registration not found."));
        if (!"CONFIRMED".equalsIgnoreCase(registration.getRegistrationStatus()))
            throw new IllegalStateException("Registration is not active.");
        if (event.getRegistrationDeadline() == null || !now.isBefore(event.getRegistrationDeadline()))
            throw new IllegalStateException("Registration cannot be cancelled after the deadline.");
        registration.setRegistrationStatus("CANCELLED");
        notifications.createRegistrationCancellation(event, registration.getUser());
        if ("REGISTRATION_CLOSED".equalsIgnoreCase(event.getStatus())
                && event.getRegistrationDeadline() != null
                && LocalDateTime.now().isBefore(event.getRegistrationDeadline())) {
            event.setStatus("PUBLISHED");
        }
        return registration;
    }

    @Transactional(readOnly = true)
    public List<RegistrationResponse> myRegistrations(Long userId) {
        return registrations.findByUserIdOrderByRegisteredAtDesc(userId).stream()
                .map(RegistrationResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public RegistrationResponse myRegistration(Long eventId, Long userId) {
        return registrations.findByEventIdAndUserId(eventId, userId)
                .map(RegistrationResponse::from)
                .orElseThrow(() -> new NoSuchElementException("Registration not found."));
    }

    @Transactional(readOnly = true)
    public List<RegistrationResponse> participants(Long eventId, Principal principal) {
        Event event = events.findById(eventId).orElseThrow(() -> new NoSuchElementException("Event not found."));
        authorization.requireAdministratorOrCommitteeCoordinator(principal, event.getCommittee().getId());
        return registrations.findByEventIdAndRegistrationStatusOrderByRegisteredAtAsc(eventId, "CONFIRMED").stream()
                .map(RegistrationResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<AdministratorEventRegistrationResponse> administratorRegistrations(Long eventId, Principal principal) {
        authorization.requireRole(principal, "ADMINISTRATOR");
        events.findById(eventId).orElseThrow(() -> new NoSuchElementException("Event not found."));
        return registrations.findByEventIdOrderByRegisteredAtAsc(eventId).stream()
                .map(AdministratorEventRegistrationResponse::from).toList();
    }

    @Transactional
    public RegistrationResponse markAttendance(Long eventId, Long userId, String attendanceStatus, Principal principal) {
        Event event = events.findByIdForUpdate(eventId).orElseThrow(() -> new NoSuchElementException("Event not found."));
        User actor = authorization.requireAdministratorOrCommitteeCoordinator(principal, event.getCommittee().getId());
        if ("CANCELLED".equalsIgnoreCase(event.getStatus())) throw new IllegalStateException("Attendance cannot be marked for a cancelled event.");
        if ("COMPLETED".equalsIgnoreCase(event.getStatus())
                && (actor.getRole() == null || !"ADMINISTRATOR".equalsIgnoreCase(actor.getRole().getCode())))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only an administrator may correct attendance after completion.");
        if (LocalDate.now().isBefore(event.getStartsAt().toLocalDate()))
            throw new IllegalStateException("Attendance can only be marked on or after the event start date.");
        String normalized = attendanceStatus.toUpperCase(Locale.ROOT);
        if (!List.of("PRESENT", "ABSENT").contains(normalized)) throw new IllegalArgumentException("Attendance status must be PRESENT or ABSENT.");
        EventRegistration registration = registrations.findByEventIdAndUserId(eventId, userId)
                .orElseThrow(() -> new NoSuchElementException("Registration not found."));
        if (!"CONFIRMED".equalsIgnoreCase(registration.getRegistrationStatus()))
            throw new IllegalStateException("Attendance can only be recorded for confirmed registrations.");
        registration.setAttendanceStatus(normalized);
        return RegistrationResponse.from(registration);
    }

    @Transactional
    public List<RegistrationResponse> markAttendanceBatch(Long eventId, AttendanceBatchRequest request, Principal principal) {
        if (request.eventId() != null && !eventId.equals(request.eventId()))
            throw new IllegalArgumentException("Body eventId must match the event in the request path.");
        Event event = events.findByIdForUpdate(eventId).orElseThrow(() -> new NoSuchElementException("Event not found."));
        User actor = authorization.requireAdministratorOrCommitteeCoordinator(principal, event.getCommittee().getId());
        if ("CANCELLED".equalsIgnoreCase(event.getStatus()))
            throw new IllegalStateException("Attendance cannot be marked for a cancelled event.");
        if ("COMPLETED".equalsIgnoreCase(event.getStatus())
                && (actor.getRole() == null || !"ADMINISTRATOR".equalsIgnoreCase(actor.getRole().getCode())))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only an administrator may correct attendance after completion.");
        if (LocalDate.now().isBefore(event.getStartsAt().toLocalDate()))
            throw new IllegalStateException("Attendance can only be marked on or after the event start date.");
        if (request.participants().stream().map(entry -> entry.userId()).distinct().count() != request.participants().size())
            throw new IllegalArgumentException("Each participant may appear only once in a batch.");

        List<RegistrationResponse> updated = new java.util.ArrayList<>();
        for (var entry : request.participants()) {
            String normalized = entry.attendanceStatus().toUpperCase(Locale.ROOT);
            if (!List.of("PRESENT", "ABSENT").contains(normalized))
                throw new IllegalArgumentException("Attendance status must be PRESENT or ABSENT.");
            EventRegistration registration = registrations.findByEventIdAndUserId(eventId, entry.userId())
                    .orElseThrow(() -> new NoSuchElementException("Registration not found for participant " + entry.userId() + "."));
            if (!"CONFIRMED".equalsIgnoreCase(registration.getRegistrationStatus()))
                throw new IllegalStateException("Attendance can only be recorded for confirmed registrations.");
            registration.setAttendanceStatus(normalized);
            updated.add(RegistrationResponse.from(registration));
        }
        return updated;
    }
}
