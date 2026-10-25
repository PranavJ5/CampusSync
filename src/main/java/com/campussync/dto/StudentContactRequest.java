package com.campussync.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record StudentContactRequest(@Pattern(regexp = "^$|^[+]?[0-9() .-]{7,20}$") String contactNumber,
                                    @Email @Size(max = 255) String alternateEmail,
                                    @Size(max = 500) String photoPath) { }
