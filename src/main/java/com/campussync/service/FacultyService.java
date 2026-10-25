package com.campussync.service;

import com.campussync.dto.FacultyRequest;
import com.campussync.dto.FacultyResponse;
import com.campussync.entity.Faculty;
import com.campussync.entity.User;
import com.campussync.repository.DepartmentRepository;
import com.campussync.repository.FacultyRepository;
import com.campussync.repository.UserRepository;
import java.security.Principal;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class FacultyService {
    private final FacultyRepository faculty;
    private final UserRepository users;
    private final DepartmentRepository departments;
    private final AuthorizationService authorization;

    public FacultyService(FacultyRepository faculty, UserRepository users, DepartmentRepository departments,
                          AuthorizationService authorization) {
        this.faculty = faculty; this.users = users; this.departments = departments; this.authorization = authorization;
    }

    @Transactional(readOnly = true)
    public List<FacultyResponse> list(Principal principal) {
        authorization.requireRole(principal, "ADMIN");
        return faculty.findAll().stream().map(FacultyResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public FacultyResponse get(Long id, Principal principal) {
        User actor = authorization.authenticatedUser(principal);
        Faculty profile = find(id);
        boolean admin = actor.getRole() != null && "ADMIN".equalsIgnoreCase(actor.getRole().getCode());
        boolean self = actor.getRole() != null && "FACULTY".equalsIgnoreCase(actor.getRole().getCode())
                && actor.getId().equals(profile.getUser().getId());
        if (!admin && !self) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Faculty profile access is restricted.");
        return FacultyResponse.from(profile);
    }

    @Transactional
    public FacultyResponse create(FacultyRequest request, Principal principal) {
        authorization.requireRole(principal, "ADMIN");
        Faculty result = new Faculty(); apply(result, request);
        return FacultyResponse.from(faculty.saveAndFlush(result));
    }

    @Transactional
    public FacultyResponse update(Long id, FacultyRequest request, Principal principal) {
        authorization.requireRole(principal, "ADMIN");
        Faculty result = find(id); apply(result, request);
        return FacultyResponse.from(faculty.saveAndFlush(result));
    }

    private Faculty find(Long id) {
        return faculty.findById(id).orElseThrow(() -> new NoSuchElementException("Faculty record not found."));
    }

    private void apply(Faculty target, FacultyRequest request) {
        User user = users.findById(request.userId()).orElseThrow(() -> new NoSuchElementException("User not found."));
        if (user.getRole() == null || !"FACULTY".equalsIgnoreCase(user.getRole().getCode()))
            throw new IllegalArgumentException("Faculty records must reference a FACULTY account.");
        target.setUser(user);
        target.setDepartment(departments.findById(request.departmentId()).orElseThrow(() -> new NoSuchElementException("Department not found.")));
        target.setEmployeeNumber(request.employeeNumber().trim());
        target.setDesignation(request.designation().trim());
        target.setEmploymentStatus(request.employmentStatus().trim());
        target.setQualification(request.qualification()); target.setContactNumber(request.contactNumber());
        target.setDateOfJoining(request.dateOfJoining());
    }
}
