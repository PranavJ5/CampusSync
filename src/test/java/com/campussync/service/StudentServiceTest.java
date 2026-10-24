package com.campussync.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.campussync.dto.StudentRequest;
import com.campussync.entity.Department;
import com.campussync.entity.Role;
import com.campussync.entity.Student;
import com.campussync.entity.User;
import com.campussync.repository.DepartmentRepository;
import com.campussync.repository.StudentRepository;
import com.campussync.repository.UserRepository;
import com.campussync.repository.FacultyRepository;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StudentServiceTest {
    @Mock StudentRepository students;
    @Mock UserRepository users;
    @Mock DepartmentRepository departments;
    @Mock AuthorizationService authorization;
    @Mock FacultyRepository faculty;

    @Test void adminCreatesStudentProfileLinkedToStudentRoleAccount() {
        User account = new User(); account.setId(12L);
        Role role = new Role(); role.setCode("STUDENT"); account.setRole(role);
        Department department = new Department(); department.setId(8L);
        when(users.findById(12L)).thenReturn(Optional.of(account));
        when(departments.findById(8L)).thenReturn(Optional.of(department));
        when(students.saveAndFlush(any(Student.class))).thenAnswer(call -> {
            Student student = call.getArgument(0); student.setId(4L); return student;
        });
        var request = new StudentRequest(12L, 8L, " EN-1 ", LocalDate.now(), " ACTIVE ", 1, LocalDate.now().getYear(),
                null, null, null, null, null);

        var response = new StudentService(students, users, departments, authorization, faculty).create(request, () -> "admin");

        assertEquals(4L, response.id());
        assertEquals("EN-1", response.enrollmentNumber());
        assertEquals("ACTIVE", response.academicStatus());
    }
}
