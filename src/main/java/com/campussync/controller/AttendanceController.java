package com.campussync.controller;

import com.campussync.dto.AdministratorAttendanceReportResponse;
import com.campussync.dto.AttendanceRosterEntryResponse;
import com.campussync.dto.AttendanceChangeResponse;
import com.campussync.dto.ClassAttendanceBatchRequest;
import com.campussync.dto.ClassSessionRequest;
import com.campussync.dto.ClassSessionResponse;
import com.campussync.dto.StudentAttendanceResponse;
import com.campussync.dto.SubjectResponse;
import com.campussync.dto.ScheduledClassSessionRequest;
import com.campussync.service.AttendanceService;
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
@RequestMapping("/api/attendance")
public class AttendanceController {
    private final AttendanceService attendance;

    public AttendanceController(AttendanceService attendance) {
        this.attendance = attendance;
    }

    @GetMapping("/faculty/subjects")
    public List<SubjectResponse> facultySubjects(Principal principal) {
        return attendance.facultySubjects(principal);
    }

    @GetMapping("/faculty/subjects/{subjectId}/students")
    public List<AttendanceRosterEntryResponse> facultyRoster(
            @PathVariable Long subjectId, Principal principal) {
        return attendance.facultyRoster(subjectId, principal);
    }

    @GetMapping("/faculty/subjects/{subjectId}/sessions")
    public List<ClassSessionResponse> facultySessions(
            @PathVariable Long subjectId, Principal principal) {
        return attendance.facultySessions(subjectId, principal);
    }

    @PostMapping("/faculty/sessions")
    public ResponseEntity<ClassSessionResponse> createSession(
            @Valid @RequestBody ClassSessionRequest request, Principal principal) {
        ClassSessionResponse response = attendance.createSession(request, principal);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PostMapping("/faculty/timetable/{timetableEntryId}/sessions")
    public ResponseEntity<ClassSessionResponse> createScheduledSession(
            @PathVariable Long timetableEntryId,
            @Valid @RequestBody ScheduledClassSessionRequest request, Principal principal) {
        ClassSessionResponse response = attendance.createScheduledSession(timetableEntryId, request, principal);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/faculty/sessions/{sessionId}/attendance")
    public List<AttendanceRosterEntryResponse> sessionRoster(
            @PathVariable Long sessionId, Principal principal) {
        return attendance.sessionRoster(sessionId, principal);
    }

    @GetMapping("/faculty/sessions/{sessionId}/changes")
    public List<AttendanceChangeResponse> sessionChanges(
            @PathVariable Long sessionId, Principal principal) {
        return attendance.sessionChanges(sessionId, principal);
    }

    @PutMapping("/faculty/sessions/{sessionId}/attendance")
    public List<AttendanceRosterEntryResponse> recordAttendance(
            @PathVariable Long sessionId, @Valid @RequestBody ClassAttendanceBatchRequest request,
            Principal principal) {
        return attendance.recordAttendance(sessionId, request, principal);
    }

    @GetMapping("/student/me")
    public StudentAttendanceResponse studentSummary(Principal principal) {
        return attendance.studentSummary(principal);
    }

    @GetMapping("/admin/report")
    public AdministratorAttendanceReportResponse administratorReport(Principal principal) {
        return attendance.administratorReport(principal);
    }
}
