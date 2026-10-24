package com.campussync.security;

import java.util.Arrays;
import java.util.Optional;

public enum ApprovedRole {
    ADMINISTRATOR,
    FACULTY,
    STUDENT,
    COMMITTEE_COORDINATOR;

    public static Optional<ApprovedRole> fromExactCode(String code) {
        return code == null ? Optional.empty()
                : Arrays.stream(values()).filter(role -> role.name().equals(code)).findFirst();
    }
}
