package com.campussync.service;

import com.campussync.dto.EventRequest;
import com.campussync.dto.EventResponse;
import com.campussync.entity.Committee;
import com.campussync.entity.Event;
import com.campussync.entity.EventRegistration;
import com.campussync.entity.User;
import com.campussync.dto.EventCancellationRequest;
import com.campussync.dto.EventUpdateRequest;
import com.campussync.repository.CommitteeRepository;
import com.campussync.repository.EventRepository;
import com.campussync.repository.EventRegistrationRepository;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scheduling.annotation.Scheduled;

@Service
public class EventService {
    private final EventRepository events;
    private final CommitteeRepository committees;
    private final AuthorizationService authorization;
    private final EventRegistrationRepository registrations;
    private final NotificationService notifications;

    public EventService(EventRepository events, CommitteeRepository committees, AuthorizationService authorization,
                        EventRegistrationRepository registrations, NotificationService notifications) {
        this.events = events;
        this.committees = committees;
        this.authorization = authorization;
        this.registrations = registrations;
        this.notifications = notifications;
    }

    @Transactional(readOnly = true)
    public List<Event> published() { return events.findByStatusInOrderByStartsAtAsc(List.of("PUBLISHED", "REGISTRATION_CLOSED", "CANCELLED")); }

    @Transactional(readOnly = true)
    public List<EventResponse> browse(String category, Long committeeId, Long departmentId,
                                      LocalDateTime from, LocalDateTime to) {
        if (from != null && to != null && to.isBefore(from))
            throw new IllegalArgumentException("to must be on or after from.");
        return events.browsePublicEvents(List.of("PUBLISHED", "REGISTRATION_CLOSED", "CANCELLED"),
                        category == null || category.isBlank() ? null : category.trim(), committeeId,
                        departmentId, from, to).stream()
                .map(event -> EventResponse.from(event, event.getCapacity() - (int) registrations.countActiveByEventId(event.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public EventResponse browseById(Long id) {
        Event event = publishedById(id);
        return EventResponse.from(event, event.getCapacity() - (int) registrations.countActiveByEventId(id));
    }

    @Transactional(readOnly = true)
    public Event publishedById(Long id) {
        Event event = events.findById(id).orElseThrow(() -> new NoSuchElementException("Event not found."));
        if (!List.of("PUBLISHED", "REGISTRATION_CLOSED", "CANCELLED").contains(event.getStatus().toUpperCase()))
            throw new NoSuchElementException("Event not found.");
        return event;
    }

    @Transactional
    public Event create(EventRequest request, Principal principal) {
        LocalDateTime now = LocalDateTime.now();
        if (!request.startsAt().isAfter(now)) throw new IllegalArgumentException("Event must start in the future.");
        if (!request.endsAt().isAfter(request.startsAt())) throw new IllegalArgumentException("Event end must be after its start.");
        if (!request.registrationDeadline().isAfter(now)) throw new IllegalArgumentException("Registration deadline must be in the future.");
        if (request.registrationDeadline().isAfter(request.startsAt()))
            throw new IllegalArgumentException("Registration deadline must be no later than the event start.");
        Committee committee = committees.findById(request.committeeId())
                .orElseThrow(() -> new NoSuchElementException("Committee not found."));
        User actor = authorization.requireAdministratorOrCommitteeCoordinator(principal, committee.getId());
        Event event = new Event();
        event.setCommittee(committee); event.setTitle(request.title()); event.setDescription(request.description());
        event.setCategory(request.category().trim()); event.setCreatedBy(actor);
        event.setStartsAt(request.startsAt()); event.setEndsAt(request.endsAt()); event.setVenue(request.venue());
        event.setRegistrationDeadline(request.registrationDeadline());
        event.setCapacity(request.capacity()); event.setStatus("DRAFT");
        return events.save(event);
    }

    @Transactional
    public Event publish(Long id, Principal principal) {
        Event event = events.findByIdForUpdate(id).orElseThrow(() -> new NoSuchElementException("Event not found."));
        User actor = authorization.requireAdministratorOrCommitteeCoordinator(principal, event.getCommittee().getId());
        if (!"DRAFT".equalsIgnoreCase(event.getStatus())) throw new IllegalStateException("Only draft events can be published.");
        event.setStatus("PUBLISHED");
        notifications.createEventPublished(event, actor);
        return event;
    }

    @Transactional
    public Event update(Long id, EventUpdateRequest request, Principal principal) {
        Event event = events.findByIdForUpdate(id).orElseThrow(() -> new NoSuchElementException("Event not found."));
        User actor = authorization.requireAdministratorOrCommitteeCoordinator(principal, event.getCommittee().getId());
        boolean draft = "DRAFT".equalsIgnoreCase(event.getStatus());
        if (!draft && !"PUBLISHED".equalsIgnoreCase(event.getStatus()))
            throw new IllegalStateException("Only draft or published events can be edited.");
        if (!draft && (request.title() != null || request.category() != null))
            throw new IllegalArgumentException("Title and category can only be changed while the event is a draft.");
        long confirmed = registrations.countActiveByEventId(id);
        if (request.capacity() != null && request.capacity() < confirmed)
            throw new IllegalStateException("Capacity cannot be lower than confirmed registrations.");
        LocalDateTime oldStarts = event.getStartsAt();
        LocalDateTime oldEnds = event.getEndsAt();
        String oldVenue = event.getVenue();
        if (request.title() != null) event.setTitle(request.title().trim());
        if (request.description() != null) event.setDescription(request.description());
        if (request.category() != null) event.setCategory(request.category().trim());
        if (request.startsAt() != null) event.setStartsAt(request.startsAt());
        if (request.endsAt() != null) event.setEndsAt(request.endsAt());
        if (request.registrationDeadline() != null) event.setRegistrationDeadline(request.registrationDeadline());
        if (request.venue() != null) event.setVenue(request.venue().trim());
        if (request.capacity() != null) event.setCapacity(request.capacity());
        if (!event.getStartsAt().isAfter(LocalDateTime.now())) throw new IllegalArgumentException("Event start must be in the future.");
        if (!event.getEndsAt().isAfter(event.getStartsAt())) throw new IllegalArgumentException("Event end must be after its start.");
        if (!event.getRegistrationDeadline().isAfter(LocalDateTime.now())
                || event.getRegistrationDeadline().isAfter(event.getStartsAt()))
            throw new IllegalArgumentException("Registration deadline must be in the future and no later than the start.");
        if (!draft && (!oldStarts.equals(event.getStartsAt()) || !oldEnds.equals(event.getEndsAt()) || !oldVenue.equals(event.getVenue()))) {
            List<User> participants = registrations.findByEventIdAndRegistrationStatusOrderByRegisteredAtAsc(id, "CONFIRMED")
                    .stream().map(EventRegistration::getUser).toList();
            notifications.createEventDetailsChanged(event, actor, participants, "schedule or venue");
        }
        return event;
    }

    @Transactional
    public void deleteDraft(Long id, Principal principal) {
        Event event = events.findByIdForUpdate(id).orElseThrow(() -> new NoSuchElementException("Event not found."));
        authorization.requireAdministratorOrCommitteeCoordinator(principal, event.getCommittee().getId());
        if (!"DRAFT".equalsIgnoreCase(event.getStatus())) throw new IllegalStateException("Only draft events can be deleted.");
        if (!registrations.findByEventId(id).isEmpty()) throw new IllegalStateException("An event with registrations cannot be deleted.");
        events.delete(event);
    }

    @Transactional
    public Event cancel(Long id, EventCancellationRequest request, Principal principal) {
        Event event = events.findByIdForUpdate(id).orElseThrow(() -> new NoSuchElementException("Event not found."));
        User actor = authorization.requireAdministratorOrCommitteeCoordinator(principal, event.getCommittee().getId());
        if (!List.of("PUBLISHED", "REGISTRATION_CLOSED").contains(event.getStatus().toUpperCase()))
            throw new IllegalStateException("Only published or registration-closed events can be cancelled.");
        List<EventRegistration> current = registrations.findByEventId(id);
        List<User> participants = current.stream()
                .filter(r -> "CONFIRMED".equalsIgnoreCase(r.getRegistrationStatus()))
                .map(EventRegistration::getUser).toList();
        event.setStatus("CANCELLED"); event.setCancellationReason(request.reason().trim());
        current.forEach(registration -> registration.setRegistrationStatus("CANCELLED"));
        notifications.createEventCancellation(event, actor, participants);
        return event;
    }

    @Transactional
    public Event complete(Long id, Principal principal) {
        Event event = events.findByIdForUpdate(id).orElseThrow(() -> new NoSuchElementException("Event not found."));
        authorization.requireAdministratorOrCommitteeCoordinator(principal, event.getCommittee().getId());
        if (!"REGISTRATION_CLOSED".equalsIgnoreCase(event.getStatus()))
            throw new IllegalStateException("Only registration-closed events can be completed.");
        if (!event.getEndsAt().isBefore(LocalDateTime.now()))
            throw new IllegalStateException("An event can be completed only after its end time.");
        event.setStatus("COMPLETED");
        return event;
    }

    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void closeExpiredRegistrations() {
        for (Event event : events.findByStatusAndRegistrationDeadlineLessThanEqual("PUBLISHED", LocalDateTime.now())) {
            event.setStatus("REGISTRATION_CLOSED");
        }
    }

    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void sendEventReminders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime windowStart = now.plusHours(24);
        LocalDateTime windowEnd = windowStart.plusMinutes(1);
        List<Event> due = events.findByStatusInAndStartsAtBetween(
                List.of("PUBLISHED", "REGISTRATION_CLOSED"), windowStart, windowEnd);
        for (Event event : due) {
            List<User> participants = registrations.findByEventIdAndRegistrationStatusOrderByRegisteredAtAsc(
                            event.getId(), "CONFIRMED").stream()
                    .map(EventRegistration::getUser).toList();
            notifications.createEventReminder(event, participants);
        }
    }
}
