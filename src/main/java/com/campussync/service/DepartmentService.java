package com.campussync.service;

import com.campussync.dto.DepartmentRequest;
import com.campussync.dto.DepartmentResponse;
import com.campussync.entity.Department;
import com.campussync.repository.DepartmentRepository;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DepartmentService {
    private final DepartmentRepository departments;

    public DepartmentService(DepartmentRepository departments) { this.departments = departments; }

    @Transactional(readOnly = true)
    public List<DepartmentResponse> list() { return departments.findAll().stream().map(DepartmentResponse::from).toList(); }

    @Transactional(readOnly = true)
    public DepartmentResponse get(Long id) { return DepartmentResponse.from(find(id)); }

    @Transactional
    public DepartmentResponse create(DepartmentRequest request) {
        Department department = new Department();
        apply(department, request);
        return DepartmentResponse.from(departments.saveAndFlush(department));
    }

    @Transactional
    public DepartmentResponse update(Long id, DepartmentRequest request) {
        Department department = find(id);
        apply(department, request);
        return DepartmentResponse.from(departments.saveAndFlush(department));
    }

    private Department find(Long id) {
        return departments.findById(id).orElseThrow(() -> new NoSuchElementException("Department not found."));
    }

    private void apply(Department department, DepartmentRequest request) {
        department.setCode(request.code().trim());
        department.setName(request.name().trim());
        department.setContactEmail(request.contactEmail() == null ? null : request.contactEmail().trim());
        department.setEstablishedYear(request.establishedYear());
        department.setHodName(request.hodName() == null ? null : request.hodName().trim());
        if (request.active() != null) department.setActive(request.active());
    }
}
