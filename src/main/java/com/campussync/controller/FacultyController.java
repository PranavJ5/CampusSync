package com.campussync.controller;

import com.campussync.dto.FacultyRequest;
import com.campussync.dto.FacultyResponse;
import com.campussync.dto.FacultyAccountProvisionRequest;
import com.campussync.service.FacultyAccountProvisioningService;
import com.campussync.service.FacultyService;
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
@RequestMapping("/api/faculty")
public class FacultyController {
    private final FacultyService faculty;
    private final FacultyAccountProvisioningService provisioning;
    public FacultyController(FacultyService faculty, FacultyAccountProvisioningService provisioning) {
        this.faculty = faculty;
        this.provisioning = provisioning;
    }

    @GetMapping
    public List<FacultyResponse> list(Principal principal) { return faculty.list(principal); }

    @PostMapping("/provision")
    public ResponseEntity<FacultyResponse> provision(
            @Valid @RequestBody FacultyAccountProvisionRequest request, Principal principal) {
        FacultyResponse response = provisioning.create(request, principal);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath().path("/api/faculty/{id}")
                .buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{id}")
    public FacultyResponse get(@PathVariable Long id, Principal principal) { return faculty.get(id, principal); }

    @PostMapping
    public ResponseEntity<FacultyResponse> create(@Valid @RequestBody FacultyRequest request, Principal principal) {
        FacultyResponse response = faculty.create(request, principal);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/{id}")
    public FacultyResponse update(@PathVariable Long id, @Valid @RequestBody FacultyRequest request, Principal principal) {
        return faculty.update(id, request, principal);
    }
}
