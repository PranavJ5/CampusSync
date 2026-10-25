package com.campussync.controller;

import com.campussync.dto.StudentRequest;
import com.campussync.dto.StudentResponse;
import com.campussync.dto.StudentContactRequest;
import com.campussync.service.StudentService;
import jakarta.validation.Valid;
import java.net.URI;
import java.security.Principal;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.RequestParam;
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
@RequestMapping("/api/students")
public class StudentController {
    private final StudentService students;
    public StudentController(StudentService students) { this.students = students; }

    @GetMapping
    public Page<StudentResponse> list(Principal principal, @PageableDefault(size = 25) Pageable pageable,
            @RequestParam(required = false) Long departmentId, @RequestParam(required = false) Integer semester,
            @RequestParam(required = false) Integer admissionYear, @RequestParam(required = false) String search) {
        return students.list(principal, pageable, departmentId, semester, admissionYear, search);
    }

    @GetMapping("/me")
    public StudentResponse me(Principal principal) { return students.me(principal); }

    @org.springframework.web.bind.annotation.PatchMapping("/me/contact")
    public StudentResponse updateOwnContact(@Valid @RequestBody StudentContactRequest request, Principal principal) {
        return students.updateOwnContact(request, principal);
    }

    @GetMapping("/{id}")
    public StudentResponse get(@PathVariable Long id, Principal principal) { return students.get(id, principal); }

    @PostMapping
    public ResponseEntity<StudentResponse> create(@Valid @RequestBody StudentRequest request, Principal principal) {
        StudentResponse response = students.create(request, principal);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/{id}")
    public StudentResponse update(@PathVariable Long id, @Valid @RequestBody StudentRequest request, Principal principal) {
        return students.update(id, request, principal);
    }
}
