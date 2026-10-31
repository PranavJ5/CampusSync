package com.campussync.controller;

import com.campussync.dto.AdministratorEventRegistrationResponse;
import com.campussync.service.EventRegistrationService;
import java.security.Principal;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/events/{eventId}/registrations")
public class AdministratorEventRegistrationController {
    private final EventRegistrationService registrations;

    public AdministratorEventRegistrationController(EventRegistrationService registrations) {
        this.registrations = registrations;
    }

    @GetMapping
    public List<AdministratorEventRegistrationResponse> registrations(
            @PathVariable Long eventId, Principal principal) {
        return registrations.administratorRegistrations(eventId, principal);
    }
}
