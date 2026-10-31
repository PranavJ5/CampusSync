package com.campussync.controller;

import com.campussync.dto.RegistrationResponse;
import com.campussync.service.AuthorizationService;
import com.campussync.service.EventRegistrationService;
import java.security.Principal;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/registrations")
public class RegistrationController {
    private final EventRegistrationService registrations;
    private final AuthorizationService authorization;

    public RegistrationController(EventRegistrationService registrations, AuthorizationService authorization) {
        this.registrations = registrations;
        this.authorization = authorization;
    }

    @GetMapping("/me")
    public List<RegistrationResponse> myRegistrations(Principal principal) {
        Long userId = authorization.requireRole(principal, "STUDENT").getId();
        return registrations.myRegistrations(userId);
    }
}
