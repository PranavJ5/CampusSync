package com.campussync.controller;

import com.campussync.dto.SubjectRequest;
import com.campussync.dto.SubjectResponse;
import com.campussync.service.AuthorizationService;
import com.campussync.service.SubjectService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/subjects")
public class SubjectController {
    private final SubjectService subjects;
    private final AuthorizationService authorization;
    public SubjectController(SubjectService subjects, AuthorizationService authorization) {
        this.subjects = subjects; this.authorization = authorization;
    }

    @GetMapping
    public List<SubjectResponse> list(@RequestParam(required = false) Long departmentId, Principal principal) {
        authorization.authenticatedUser(principal);
        return departmentId == null ? subjects.list() : subjects.listByDepartment(departmentId);
    }

    @GetMapping("/{id}")
    public SubjectResponse get(@PathVariable Long id, Principal principal) {
        authorization.authenticatedUser(principal);
        return subjects.get(id);
    }

    @PostMapping
    public ResponseEntity<SubjectResponse> create(@Valid @RequestBody SubjectRequest request, Principal principal) {
        authorization.requireRole(principal, "ADMIN");
        SubjectResponse response = subjects.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/{id}")
    public SubjectResponse update(@PathVariable Long id, @Valid @RequestBody SubjectRequest request, Principal principal) {
        authorization.requireRole(principal, "ADMIN");
        return subjects.update(id, request);
    }
}
