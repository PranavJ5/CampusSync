package com.campussync.dto;

public record LoginResponse(String token, String tokenType, long expiresInSeconds) {
}
