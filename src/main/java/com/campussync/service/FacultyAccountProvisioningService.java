package com.campussync.service;

import com.campussync.dto.FacultyAccountProvisionRequest;
import com.campussync.dto.FacultyResponse;
import com.campussync.entity.Faculty;
import com.campussync.entity.User;
import com.campussync.repository.DepartmentRepository;
import com.campussync.repository.FacultyRepository;
import com.campussync.repository.RoleRepository;
import com.campussync.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class FacultyAccountProvisioningService {
    private final AuthorizationService authorization;
    private final UserRepository users;
    private final RoleRepository roles;
    private final DepartmentRepository departments;
    private final FacultyRepository faculty;
    private final PasswordEncoder passwordEncoder;

    public FacultyAccountProvisioningService(AuthorizationService authorization, UserRepository users,
                                             RoleRepository roles, DepartmentRepository departments,
                                             FacultyRepository faculty, PasswordEncoder passwordEncoder) {
        this.authorization = authorization;
        this.users = users;
        this.roles = roles;
        this.departments = departments;
        this.faculty = faculty;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public FacultyResponse create(FacultyAccountProvisionRequest request, Principal principal) {
        authorization.requireRole(principal, "ADMINISTRATOR");
        String username = request.username().trim();
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        String employeeNumber = request.employeeNumber().trim();
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Password must be at most 72 UTF-8 bytes.");
        }
        if (users.existsByUsernameIgnoreCase(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username is already registered.");
        }
        if (users.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered.");
        }
        if (faculty.existsByEmployeeNumberIgnoreCase(employeeNumber)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Employee number is already registered.");
        }

        var role = roles.findByCode("FACULTY")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                        "Faculty account provisioning is unavailable because the FACULTY role is not configured."));
        var department = departments.findById(request.departmentId())
                .filter(com.campussync.entity.Department::isActive)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Choose an active department."));

        User user = new User();
        user.setRole(role);
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setActive(true);
        user = users.saveAndFlush(user);

        Faculty profile = new Faculty();
        profile.setUser(user);
        profile.setDepartment(department);
        profile.setEmployeeNumber(employeeNumber);
        profile.setDesignation(request.designation().trim());
        profile.setEmploymentStatus(request.employmentStatus().trim());
        profile.setQualification(request.qualification());
        profile.setContactNumber(request.contactNumber());
        profile.setDateOfJoining(request.dateOfJoining());
        return FacultyResponse.from(faculty.saveAndFlush(profile));
    }
}
