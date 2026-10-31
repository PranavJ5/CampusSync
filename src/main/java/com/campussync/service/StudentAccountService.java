package com.campussync.service;

import com.campussync.dto.CurrentUserResponse;
import com.campussync.dto.StudentAccountRequest;
import com.campussync.entity.Role;
import com.campussync.entity.Student;
import com.campussync.entity.User;
import com.campussync.repository.DepartmentRepository;
import com.campussync.repository.RoleRepository;
import com.campussync.repository.StudentRepository;
import com.campussync.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class StudentAccountService {
    private final UserRepository users;
    private final RoleRepository roles;
    private final DepartmentRepository departments;
    private final StudentRepository students;
    private final PasswordEncoder passwordEncoder;

    public StudentAccountService(UserRepository users, RoleRepository roles, DepartmentRepository departments,
                                 StudentRepository students, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.roles = roles;
        this.departments = departments;
        this.students = students;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public CurrentUserResponse register(StudentAccountRequest request) {
        String username = request.username().trim();
        String email = request.email().trim().toLowerCase(java.util.Locale.ROOT);
        String enrollmentNumber = request.enrollmentNumber().trim();
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be at most 72 UTF-8 bytes.");
        }
        if (users.existsByUsernameIgnoreCase(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username is already registered.");
        }
        if (users.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered.");
        }
        if (students.existsByEnrollmentNumberIgnoreCase(enrollmentNumber)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Enrollment number is already registered.");
        }

        Role studentRole = roles.findByCode("STUDENT")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                        "Student account registration is temporarily unavailable because the STUDENT role is not configured. "
                                + "Contact a CampusSync administrator."));
        var department = departments.findById(request.departmentId())
                .filter(com.campussync.entity.Department::isActive)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Choose an active department."));

        User user = new User();
        user.setRole(studentRole);
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setActive(true);
        user = users.saveAndFlush(user);

        Student student = new Student();
        student.setUser(user);
        student.setDepartment(department);
        student.setEnrollmentNumber(enrollmentNumber);
        student.setAdmissionDate(LocalDate.now());
        student.setCurrentSemester(request.currentSemester());
        student.setAdmissionYear(request.admissionYear());
        student.setAcademicStatus("ACTIVE");
        students.saveAndFlush(student);

        return new CurrentUserResponse(user.getId(), user.getUsername(), user.getEmail(), studentRole.getCode());
    }
}
