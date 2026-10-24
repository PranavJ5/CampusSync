package com.campussync.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campussync.dto.DepartmentRequest;
import com.campussync.entity.Department;
import com.campussync.repository.DepartmentRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceTest {
    @Mock DepartmentRepository departments;

    @Test void createsValidatedDepartmentDataWithoutExposingTheEntity() {
        when(departments.saveAndFlush(any(Department.class))).thenAnswer(call -> {
            Department department = call.getArgument(0);
            department.setId(17L);
            return department;
        });
        DepartmentService service = new DepartmentService(departments);

        var response = service.create(new DepartmentRequest(" CS ", " Computer Science ", "cs@example.edu ", null, null, null));

        assertEquals(17L, response.id());
        assertEquals("CS", response.code());
        assertEquals("Computer Science", response.name());
        assertEquals("cs@example.edu", response.contactEmail());
        verify(departments).saveAndFlush(any(Department.class));
    }

    @Test void updatesAnExistingDepartment() {
        Department existing = new Department(); existing.setId(4L);
        when(departments.findById(4L)).thenReturn(Optional.of(existing));
        when(departments.saveAndFlush(existing)).thenReturn(existing);

        var response = new DepartmentService(departments).update(4L, new DepartmentRequest("EE", "Electrical", null, null, null, null));

        assertEquals("EE", response.code());
        assertEquals("Electrical", response.name());
    }
}
