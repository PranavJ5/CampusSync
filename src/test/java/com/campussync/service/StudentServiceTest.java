package com.campussync.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campussync.dto.StudentRequest;
import com.campussync.dto.StudentLabBatchRequest;
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
                null, null, null, null, null, "B");

        var response = new StudentService(students, users, departments, authorization, faculty).create(request, () -> "admin");

        assertEquals(4L, response.id());
        assertEquals("EN-1", response.enrollmentNumber());
        assertEquals("ACTIVE", response.academicStatus());
    }

    @Test void administratorCanAssignStudentLabBatch() {
        Student student = new Student();
        student.setId(4L);
        student.setUser(new User());
        student.getUser().setId(12L);
        student.getUser().setUsername("student");
        student.getUser().setEmail("student@example.test");
        student.setDepartment(new Department());
        student.setEnrollmentNumber("EN-1");
        when(students.findById(4L)).thenReturn(Optional.of(student));
        when(students.saveAndFlush(student)).thenReturn(student);

        var response = new StudentService(students, users, departments, authorization, faculty)
                .updateLabBatch(4L, new StudentLabBatchRequest("b2"), () -> "admin");

        assertEquals("B2", response.labBatch());
    }

    @Test void nonAdministratorCannotAssignStudentLabBatch() {
        var studentPrincipal = (java.security.Principal) () -> "student";
        when(authorization.requireRole(studentPrincipal, "ADMINISTRATOR"))
                .thenThrow(new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.FORBIDDEN));

        var service = new StudentService(students, users, departments, authorization, faculty);

        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> service.updateLabBatch(4L, new StudentLabBatchRequest("B1"), studentPrincipal));
        verify(students, never()).findById(4L);
    }
}
