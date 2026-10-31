package com.campussync.controller;

import com.campussync.dto.RegistrationResponse;
import com.campussync.dto.AttendanceRequest;
import com.campussync.dto.AttendanceBatchRequest;
import com.campussync.service.EventRegistrationService;
import com.campussync.service.AuthorizationService;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/events/{eventId}/registrations")
public class EventRegistrationController {
    private final EventRegistrationService service;
    private final AuthorizationService authorization;
    public EventRegistrationController(EventRegistrationService service, AuthorizationService authorization) {
        this.service = service; this.authorization = authorization;
    }

    @PostMapping
    public ResponseEntity<RegistrationResponse> register(@PathVariable Long eventId, Principal principal) {
        Long userId = authorization.requireRole(principal, "STUDENT").getId();
        RegistrationResponse result = RegistrationResponse.from(service.register(eventId, userId));
        java.net.URI location = ServletUriComponentsBuilder.fromCurrentRequest().build().toUri();
        return ResponseEntity.created(location).body(result);
    }

    @GetMapping("/me")
    public RegistrationResponse myRegistration(@PathVariable Long eventId, Principal principal) {
        Long userId = authorization.requireRole(principal, "STUDENT").getId();
        return service.myRegistration(eventId, userId);
    }

    @DeleteMapping
    public RegistrationResponse cancel(@PathVariable Long eventId, Principal principal) {
        return RegistrationResponse.from(service.cancel(eventId, authorization.requireRole(principal, "STUDENT").getId()));
    }

    @GetMapping
    public java.util.List<RegistrationResponse> participants(@PathVariable Long eventId, Principal principal) {
        return service.participants(eventId, principal);
    }

    @org.springframework.web.bind.annotation.PatchMapping("/{userId}/attendance")
    public RegistrationResponse markAttendance(@PathVariable Long eventId, @PathVariable Long userId,
                                               @Valid @RequestBody AttendanceRequest request, Principal principal) {
        return service.markAttendance(eventId, userId, request.attendanceStatus(), principal);
    }

    @org.springframework.web.bind.annotation.PatchMapping("/attendance")
    public java.util.List<RegistrationResponse> markAttendanceBatch(@PathVariable Long eventId,
            @Valid @RequestBody AttendanceBatchRequest request, Principal principal) {
        return service.markAttendanceBatch(eventId, request, principal);
    }
}
