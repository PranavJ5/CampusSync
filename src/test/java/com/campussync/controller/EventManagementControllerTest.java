package com.campussync.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campussync.dto.EventResponse;
import com.campussync.service.AuthorizationService;
import com.campussync.service.EventService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class EventManagementControllerTest {
    @Mock EventService events;
    @Mock AuthorizationService authorization;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new EventController(events, authorization)).build();
    }

    @Test
    void publishedEventListRequiresStudentRole() throws Exception {
        when(authorization.requireRole(any(), org.mockito.ArgumentMatchers.eq("STUDENT")))
                .thenReturn(new com.campussync.entity.User());
        when(events.browse(any(), any(), any(), any(), any())).thenReturn(List.of(response(1L, "PUBLISHED")));

        mockMvc.perform(get("/api/events").principal(() -> "student"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(authorization).requireRole(any(), org.mockito.ArgumentMatchers.eq("STUDENT"));
    }

    @Test
    void nonStudentCannotBrowsePublishedEventList() throws Exception {
        when(authorization.requireRole(any(), org.mockito.ArgumentMatchers.eq("STUDENT")))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN));

        mockMvc.perform(get("/api/events").principal(() -> "faculty"))
                .andExpect(status().isForbidden());
    }

    @Test
    void eventDetailsRequireStudentRole() throws Exception {
        when(authorization.requireRole(any(), org.mockito.ArgumentMatchers.eq("STUDENT")))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN));

        mockMvc.perform(get("/api/events/1").principal(() -> "coordinator"))
                .andExpect(status().isForbidden());
    }

    @Test
    void managementRouteReturnsDraftsAndPublishedEvents() throws Exception {
        when(events.managementList(any())).thenReturn(List.of(
                response(1L, "DRAFT"), response(2L, "PUBLISHED")));

        mockMvc.perform(get("/api/events/management").principal(() -> "administrator"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].status").value("DRAFT"))
                .andExpect(jsonPath("$[1].status").value("PUBLISHED"));

        verify(events).managementList(any());
    }

    private EventResponse response(Long id, String status) {
        return new EventResponse(id, 3L, "Workshop", null, "LEARNING", null, null, null,
                "Main hall", 20, status, null, null, 1L, null, null, 20, 0L);
    }
}
