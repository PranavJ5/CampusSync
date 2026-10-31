package com.campussync.dto;

import com.campussync.entity.Committee;

public record CommitteeResponse(Long id, Long departmentId, String code, String name, String description, String status,
                                String category, Integer establishedYear, Long facultyCoordinatorId,
                                String facultyCoordinatorName, String logoPath, boolean active) {
    public static CommitteeResponse from(Committee committee) {
        var coordinator = committee.getFacultyCoordinator();
        return new CommitteeResponse(
                committee.getId(),
                committee.getDepartment() == null ? null : committee.getDepartment().getId(),
                committee.getCode(),
                committee.getName(),
                committee.getDescription(),
                committee.getStatus(),
                committee.getCategory(),
                committee.getEstablishedYear(),
                coordinator == null ? null : coordinator.getId(),
                coordinator == null ? null : coordinator.getUser().getFirstName() + " " + coordinator.getUser().getLastName(),
                committee.getLogoPath(),
                committee.isActive());
    }
}
