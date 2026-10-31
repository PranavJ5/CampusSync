package com.campussync.dto;

public record CoordinatorProvisionResponse(
        Long userId,
        String username,
        String email,
        String role,
        Long committeeId,
        String committeeName
) {
}
