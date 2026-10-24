package com.campussync.dto;

import com.campussync.entity.Department;

public record DepartmentResponse(Long id, String code, String name, String contactEmail,
                                 Integer establishedYear, String hodName, boolean active) {
    public static DepartmentResponse from(Department department) {
        return new DepartmentResponse(department.getId(), department.getCode(), department.getName(), department.getContactEmail(),
                department.getEstablishedYear(), department.getHodName(), department.isActive());
    }
}
