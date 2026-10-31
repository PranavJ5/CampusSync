package com.campussync.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campussync.dto.StudentAccountRequest;
import com.campussync.entity.Department;
import com.campussync.entity.Role;
import com.campussync.entity.Student;
import com.campussync.entity.User;
import com.campussync.repository.DepartmentRepository;
import com.campussync.repository.RoleRepository;
import com.campussync.repository.StudentRepository;
import com.campussync.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class StudentAccountServiceTest {
    @Mock UserRepository users;
    @Mock RoleRepository roles;
    @Mock DepartmentRepository departments;
    @Mock StudentRepository students;
    private BCryptPasswordEncoder passwordEncoder;
    private StudentAccountService service;
    private Department department;
    private Role studentRole;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        service = new StudentAccountService(users, roles, departments, students, passwordEncoder);
    }

    private void prepareSuccessfulRegistration() {
        department = new Department();
        department.setId(2L);
        department.setActive(true);
        studentRole = new Role();
        studentRole.setCode("STUDENT");
        when(users.existsByUsernameIgnoreCase("campususer")).thenReturn(false);
        when(users.existsByEmailIgnoreCase("campususer@example.com")).thenReturn(false);
        when(students.existsByEnrollmentNumberIgnoreCase("E100")).thenReturn(false);
        when(roles.findByCode("STUDENT")).thenReturn(Optional.of(studentRole));
        when(departments.findById(2L)).thenReturn(Optional.of(department));
        when(users.saveAndFlush(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(10L);
            return user;
        });
    }

    @Test
    void createsStudentProfileAndStoresOnlyBcryptPasswordHash() {
        prepareSuccessfulRegistration();
        when(students.saveAndFlush(any(Student.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.register(request());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(users).saveAndFlush(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertEquals("STUDENT", savedUser.getRole().getCode());
        assertFalse(savedUser.getPasswordHash().equals("secure-password"));
        assertTrue(passwordEncoder.matches("secure-password", savedUser.getPasswordHash()));
        assertEquals("STUDENT", response.role());
        ArgumentCaptor<Student> studentCaptor = ArgumentCaptor.forClass(Student.class);
        verify(students).saveAndFlush(studentCaptor.capture());
        assertEquals(savedUser, studentCaptor.getValue().getUser());
        assertEquals(department, studentCaptor.getValue().getDepartment());
        assertEquals("ACTIVE", studentCaptor.getValue().getAcademicStatus());
    }

    @Test
    void missingStudentRoleReturnsActionableServiceUnavailableWithoutCreatingRecords() {
        when(users.existsByUsernameIgnoreCase("campususer")).thenReturn(false);
        when(users.existsByEmailIgnoreCase("campususer@example.com")).thenReturn(false);
        when(students.existsByEnrollmentNumberIgnoreCase("E100")).thenReturn(false);
        when(roles.findByCode("STUDENT")).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class, () -> service.register(request()));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatusCode());
        assertTrue(exception.getReason().contains("STUDENT role is not configured"));
        verify(users, never()).saveAndFlush(any());
        verify(students, never()).saveAndFlush(any());
    }

    @Test
    void rejectsDuplicateEmailBeforeSavingEitherRecord() {
        when(users.existsByUsernameIgnoreCase("campususer")).thenReturn(false);
        when(users.existsByEmailIgnoreCase("campususer@example.com")).thenReturn(true);

        assertThrows(ResponseStatusException.class, () -> service.register(request()));

        verify(users, never()).saveAndFlush(any());
        verify(students, never()).saveAndFlush(any());
    }

    @Test
    void rejectsDuplicateEnrollmentNumberBeforeSavingEitherRecord() {
        when(users.existsByUsernameIgnoreCase("campususer")).thenReturn(false);
        when(users.existsByEmailIgnoreCase("campususer@example.com")).thenReturn(false);
        when(students.existsByEnrollmentNumberIgnoreCase("E100")).thenReturn(true);

        assertThrows(ResponseStatusException.class, () -> service.register(request()));

        verify(users, never()).saveAndFlush(any());
        verify(students, never()).saveAndFlush(any());
    }

    @Test
    void rejectsDuplicateUsernameBeforeSavingEitherRecord() {
        when(users.existsByUsernameIgnoreCase("campususer")).thenReturn(true);

        assertThrows(ResponseStatusException.class, () -> service.register(request()));

        verify(users, never()).saveAndFlush(any());
        verify(students, never()).saveAndFlush(any());
    }

    private StudentAccountRequest request() {
        return new StudentAccountRequest("campususer", "campususer@example.com", "secure-password",
                "Campus", "User", "E100", 2L, 1, 2026);
    }
}
