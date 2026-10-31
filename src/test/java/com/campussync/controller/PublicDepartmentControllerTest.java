package com.campussync.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campussync.entity.Department;
import com.campussync.repository.DepartmentRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class PublicDepartmentControllerTest {
    @Mock DepartmentRepository departments;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new PublicDepartmentController(departments)).build();
    }

    @Test
    void listsRequiredDepartmentsByActiveRecordsWithoutInventingIds() throws Exception {
        when(departments.findByActiveTrueOrderByNameAsc()).thenReturn(List.of(
                department(17L, "CE", "Computer Engineering"),
                department(21L, "CSE", "Computer Science and Engineering"),
                department(29L, "EXTC", "Electronics and Telecommunication")));

        mockMvc.perform(get("/api/public/departments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].id").value(17))
                .andExpect(jsonPath("$[0].name").value("Computer Engineering"))
                .andExpect(jsonPath("$[1].name").value("Computer Science and Engineering"))
                .andExpect(jsonPath("$[2].name").value("Electronics and Telecommunication"));
    }

    @Test
    void returnsAnEmptyArrayWhenNoActiveDepartmentsExist() throws Exception {
        when(departments.findByActiveTrueOrderByNameAsc()).thenReturn(List.of());

        mockMvc.perform(get("/api/public/departments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    private Department department(Long id, String code, String name) {
        Department department = new Department();
        department.setId(id);
        department.setCode(code);
        department.setName(name);
        department.setActive(true);
        return department;
    }
}
