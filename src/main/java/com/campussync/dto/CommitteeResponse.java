package com.campussync.dto;

import com.campussync.entity.Committee;

public record CommitteeResponse(Long id, Long departmentId, String code, String name, String description, String status,
                                String category, Integer establishedYear, Long facultyCoordinatorId,
                                String logoPath, boolean active) {
    public static CommitteeResponse from(Committee committee) {
        return new CommitteeResponse(committee.getId(), committee.getDepartment() == null ? null : committee.getDepartment().getId(), committee.getCode(),
                committee.getName(), committee.getDescription(), committee.getStatus(), committee.getCategory(),
                committee.getEstablishedYear(), committee.getFacultyCoordinator() == null ? null : committee.getFacultyCoordinator().getId(),
                committee.getLogoPath(), committee.isActive());
    }
}
