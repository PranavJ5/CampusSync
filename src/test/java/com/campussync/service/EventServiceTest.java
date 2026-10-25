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
import com.campussync.repository.CommitteeRepository;
import com.campussync.repository.EventRepository;
import com.campussync.repository.EventRegistrationRepository;
import java.util.List;
import java.util.Optional;
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
}
