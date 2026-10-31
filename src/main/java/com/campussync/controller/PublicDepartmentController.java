package com.campussync.controller;

import com.campussync.dto.SignupDepartmentResponse;
import com.campussync.repository.DepartmentRepository;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/departments")
public class PublicDepartmentController {
    private final DepartmentRepository departments;

    public PublicDepartmentController(DepartmentRepository departments) {
        this.departments = departments;
    }

    @GetMapping
    public List<SignupDepartmentResponse> listActive() {
        return departments.findByActiveTrueOrderByNameAsc().stream()
                .map(department -> new SignupDepartmentResponse(
                        department.getId(), department.getCode(), department.getName()))
                .toList();
    }
}
