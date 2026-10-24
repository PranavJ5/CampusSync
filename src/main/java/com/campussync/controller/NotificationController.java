package com.campussync.controller;

import com.campussync.dto.NotificationResponse;
import com.campussync.dto.NotificationRequest;
import com.campussync.dto.NotificationPublicationResponse;
import com.campussync.service.AuthorizationService;
import com.campussync.service.NotificationService;
import java.security.Principal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService notifications;
    private final AuthorizationService authorization;

    public NotificationController(NotificationService notifications, AuthorizationService authorization) {
        this.notifications = notifications;
        this.authorization = authorization;
    }

    @GetMapping
    public Page<NotificationResponse> list(Principal principal, @PageableDefault(size = 20) Pageable pageable,
                                           @RequestParam(required = false) String category,
                                           @RequestParam(required = false) Boolean unread) {
        return notifications.forUser(authorization.authenticatedUser(principal).getId(), pageable, category, unread);
    }

    @GetMapping("/unread-count")
    public long unreadCount(Principal principal) {
        return notifications.unreadCount(authorization.authenticatedUser(principal).getId());
    }

    @PatchMapping("/{notificationId}/read")
    public NotificationResponse markRead(@PathVariable Long notificationId, Principal principal) {
        Long userId = authorization.authenticatedUser(principal).getId();
        return notifications.markRead(notificationId, userId);
    }

    @GetMapping("/{notificationId}")
    public NotificationResponse open(@PathVariable Long notificationId, Principal principal) {
        Long userId = authorization.authenticatedUser(principal).getId();
        return notifications.open(notificationId, userId);
    }

    @org.springframework.web.bind.annotation.PatchMapping("/read-all")
    public java.util.Map<String, Integer> markAllRead(Principal principal) {
        int updated = notifications.markAllRead(authorization.authenticatedUser(principal).getId());
        return java.util.Map.of("updated", updated);
    }

    @PostMapping
    public org.springframework.http.ResponseEntity<NotificationPublicationResponse> publish(
            @Valid @RequestBody NotificationRequest request, Principal principal) {
        NotificationPublicationResponse response = notifications.publish(request, principal);
        java.net.URI location = org.springframework.web.servlet.support.ServletUriComponentsBuilder
                .fromCurrentRequest().path("/{id}").buildAndExpand(response.id()).toUri();
        return org.springframework.http.ResponseEntity.created(location).body(response);
    }
}
