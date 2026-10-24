package com.campussync.dto;

import static org.junit.jupiter.api.Assertions.assertFalse;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class RequestValidationTest {
    private static Validator validator;
    private static ValidatorFactory factory;

    @BeforeAll static void createValidator() { factory = Validation.buildDefaultValidatorFactory(); validator = factory.getValidator(); }
    @AfterAll static void closeValidator() { factory.close(); }

    @Test void eventCancellationRequiresAReason() {
        assertFalse(validator.validate(new EventCancellationRequest("  ")).isEmpty());
    }

    @Test void committeeMemberRoleIsRestrictedToSupportedValues() {
        assertFalse(validator.validate(new CommitteeMemberRequest(1L, "OWNER")).isEmpty());
    }

    @Test void studentSemesterCannotExceedEight() {
        var request = new StudentRequest(1L, 1L, "EN-1", LocalDate.now(), "ACTIVE", 9,
                LocalDate.now().getYear(), null, null, null, null, null);
        assertFalse(validator.validate(request).isEmpty());
    }

    @Test void optionalEventUpdateTextCannotBeBlank() {
        assertFalse(validator.validate(new EventUpdateRequest("  ", null, null, null, null, null, null, null)).isEmpty());
        assertFalse(validator.validate(new EventUpdateRequest(null, null, null, null, null, null, "  ", null)).isEmpty());
    }
}
