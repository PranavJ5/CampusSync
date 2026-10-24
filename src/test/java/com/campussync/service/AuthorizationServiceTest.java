package com.campussync.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.campussync.entity.CommitteeMember;
import com.campussync.entity.Role;
import com.campussync.entity.User;
import com.campussync.repository.CommitteeMemberRepository;
import com.campussync.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class AuthorizationServiceTest {
    @Mock UserRepository users;
    @Mock CommitteeMemberRepository memberships;

    @Test void missingPrincipalIsUnauthorized() {
        var failure = assertThrows(ResponseStatusException.class,
                () -> new AuthorizationService(users, memberships).authenticatedUser(null));
        assertEquals(HttpStatus.UNAUTHORIZED, failure.getStatusCode());
    }

    @Test void coordinatorScopeRequiresAnActiveCoordinatorMembership() {
        User coordinator = new User(); coordinator.setId(4L); coordinator.setUsername("coordinator");
        Role role = new Role(); role.setCode("COORDINATOR"); coordinator.setRole(role);
        when(users.findByUsername("coordinator")).thenReturn(Optional.of(coordinator));
        when(memberships.findByCommitteeId(8L)).thenReturn(List.of());

        var failure = assertThrows(ResponseStatusException.class,
                () -> new AuthorizationService(users, memberships).requireAdministratorOrCommitteeCoordinator(
                        () -> "coordinator", 8L));

        assertEquals(HttpStatus.FORBIDDEN, failure.getStatusCode());
    }

    @Test void inactiveUserCannotAuthenticateThroughPrincipalLookup() {
        User inactive = new User(); inactive.setUsername("inactive"); inactive.setActive(false);
        when(users.findByUsername("inactive")).thenReturn(Optional.of(inactive));

        var failure = assertThrows(ResponseStatusException.class,
                () -> new AuthorizationService(users, memberships).authenticatedUser(() -> "inactive"));
        assertEquals(HttpStatus.UNAUTHORIZED, failure.getStatusCode());
    }
}
