package com.campussync.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RoleValidationSecurityTest {

    @Test
    void acceptsOnlyApprovedExactRoleCodes() {
        for (ApprovedRole role : ApprovedRole.values()) {
            CampusUserPrincipal principal = new CampusUserPrincipal(1L, "user", "user@example.com",
                    "hash", true, role.name());
            assertEquals("ROLE_" + role.name(), principal.getAuthorities().iterator().next().getAuthority());
        }
    }

    @Test
    void rejectsUnrecognizedRoleCodes() {
        assertThrows(IllegalArgumentException.class, () -> new CampusUserPrincipal(
                1L, "user", "user@example.com", "hash", true, "ROLE_STUDENT"));
        assertThrows(IllegalArgumentException.class, () -> new CampusUserPrincipal(
                1L, "user", "user@example.com", "hash", true, "student"));
        assertThrows(IllegalArgumentException.class, () -> new CampusUserPrincipal(
                1L, "user", "user@example.com", "hash", true, "UNKNOWN"));
    }
}
