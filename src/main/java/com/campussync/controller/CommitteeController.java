package com.campussync.controller;

import com.campussync.dto.CommitteeRequest;
import com.campussync.dto.CommitteeResponse;
import com.campussync.dto.CommitteeMemberRequest;
import com.campussync.dto.CommitteeMemberResponse;
import com.campussync.service.AuthorizationService;
import com.campussync.service.CommitteeService;
import jakarta.validation.Valid;
import java.net.URI;
import java.security.Principal;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/committees")
public class CommitteeController {
    private final CommitteeService committees;
    private final AuthorizationService authorization;
    public CommitteeController(CommitteeService committees, AuthorizationService authorization) {
        this.committees = committees; this.authorization = authorization;
    }

    @GetMapping
    public List<CommitteeResponse> list(Principal principal) {
        authorization.authenticatedUser(principal);
        return committees.list();
    }

    @GetMapping("/manageable")
    public List<CommitteeResponse> manageable(Principal principal) {
        return committees.manageable(principal);
    }

    @GetMapping("/{id}")
    public CommitteeResponse get(@PathVariable Long id, Principal principal) {
        authorization.authenticatedUser(principal);
        return committees.get(id);
    }

    @GetMapping("/{id}/members")
    public List<CommitteeMemberResponse> members(@PathVariable Long id, Principal principal,
            @RequestParam(required = false) Long departmentId, @RequestParam(required = false) Integer semester) {
        return committees.members(id, principal, departmentId, semester);
    }

    @GetMapping("/{id}/members/history")
    public List<CommitteeMemberResponse> memberHistory(@PathVariable Long id, Principal principal) {
        return committees.memberHistory(id, principal);
    }

    @GetMapping("/memberships/me")
    public List<CommitteeMemberResponse> myMemberships(Principal principal) {
        return committees.myMemberships(principal);
    }

    @PostMapping("/{id}/members")
    public ResponseEntity<CommitteeMemberResponse> addMember(@PathVariable Long id,
            @Valid @RequestBody CommitteeMemberRequest request, Principal principal) {
        CommitteeMemberResponse response = committees.addMember(id, request, principal);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{userId}")
                .buildAndExpand(response.userId()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/{id}/members/{userId}")
    public ResponseEntity<Void> removeMember(@PathVariable Long id, @PathVariable Long userId, Principal principal) {
        committees.removeMember(id, userId, principal);
        return ResponseEntity.noContent().build();
    }

    @PostMapping
    public ResponseEntity<CommitteeResponse> create(@Valid @RequestBody CommitteeRequest request, Principal principal) {
        authorization.requireRole(principal, "ADMINISTRATOR");
        CommitteeResponse response = committees.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/{id}")
    public CommitteeResponse update(@PathVariable Long id, @Valid @RequestBody CommitteeRequest request, Principal principal) {
        // This request replaces coordinator assignment, department, status and active state too.
        // Keep full committee-profile administration with admins; coordinators manage their scoped events/members.
        authorization.requireRole(principal, "ADMINISTRATOR");
        return committees.update(id, request);
    }
}
