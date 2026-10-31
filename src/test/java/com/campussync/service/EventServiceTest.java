package com.campussync.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.eq;

import com.campussync.entity.Event;
import com.campussync.entity.EventRegistration;
import com.campussync.entity.Committee;
import com.campussync.entity.User;
import com.campussync.dto.EventCancellationRequest;
import com.campussync.dto.EventRequest;
import com.campussync.dto.EventUpdateRequest;
import com.campussync.repository.CommitteeRepository;
import com.campussync.repository.EventRepository;
import com.campussync.repository.EventRegistrationRepository;
import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;
import static org.mockito.ArgumentMatchers.any;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {
    @Mock EventRepository events;
    @Mock CommitteeRepository committees;
    @Mock AuthorizationService authorization;
    @Mock EventRegistrationRepository registrations;
    @Mock NotificationService notifications;

    @Test void managementListRequiresAdministratorAndIncludesDraftEvents() {
        var principal = (java.security.Principal) () -> "admin";
        User authorizedAdmin = new User();
        var role = new com.campussync.entity.Role();
        role.setCode("ADMINISTRATOR");
        authorizedAdmin.setRole(role);
        Committee committee = new Committee(); committee.setId(44L);
        Event draft = draftEvent(6L, committee);
        Event published = draftEvent(7L, committee);
        published.setStatus("PUBLISHED");
        when(events.findAllByOrderByStartsAtAsc()).thenReturn(List.of(draft, published));
        when(authorization.authenticatedUser(principal)).thenReturn(authorizedAdmin);
        when(registrations.countActiveByEventId(6L)).thenReturn(0L);
        when(registrations.countActiveByEventId(7L)).thenReturn(3L);

        var result = new EventService(events, committees, authorization, registrations, notifications)
                .managementList(principal);

        verify(authorization).authenticatedUser(principal);
        assertEquals(List.of("DRAFT", "PUBLISHED"), result.stream().map(response -> response.status()).toList());
        assertEquals(17, result.get(1).remainingSeats());
        assertEquals(3L, result.get(1).registeredCount());
    }

    @Test void coordinatorManagementListContainsOnlyEventsFromAssignedCommittees() {
        var principal = (java.security.Principal) () -> "coordinator";
        User coordinator = new User();
        var role = new com.campussync.entity.Role();
        role.setCode("COMMITTEE_COORDINATOR");
        coordinator.setRole(role);
        Committee assignedCommittee = new Committee();
        assignedCommittee.setId(44L);
        Event assignedEvent = draftEvent(6L, assignedCommittee);
        when(authorization.authenticatedUser(principal)).thenReturn(coordinator);
        when(authorization.coordinatorCommitteeIds(principal)).thenReturn(List.of(44L));
        when(events.findByCommitteeIdInOrderByStartsAtAsc(List.of(44L))).thenReturn(List.of(assignedEvent));
        when(registrations.countActiveByEventId(6L)).thenReturn(0L);

        var result = new EventService(events, committees, authorization, registrations, notifications)
                .managementList(principal);

        assertEquals(List.of(6L), result.stream().map(response -> response.id()).toList());
        verify(events, never()).findAllByOrderByStartsAtAsc();
    }

    @Test void studentCannotListEventsThroughTheManagementEndpoint() {
        var principal = (java.security.Principal) () -> "student";
        User student = new User();
        var role = new com.campussync.entity.Role();
        role.setCode("STUDENT");
        student.setRole(role);
        when(authorization.authenticatedUser(principal)).thenReturn(student);
        when(authorization.coordinatorCommitteeIds(principal)).thenThrow(
                new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.FORBIDDEN));

        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> new EventService(events, committees, authorization, registrations, notifications)
                        .managementList(principal));

        verify(events, never()).findAllByOrderByStartsAtAsc();
        verify(events, never()).findByCommitteeIdInOrderByStartsAtAsc(any());
    }

    @Test void administratorCanCreateEvent() {
        var principal = (java.security.Principal) () -> "admin";
        User administrator = new User();
        Committee committee = activeCommittee(44L);
        LocalDateTime startsAt = LocalDateTime.now().plusDays(2);
        EventRequest request = new EventRequest(44L, "Workshop", null, "LEARNING",
                startsAt, startsAt.plusHours(1), startsAt.minusHours(1), "Hall", 20);
        when(committees.findById(44L)).thenReturn(Optional.of(committee));
        when(authorization.requireAdministratorOrCommitteeCoordinator(principal, 44L)).thenReturn(administrator);
        when(events.save(any(Event.class))).thenAnswer(call -> call.getArgument(0));

        Event created = new EventService(events, committees, authorization, registrations, notifications).create(request, principal);

        assertEquals("DRAFT", created.getStatus());
        assertEquals(administrator, created.getCreatedBy());
        verify(authorization).requireAdministratorOrCommitteeCoordinator(principal, 44L);
    }

    @Test void eventCreationRejectsInactiveCommittee() {
        var principal = (java.security.Principal) () -> "admin";
        Committee committee = new Committee();
        committee.setId(44L);
        committee.setActive(false);
        committee.setStatus("INACTIVE");
        LocalDateTime startsAt = LocalDateTime.now().plusDays(2);
        EventRequest request = new EventRequest(44L, "Workshop", null, "LEARNING",
                startsAt, startsAt.plusHours(1), startsAt.minusHours(1), "Hall", 20);
        when(committees.findById(44L)).thenReturn(Optional.of(committee));

        assertThrows(IllegalStateException.class,
                () -> new EventService(events, committees, authorization, registrations, notifications)
                        .create(request, principal));
        verify(authorization, never()).requireAdministratorOrCommitteeCoordinator(principal, 44L);
    }

    @Test void administratorCanUpdateDraftEvent() {
        var principal = (java.security.Principal) () -> "admin";
        Committee committee = new Committee(); committee.setId(44L);
        Event event = draftEvent(6L, committee);
        User administrator = new User();
        when(events.findByIdForUpdate(6L)).thenReturn(Optional.of(event));
        when(authorization.requireAdministratorOrCommitteeCoordinator(principal, 44L)).thenReturn(administrator);
        when(registrations.countActiveByEventId(6L)).thenReturn(0L);

        Event updated = new EventService(events, committees, authorization, registrations, notifications)
                .update(6L, new EventUpdateRequest(null, "Updated details", null, null, null, null, null, null), principal);

        assertEquals("Updated details", updated.getDescription());
        verify(authorization).requireAdministratorOrCommitteeCoordinator(principal, 44L);
    }

    @Test void administratorCanPublishDraftEvent() {
        var principal = (java.security.Principal) () -> "admin";
        Committee committee = new Committee(); committee.setId(44L);
        Event event = draftEvent(6L, committee);
        User administrator = new User();
        when(events.findByIdForUpdate(6L)).thenReturn(Optional.of(event));
        when(authorization.requireAdministratorOrCommitteeCoordinator(principal, 44L)).thenReturn(administrator);

        Event published = new EventService(events, committees, authorization, registrations, notifications).publish(6L, principal);

        assertEquals("PUBLISHED", published.getStatus());
        verify(authorization).requireAdministratorOrCommitteeCoordinator(principal, 44L);
        verify(notifications).createEventPublished(event, administrator);
    }

    @Test void publicListingIncludesPublishedAndRegistrationClosedEvents() {
        Event published = new Event();
        Event closed = new Event();
        when(events.findByStatusInOrderByStartsAtAsc(List.of("PUBLISHED", "REGISTRATION_CLOSED", "CANCELLED")))
                .thenReturn(List.of(published, closed));
        EventService service = new EventService(events, committees, authorization, registrations, notifications);

        assertEquals(List.of(published, closed), service.published());
        verify(events).findByStatusInOrderByStartsAtAsc(List.of("PUBLISHED", "REGISTRATION_CLOSED", "CANCELLED"));
    }

    @Test void browseFiltersAndReportsRemainingSeats() {
        Committee committee = new Committee(); committee.setId(3L);
        Event event = new Event(); event.setId(7L); event.setCommittee(committee); event.setCapacity(10);
        event.setTitle("Talk"); event.setCategory("TALK"); event.setStatus("PUBLISHED");
        when(events.browsePublicEvents(List.of("PUBLISHED", "REGISTRATION_CLOSED", "CANCELLED"),
                "TALK", 3L, null, null, null)).thenReturn(List.of(event));
        when(registrations.countActiveByEventId(7L)).thenReturn(4L);

        var result = new EventService(events, committees, authorization, registrations, notifications)
                .browse("TALK", 3L, null, null, null);

        assertEquals(6, result.getFirst().remainingSeats());
    }

    @Test void cancellingEventPersistsReasonCancelsRegistrationsAndNotifiesParticipants() {
        Committee committee = new Committee(); committee.setId(44L);
        Event event = new Event(); event.setId(6L); event.setCommittee(committee); event.setStatus("PUBLISHED");
        event.setTitle("Workshop");
        User coordinator = new User(); coordinator.setId(3L);
        User attendee = new User(); attendee.setId(9L); attendee.setActive(true);
        EventRegistration registration = new EventRegistration(); registration.setUser(attendee);
        registration.setRegistrationStatus("CONFIRMED");
        when(events.findByIdForUpdate(6L)).thenReturn(Optional.of(event));
        when(authorization.requireAdministratorOrCommitteeCoordinator(null, 44L)).thenReturn(coordinator);
        when(registrations.findByEventId(6L)).thenReturn(List.of(registration));

        Event result = new EventService(events, committees, authorization, registrations, notifications)
                .cancel(6L, new EventCancellationRequest("Venue unavailable"), null);

        assertEquals("CANCELLED", result.getStatus());
        assertEquals("Venue unavailable", result.getCancellationReason());
        assertEquals("CANCELLED", registration.getRegistrationStatus());
        verify(notifications).createEventCancellation(eq(event), eq(coordinator), eq(List.of(attendee)));
    }

    @Test void cancellationRejectsDraftEvents() {
        Committee committee = new Committee(); committee.setId(44L);
        Event event = new Event(); event.setId(6L); event.setCommittee(committee); event.setStatus("DRAFT");
        when(events.findByIdForUpdate(6L)).thenReturn(Optional.of(event));
        when(authorization.requireAdministratorOrCommitteeCoordinator(null, 44L)).thenReturn(new User());

        assertThrows(IllegalStateException.class, () -> new EventService(events, committees, authorization,
                        registrations, notifications).cancel(6L, new EventCancellationRequest("reason"), null));
        verify(registrations, never()).findByEventId(6L);
    }

    private Event draftEvent(Long id, Committee committee) {
        Event event = new Event();
        event.setId(id);
        event.setCommittee(committee);
        event.setStatus("DRAFT");
        event.setTitle("Workshop");
        event.setCategory("LEARNING");
        event.setStartsAt(LocalDateTime.now().plusDays(2));
        event.setEndsAt(event.getStartsAt().plusHours(1));
        event.setRegistrationDeadline(event.getStartsAt().minusHours(1));
        event.setVenue("Hall");
        event.setCapacity(20);
        return event;
    }

    private Committee activeCommittee(Long id) {
        Committee committee = new Committee();
        committee.setId(id);
        committee.setActive(true);
        committee.setStatus("ACTIVE");
        return committee;
    }
}
