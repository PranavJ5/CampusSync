package com.campussync.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campussync.dto.RegistrationResponse;
import com.campussync.entity.User;
import com.campussync.service.AuthorizationService;
import com.campussync.service.EventRegistrationService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class RegistrationControllerTest {
    @Mock EventRegistrationService registrations;
    @Mock AuthorizationService authorization;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new RegistrationController(registrations, authorization)).build();
    }

    @Test
    void authenticatedStudentReceivesOnlyTheirRegistrations() throws Exception {
        User student = new User();
        student.setId(42L);
        when(authorization.requireRole(any(), eq("STUDENT"))).thenReturn(student);
        when(registrations.myRegistrations(42L)).thenReturn(List.of(new RegistrationResponse(
                7L, 42L, "Campus workshop", "Test Student", null, null,
                LocalDateTime.of(2026, 11, 2, 10, 0), LocalDateTime.of(2026, 11, 1, 10, 0),
                "Main hall", LocalDateTime.of(2026, 10, 20, 9, 0), "CONFIRMED", "NOT_MARKED")));

        mockMvc.perform(get("/api/registrations/me").principal(() -> "student"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].eventId").value(7))
                .andExpect(jsonPath("$[0].userId").value(42))
                .andExpect(jsonPath("$[0].startsAt").exists())
                .andExpect(jsonPath("$[0].registrationDeadline").exists())
                .andExpect(jsonPath("$[0].status").value("CONFIRMED"));

        verify(authorization).requireRole(any(), eq("STUDENT"));
        verify(registrations).myRegistrations(42L);
    }

    @Test
    void authenticatedStudentWithNoRegistrationsReceivesAnEmptyList() throws Exception {
        User student = new User();
        student.setId(43L);
        when(authorization.requireRole(any(), eq("STUDENT"))).thenReturn(student);
        when(registrations.myRegistrations(43L)).thenReturn(List.of());

        mockMvc.perform(get("/api/registrations/me").principal(() -> "student"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        verify(registrations).myRegistrations(43L);
    }
}
