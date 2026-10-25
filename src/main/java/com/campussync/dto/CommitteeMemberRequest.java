package com.campussync.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Pattern;

public record CommitteeMemberRequest(@NotNull @Positive Long userId,
                                     @NotBlank @Pattern(regexp = "(?i)MEMBER|COORDINATOR") String memberRole) { }
