package com.campussync.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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

    @Test void administratorCanManageEventsWithoutCommitteeMembership() {
        User administrator = user(1L, "administrator", "ADMINISTRATOR");
        when(users.findByUsername("administrator")).thenReturn(Optional.of(administrator));

        User authorized = new AuthorizationService(users, memberships)
                .requireAdministratorOrCommitteeCoordinator(() -> "administrator", 8L);

        assertEquals(administrator, authorized);
        verify(memberships, never()).findByCommitteeId(8L);
    }

    @Test void administratorRolePassesAdministratorOnlyChecks() {
        User administrator = user(1L, "administrator", "ADMINISTRATOR");
        when(users.findByUsername("administrator")).thenReturn(Optional.of(administrator));

        User authorized = new AuthorizationService(users, memberships)
                .requireRole(() -> "administrator", "ADMINISTRATOR");

        assertEquals(administrator, authorized);
    }

    @Test void studentCannotPassAdministratorOnlyChecks() {
        User student = user(4L, "student", "STUDENT");
        when(users.findByUsername("student")).thenReturn(Optional.of(student));

        var failure = assertThrows(ResponseStatusException.class,
                () -> new AuthorizationService(users, memberships)
                        .requireRole(() -> "student", "ADMINISTRATOR"));

        assertEquals(HttpStatus.FORBIDDEN, failure.getStatusCode());
    }

    @Test void assignedCommitteeCoordinatorCanManageTheirCommitteeEvents() {
        User coordinator = user(4L, "coordinator", "COMMITTEE_COORDINATOR");
        CommitteeMember membership = membership(coordinator, "COORDINATOR", "ACTIVE");
        when(users.findByUsername("coordinator")).thenReturn(Optional.of(coordinator));
        when(memberships.findByCommitteeId(8L)).thenReturn(List.of(membership));

        User authorized = new AuthorizationService(users, memberships)
                .requireAdministratorOrCommitteeCoordinator(() -> "coordinator", 8L);

        assertEquals(coordinator, authorized);
    }

    @Test void unassignedCommitteeCoordinatorCannotManageCommitteeEvents() {
        User coordinator = user(4L, "coordinator", "COMMITTEE_COORDINATOR");
        when(users.findByUsername("coordinator")).thenReturn(Optional.of(coordinator));
        when(memberships.findByCommitteeId(8L)).thenReturn(List.of());

        var failure = assertThrows(ResponseStatusException.class,
                () -> new AuthorizationService(users, memberships).requireAdministratorOrCommitteeCoordinator(
                        () -> "coordinator", 8L));

        assertEquals(HttpStatus.FORBIDDEN, failure.getStatusCode());
    }

    @Test void assignedStudentCoordinatorCanManageOnlyTheirCommittee() {
        User student = user(4L, "student", "STUDENT");
        CommitteeMember membership = membership(student, "COORDINATOR", "ACTIVE");
        when(users.findByUsername("student")).thenReturn(Optional.of(student));
        when(memberships.findByCommitteeId(8L)).thenReturn(List.of(membership));

        User authorized = new AuthorizationService(users, memberships)
                .requireAdministratorOrCommitteeCoordinator(() -> "student", 8L);

        assertEquals(student, authorized);
    }

    @Test void assignedFacultyCoordinatorCanManageTheirCommittee() {
        User faculty = user(4L, "faculty", "FACULTY");
        CommitteeMember membership = membership(faculty, "COORDINATOR", "ACTIVE");
        when(users.findByUsername("faculty")).thenReturn(Optional.of(faculty));
        when(memberships.findByCommitteeId(8L)).thenReturn(List.of(membership));

        User authorized = new AuthorizationService(users, memberships)
                .requireAdministratorOrCommitteeCoordinator(() -> "faculty", 8L);

        assertEquals(faculty, authorized);
    }

    @Test void unassignedStudentCannotManageCommittee() {
        User student = user(4L, "student", "STUDENT");
        when(users.findByUsername("student")).thenReturn(Optional.of(student));
        when(memberships.findByCommitteeId(8L)).thenReturn(List.of());

        var failure = assertThrows(ResponseStatusException.class,
                () -> new AuthorizationService(users, memberships).requireAdministratorOrCommitteeCoordinator(
                        () -> "student", 8L));

        assertEquals(HttpStatus.FORBIDDEN, failure.getStatusCode());
    }

    @Test void assignedStudentCoordinatorCannotManageAnotherCommittee() {
        User student = user(4L, "student", "STUDENT");
        when(users.findByUsername("student")).thenReturn(Optional.of(student));
        when(memberships.findByCommitteeId(8L)).thenReturn(List.of());

        var failure = assertThrows(ResponseStatusException.class,
                () -> new AuthorizationService(users, memberships)
                        .requireAdministratorOrCommitteeCoordinator(() -> "student", 8L));

        assertEquals(HttpStatus.FORBIDDEN, failure.getStatusCode());
    }

    @Test void coordinatorScopeRequiresAnActiveCoordinatorMembership() {
        User coordinator = new User(); coordinator.setId(4L); coordinator.setUsername("coordinator");
        Role role = new Role(); role.setCode("COMMITTEE_COORDINATOR"); coordinator.setRole(role);
        when(users.findByUsername("coordinator")).thenReturn(Optional.of(coordinator));
        when(memberships.findByCommitteeId(8L))
                .thenReturn(List.of(membership(coordinator, "COORDINATOR", "INACTIVE")));

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

    private User user(Long id, String username, String roleCode) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setActive(true);
        Role role = new Role();
        role.setCode(roleCode);
        user.setRole(role);
        return user;
    }

    private CommitteeMember membership(User user, String memberRole, String status) {
        CommitteeMember member = new CommitteeMember();
        member.setUser(user);
        member.setMemberRole(memberRole);
        member.setMembershipStatus(status);
        com.campussync.entity.Committee committee = new com.campussync.entity.Committee();
        committee.setActive(true);
        committee.setStatus("ACTIVE");
        member.setCommittee(committee);
        return member;
    }
}
