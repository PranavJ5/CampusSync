package com.campussync.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campussync.dto.RegistrationResponse;
import com.campussync.entity.Event;
import com.campussync.entity.EventRegistration;
import com.campussync.entity.User;
import com.campussync.service.AuthorizationService;
import com.campussync.service.EventRegistrationService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class EventRegistrationControllerTest {
    @Mock EventRegistrationService registrations;
    @Mock AuthorizationService authorization;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                new EventRegistrationController(registrations, authorization)).build();
    }

    @Test
    void cancellationUsesTheEventScopedEndpoint() throws Exception {
        User student = new User();
        student.setId(42L);
        when(authorization.requireRole(any(), eq("STUDENT"))).thenReturn(student);
        when(registrations.cancel(7L, 42L)).thenReturn(registration(7L, student));

        mockMvc.perform(delete("/api/events/7/registrations").principal(() -> "student"))
                .andExpect(status().isOk());

        verify(registrations).cancel(7L, 42L);
    }

    @Test
    void obsoleteUnscopedCancellationPathDoesNotResolve() throws Exception {
        mockMvc.perform(delete("/api/registrations/7").principal(() -> "student"))
                .andExpect(status().isNotFound());
    }

    @Test
    void eventParticipantReportUsesTheExistingScopedServiceAuthorization() throws Exception {
        when(registrations.participants(eq(7L), any())).thenReturn(List.of());

        mockMvc.perform(get("/api/events/7/registrations").principal(() -> "administrator"))
                .andExpect(status().isOk());

        verify(registrations).participants(eq(7L), any());
    }

    @Test
    void ownEventRegistrationStatusRequiresStudentRole() throws Exception {
        User student = new User();
        student.setId(42L);
        when(authorization.requireRole(any(), eq("STUDENT"))).thenReturn(student);
        when(registrations.myRegistration(7L, 42L)).thenReturn(new RegistrationResponse(
                7L, 42L, "Campus workshop", "Test Student", null, null, null, null, null,
                null, "CONFIRMED", "NOT_MARKED"));

        mockMvc.perform(get("/api/events/7/registrations/me").principal(() -> "student"))
                .andExpect(status().isOk());

        verify(registrations).myRegistration(7L, 42L);
    }

    private EventRegistration registration(Long eventId, User student) {
        Event event = new Event();
        event.setId(eventId);
        event.setTitle("Campus workshop");
        EventRegistration registration = new EventRegistration();
        registration.setEvent(event);
        registration.setUser(student);
        registration.setRegistrationStatus("CANCELLED");
        registration.setAttendanceStatus("NOT_MARKED");
        return registration;
    }
}
