package com.campussync.controller;

import com.campussync.dto.TimetableDashboardResponse;
import com.campussync.dto.TimetableEntryRequest;
import com.campussync.dto.TimetableEntryResponse;
import com.campussync.dto.SubjectResponse;
import com.campussync.service.TimetableService;
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
@RequestMapping("/api/timetable")
public class TimetableController {
    private final TimetableService timetables;

    public TimetableController(TimetableService timetables) {
        this.timetables = timetables;
    }

    @GetMapping("/student/me")
    public TimetableDashboardResponse studentDashboard(Principal principal) {
        return timetables.studentDashboard(principal);
    }

    @GetMapping("/faculty/me")
    public TimetableDashboardResponse facultyDashboard(Principal principal) {
        return timetables.facultyDashboard(principal);
    }

    @GetMapping("/faculty/subjects")
    public List<SubjectResponse> facultySubjects(Principal principal) {
        return timetables.facultySubjects(principal);
    }

    @PostMapping("/faculty")
    public ResponseEntity<TimetableEntryResponse> createFacultyLecture(
            @Valid @RequestBody TimetableEntryRequest request, Principal principal) {
        TimetableEntryResponse response = timetables.createFacultyLecture(request, principal);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/management")
    public List<TimetableEntryResponse> managementList(Principal principal) {
        return timetables.managementList(principal);
    }

    @PostMapping("/management")
    public ResponseEntity<TimetableEntryResponse> create(
            @Valid @RequestBody TimetableEntryRequest request, Principal principal) {
        TimetableEntryResponse response = timetables.create(request, principal);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/management/{id}")
    public TimetableEntryResponse update(@PathVariable Long id,
            @Valid @RequestBody TimetableEntryRequest request, Principal principal) {
        return timetables.update(id, request, principal);
    }
}
