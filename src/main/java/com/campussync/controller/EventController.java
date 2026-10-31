package com.campussync.controller;

import com.campussync.dto.EventRequest;
import com.campussync.dto.EventResponse;
import com.campussync.dto.EventCancellationRequest;
import com.campussync.dto.EventUpdateRequest;
import com.campussync.service.AuthorizationService;
import com.campussync.service.EventService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.time.LocalDateTime;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import java.security.Principal;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/events")
public class EventController {
    private final EventService service;
    private final AuthorizationService authorization;
    public EventController(EventService service, AuthorizationService authorization) {
        this.service = service;
        this.authorization = authorization;
    }

    @GetMapping
    public List<EventResponse> list(@RequestParam(required = false) String category,
            @RequestParam(required = false) Long committeeId, @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) LocalDateTime from, @RequestParam(required = false) LocalDateTime to,
            Principal principal) {
        authorization.requireRole(principal, "STUDENT");
        return service.browse(category, committeeId, departmentId, from, to);
    }

    @GetMapping("/management")
    public List<EventResponse> managementList(Principal principal) {
        return service.managementList(principal);
    }

    @GetMapping("/{id}")
    public EventResponse get(@PathVariable Long id, Principal principal) {
        authorization.requireRole(principal, "STUDENT");
        return service.browseById(id);
    }

    @PostMapping
    public ResponseEntity<EventResponse> create(@Valid @RequestBody EventRequest request, Principal principal) {
        EventResponse created = EventResponse.from(service.create(request, principal));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @org.springframework.web.bind.annotation.PostMapping("/{id}/publish")
    public EventResponse publish(@PathVariable Long id, Principal principal) {
        return EventResponse.from(service.publish(id, principal));
    }

    @org.springframework.web.bind.annotation.PostMapping("/{id}/cancel")
    public EventResponse cancel(@PathVariable Long id, @Valid @RequestBody EventCancellationRequest request, Principal principal) {
        return EventResponse.from(service.cancel(id, request, principal));
    }

    @org.springframework.web.bind.annotation.PostMapping("/{id}/complete")
    public EventResponse complete(@PathVariable Long id, Principal principal) {
        return EventResponse.from(service.complete(id, principal));
    }

    @org.springframework.web.bind.annotation.PutMapping("/{id}")
    public EventResponse update(@PathVariable Long id, @Valid @RequestBody EventUpdateRequest request, Principal principal) {
        return EventResponse.from(service.update(id, request, principal));
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/{id}")
    public org.springframework.http.ResponseEntity<Void> deleteDraft(@PathVariable Long id, Principal principal) {
        service.deleteDraft(id, principal);
        return org.springframework.http.ResponseEntity.noContent().build();
    }
}
