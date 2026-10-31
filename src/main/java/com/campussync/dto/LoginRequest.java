package com.campussync.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record LoginRequest(
        @NotBlank @Email String email,
        @NotBlank String password,
        @Pattern(regexp = "STUDENT|FACULTY|ADMINISTRATOR|COMMITTEE_COORDINATOR",
                message = "must be an approved CampusSync role")
        String selectedRole
) {
}
