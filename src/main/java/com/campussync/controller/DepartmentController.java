package com.campussync.controller;

import com.campussync.dto.DepartmentRequest;
import com.campussync.dto.DepartmentResponse;
import com.campussync.service.AuthorizationService;
import com.campussync.service.DepartmentService;
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
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/departments")
public class DepartmentController {
    private final DepartmentService departments;
    private final AuthorizationService authorization;

    public DepartmentController(DepartmentService departments, AuthorizationService authorization) {
        this.departments = departments;
        this.authorization = authorization;
    }

    @GetMapping
    public List<DepartmentResponse> list(Principal principal) {
        authorization.authenticatedUser(principal);
        return departments.list();
    }

    @GetMapping("/{id}")
    public DepartmentResponse get(@PathVariable Long id, Principal principal) {
        authorization.authenticatedUser(principal);
        return departments.get(id);
    }

    @PostMapping
    public ResponseEntity<DepartmentResponse> create(@Valid @RequestBody DepartmentRequest request, Principal principal) {
        authorization.requireRole(principal, "ADMIN");
        DepartmentResponse response = departments.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/{id}")
    public DepartmentResponse update(@PathVariable Long id, @Valid @RequestBody DepartmentRequest request, Principal principal) {
        authorization.requireRole(principal, "ADMIN");
        return departments.update(id, request);
    }
}
