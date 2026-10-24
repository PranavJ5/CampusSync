package com.campussync.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.campussync.dto.FacultyRequest;
import com.campussync.entity.Department;
import com.campussync.entity.Faculty;
import com.campussync.entity.Role;
import com.campussync.entity.User;
import com.campussync.repository.DepartmentRepository;
import com.campussync.repository.FacultyRepository;
import com.campussync.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FacultyServiceTest {
    @Mock FacultyRepository faculty;
    @Mock UserRepository users;
    @Mock DepartmentRepository departments;
    @Mock AuthorizationService authorization;

    @Test void createsFacultyProfileForFacultyAccount() {
        User user = new User(); user.setId(4L); Role role = new Role(); role.setCode("FACULTY"); user.setRole(role);
        Department department = new Department(); department.setId(8L);
        when(users.findById(4L)).thenReturn(Optional.of(user));
        when(departments.findById(8L)).thenReturn(Optional.of(department));
        when(faculty.saveAndFlush(any(Faculty.class))).thenAnswer(call -> {
            Faculty result = call.getArgument(0); result.setId(14L); return result;
        });

        var response = new FacultyService(faculty, users, departments, authorization).create(
                new FacultyRequest(4L, 8L, " EMP-1 ", " Lecturer ", " ACTIVE ", null, null, null), null);

        assertEquals(14L, response.id());
        assertEquals(4L, response.userId());
        assertEquals(8L, response.departmentId());
        assertEquals("EMP-1", response.employeeNumber());
    }
}
