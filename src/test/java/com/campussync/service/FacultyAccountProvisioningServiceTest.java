package com.campussync.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campussync.dto.FacultyAccountProvisionRequest;
import com.campussync.entity.Department;
import com.campussync.entity.Faculty;
import com.campussync.entity.Role;
import com.campussync.entity.User;
import com.campussync.repository.DepartmentRepository;
import com.campussync.repository.FacultyRepository;
import com.campussync.repository.RoleRepository;
import com.campussync.repository.UserRepository;
import java.security.Principal;
import java.util.Optional;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class FacultyAccountProvisioningServiceTest {
    @Mock AuthorizationService authorization;
    @Mock UserRepository users;
    @Mock RoleRepository roles;
    @Mock DepartmentRepository departments;
    @Mock FacultyRepository faculty;
    @Mock PasswordEncoder passwordEncoder;

    private final Principal administrator = () -> "admin";

    @Test
    void provisionsFacultyAccountWithFixedRoleAndHashedPassword() {
        Role facultyRole = new Role();
        facultyRole.setCode("FACULTY");
        Department department = new Department();
        department.setId(3L);
        department.setActive(true);
        when(roles.findByCode("FACULTY")).thenReturn(Optional.of(facultyRole));
        when(departments.findById(3L)).thenReturn(Optional.of(department));
        when(passwordEncoder.encode("temporary-password")).thenReturn("bcrypt-hash");
        when(users.saveAndFlush(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(10L);
            return saved;
        });
        when(faculty.saveAndFlush(any(Faculty.class))).thenAnswer(invocation -> {
            Faculty saved = invocation.getArgument(0);
            saved.setId(20L);
            return saved;
        });

        var response = service().create(request("temporary-password"), administrator);

        assertEquals(20L, response.id());
        assertEquals(10L, response.userId());
        assertEquals("faculty@example.edu", response.institutionalEmail());
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(users).saveAndFlush(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertEquals("FACULTY", savedUser.getRole().getCode());
        assertNotEquals("temporary-password", savedUser.getPasswordHash());
        verify(passwordEncoder).encode("temporary-password");
    }

    @Test
    void rejectsNonAdministratorBeforeCreatingAccount() {
        when(authorization.requireRole(administrator, "ADMINISTRATOR"))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN));

        assertThrows(ResponseStatusException.class, () -> service().create(request("temporary-password"), administrator));

        verify(users, never()).saveAndFlush(any(User.class));
        verify(faculty, never()).saveAndFlush(any(Faculty.class));
    }

    private FacultyAccountProvisioningService service() {
        return new FacultyAccountProvisioningService(
                authorization, users, roles, departments, faculty, passwordEncoder);
    }

    private FacultyAccountProvisionRequest request(String password) {
        return new FacultyAccountProvisionRequest("faculty1", "FACULTY@example.edu", password,
                "First", "Faculty", 3L, "EMP-1", "Lecturer", "ACTIVE",
                null, null, null);
    }
}
