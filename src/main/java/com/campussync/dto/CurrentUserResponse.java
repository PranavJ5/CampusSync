package com.campussync.dto;

public record CurrentUserResponse(Long userId, String username, String email, String role) {
}
