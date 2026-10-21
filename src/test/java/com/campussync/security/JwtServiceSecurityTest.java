package com.campussync.security;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServiceSecurityTest {

    private static final String SECRET = "test-secret-that-is-at-least-32-bytes-long";

    @Test
    void createsAndValidatesSignedToken() {
        JwtService service = new JwtService(SECRET, 3600);
        CampusUserPrincipal principal = principal("STUDENT");

        String token = service.generateToken(principal);

        assertEquals("student@example.com", service.extractUsername(token));
        org.junit.jupiter.api.Assertions.assertTrue(service.isValid(token, principal));
    }

    @Test
    void rejectsInvalidSignature() {
        JwtService service = new JwtService(SECRET, 3600);
        String token = service.generateToken(principal("STUDENT"));
        String tampered = token.substring(0, token.length() - 1) + (token.endsWith("a") ? "b" : "a");

        assertThrows(JwtException.class, () -> service.extractUsername(tampered));
    }

    @Test
    void rejectsExpiredToken() {
        JwtService service = new JwtService(SECRET, 1);
        String token = service.generateToken(principal("STUDENT"));

        org.junit.jupiter.api.Assertions.assertTimeoutPreemptively(java.time.Duration.ofSeconds(3), () -> {
            Thread.sleep(1100);
            assertThrows(JwtException.class, () -> service.extractUsername(token));
        });
    }

    @Test
    void rejectsWeakSecret() {
        assertThrows(IllegalArgumentException.class, () -> new JwtService("too-short", 3600));
    }

    private CampusUserPrincipal principal(String role) {
        return new CampusUserPrincipal(7L, "student", "student@example.com", "hash", true, role);
    }
}
