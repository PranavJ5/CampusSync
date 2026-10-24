package com.campussync.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.campussync.dto.SubjectRequest;
import com.campussync.entity.Department;
import com.campussync.entity.Subject;
import com.campussync.repository.DepartmentRepository;
import com.campussync.repository.SubjectRepository;
import com.campussync.repository.FacultyRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SubjectServiceTest {
    @Mock SubjectRepository subjects;
    @Mock DepartmentRepository departments;
    @Mock FacultyRepository faculty;

    @Test void createsSubjectWithDepartmentReference() {
        Department department = new Department(); department.setId(8L);
        when(departments.findById(8L)).thenReturn(Optional.of(department));
        when(subjects.saveAndFlush(any(Subject.class))).thenAnswer(call -> {
            Subject subject = call.getArgument(0); subject.setId(12L); return subject;
        });

        var response = new SubjectService(subjects, departments, faculty).create(
                new SubjectRequest(8L, " CS101 ", " Programming ", new BigDecimal("4.0"), 1, "2026-27", null, "A"));

        assertEquals(12L, response.id());
        assertEquals(8L, response.departmentId());
        assertEquals("CS101", response.code());
        assertEquals("Programming", response.name());
    }
}
