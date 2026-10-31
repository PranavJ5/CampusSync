package com.campussync.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campussync.entity.Event;
import com.campussync.entity.Notification;
import com.campussync.entity.NotificationRecipient;
import com.campussync.entity.User;
import com.campussync.dto.NotificationRequest;
import com.campussync.repository.NotificationRecipientRepository;
import com.campussync.repository.NotificationRepository;
import com.campussync.repository.UserRepository;
import com.campussync.repository.StudentRepository;
import com.campussync.repository.FacultyRepository;
import com.campussync.repository.CommitteeMemberRepository;
import com.campussync.repository.DepartmentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {
    @Mock NotificationRepository notifications;
    @Mock NotificationRecipientRepository recipients;
    @Mock UserRepository users;
    @Mock StudentRepository students;
    @Mock FacultyRepository faculty;
    @Mock CommitteeMemberRepository memberships;
    @Mock AuthorizationService authorization;
    @Mock DepartmentRepository departments;

    @Test void registrationConfirmationAndRecipientAreCreated() {
        NotificationService service = service();
        when(notifications.saveAndFlush(any(Notification.class))).thenAnswer(call -> {
            Notification notification = call.getArgument(0); notification.setId(21L); return notification;
        });
        Event event = new Event(); event.setId(3L); event.setTitle("Open day");
        User student = new User(); student.setId(6L);

        service.createRegistrationConfirmation(event, student);

        ArgumentCaptor<Notification> notification = ArgumentCaptor.forClass(Notification.class);
        verify(notifications).saveAndFlush(notification.capture());
        assertEquals("EVENT_REMINDER", notification.getValue().getCategory());
        assertEquals(event, notification.getValue().getRelatedEvent());
        ArgumentCaptor<java.util.List<NotificationRecipient>> recipientRows = ArgumentCaptor.forClass(java.util.List.class);
        verify(recipients).saveAll(recipientRows.capture());
        NotificationRecipient recipient = recipientRows.getValue().getFirst();
        assertEquals(student, recipient.getUser());
        assertEquals(notification.getValue(), recipient.getNotification());
    }

    @Test void aRecipientCanMarkTheirNotificationRead() {
        Notification notification = new Notification(); notification.setId(10L); notification.setTitle("Title");
        notification.setMessage("Body"); notification.setCategory("CIRCULAR");
        User sender = new User(); sender.setId(1L); sender.setUsername("admin"); notification.setCreatedBy(sender);
        NotificationRecipient row = new NotificationRecipient(); row.setNotification(notification);
        User owner = new User(); owner.setId(2L); owner.setUsername("owner"); row.setUser(owner);
        when(recipients.findByNotificationIdAndUserId(10L, 2L)).thenReturn(Optional.of(row));

        var response = service().markRead(10L, 2L);

        assertEquals(true, response.read());
        verify(recipients).findByNotificationIdAndUserId(10L, 2L);
    }

    @Test void notificationLookupDoesNotRevealRecordsOutsideRecipientSet() {
        when(recipients.findByNotificationIdAndUserId(10L, 99L)).thenReturn(Optional.empty());

        assertThrows(java.util.NoSuchElementException.class, () -> service().markRead(10L, 99L));
    }

    @Test void administratorCircularResolvesRecipientsAndStoresPerRecipientRows() {
        User admin = new User(); admin.setId(1L); admin.setUsername("admin"); admin.setActive(true);
        User first = new User(); first.setId(2L); first.setActive(true);
        User second = new User(); second.setId(3L); second.setActive(true);
        var principal = (java.security.Principal) () -> "admin";
        when(authorization.authenticatedUser(principal)).thenReturn(admin);
        when(authorization.requireRole(principal, "ADMINISTRATOR")).thenReturn(admin);
        when(users.findByActiveTrue()).thenReturn(java.util.List.of(first, second));
        when(notifications.saveAndFlush(any(Notification.class))).thenAnswer(call -> {
            Notification record = call.getArgument(0); record.setId(31L); return record;
        });

        var result = service().publish(new NotificationRequest("Notice", "Body", "CIRCULAR", "ALL", null, null, null, null), principal);

        assertEquals(31L, result.id());
        assertEquals(2, result.recipientCount());
        ArgumentCaptor<java.util.List<NotificationRecipient>> rows = ArgumentCaptor.forClass(java.util.List.class);
        verify(recipients).saveAll(rows.capture());
        assertEquals(java.util.Set.of(2L, 3L), rows.getValue().stream().map(row -> row.getUser().getId()).collect(java.util.stream.Collectors.toSet()));
    }

    private NotificationService service() {
        return new NotificationService(notifications, recipients, users, students, faculty, memberships, authorization, departments);
    }
}
