package com.campussync.dto;

import jakarta.validation.constraints.Pattern;

public record StudentLabBatchRequest(
        @Pattern(regexp = "^(B[1-4]|b[1-4])$") String labBatch) {
}
