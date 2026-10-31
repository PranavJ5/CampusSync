package com.campussync.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campussync.dto.CoordinatorProvisionRequest;
import com.campussync.entity.Committee;
import com.campussync.entity.CommitteeMember;
import com.campussync.entity.Role;
import com.campussync.entity.User;
import com.campussync.repository.CommitteeMemberRepository;
import com.campussync.repository.CommitteeRepository;
import com.campussync.repository.RoleRepository;
import com.campussync.repository.UserRepository;
import java.security.Principal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class CoordinatorProvisioningServiceTest {
    @Mock AuthorizationService authorization;
    @Mock UserRepository users;
    @Mock RoleRepository roles;
    @Mock CommitteeRepository committees;
    @Mock CommitteeMemberRepository memberships;
    private BCryptPasswordEncoder encoder;
    private CoordinatorProvisioningService service;
    private Committee committee;
    private Role coordinatorRole;
    private final Principal administrator = () -> "admin";

    @BeforeEach
    void setUp() {
        encoder = new BCryptPasswordEncoder();
        service = new CoordinatorProvisioningService(
                authorization, users, roles, committees, memberships, encoder);
        committee = new Committee();
        committee.setId(12L);
        committee.setName("Sports Committee");
        committee.setActive(true);
        committee.setStatus("ACTIVE");
        coordinatorRole = new Role();
        coordinatorRole.setCode("COMMITTEE_COORDINATOR");
    }

    @Test
    void administratorCreatesHashedCoordinatorAndActiveScopedMembership() {
        when(authorization.requireRole(administrator, "ADMINISTRATOR")).thenReturn(new User());
        when(users.existsByUsernameIgnoreCase("sportslead")).thenReturn(false);
        when(users.existsByEmailIgnoreCase("sports@example.com")).thenReturn(false);
        when(roles.findByCode("COMMITTEE_COORDINATOR")).thenReturn(Optional.of(coordinatorRole));
        when(users.saveAndFlush(any(User.class))).thenAnswer(invocation -> {
            User created = invocation.getArgument(0);
            created.setId(31L);
            return created;
        });
        when(committees.findById(12L)).thenReturn(Optional.of(committee));
        when(memberships.existsByCommitteeIdAndUserIdAndMembershipStatusIgnoreCase(12L, 31L, "ACTIVE"))
                .thenReturn(false);
        when(memberships.saveAndFlush(any(CommitteeMember.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.create(request(), administrator);

        assertEquals("COMMITTEE_COORDINATOR", response.role());
        assertEquals("Sports Committee", response.committeeName());
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(users).saveAndFlush(userCaptor.capture());
        User created = userCaptor.getValue();
        assertTrue(encoder.matches("coordinator-password", created.getPasswordHash()));
        assertFalse(created.getPasswordHash().equals("coordinator-password"));
        ArgumentCaptor<CommitteeMember> membershipCaptor = ArgumentCaptor.forClass(CommitteeMember.class);
        verify(memberships).saveAndFlush(membershipCaptor.capture());
        assertEquals(created, membershipCaptor.getValue().getUser());
        assertEquals("COORDINATOR", membershipCaptor.getValue().getMemberRole());
        assertEquals("ACTIVE", membershipCaptor.getValue().getMembershipStatus());
    }

    @Test
    void provisioningRequiresAdministratorAuthorization() {
        when(authorization.requireRole(administrator, "ADMINISTRATOR"))
                .thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN));

        assertThrows(ResponseStatusException.class, () -> service.create(request(), administrator));

        verify(users, never()).saveAndFlush(any());
        verify(memberships, never()).saveAndFlush(any());
    }

    @Test
    void missingCoordinatorRoleReturnsActionableServiceUnavailable() {
        when(authorization.requireRole(administrator, "ADMINISTRATOR")).thenReturn(new User());
        when(users.existsByUsernameIgnoreCase("sportslead")).thenReturn(false);
        when(users.existsByEmailIgnoreCase("sports@example.com")).thenReturn(false);
        when(roles.findByCode("COMMITTEE_COORDINATOR")).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class, () -> service.create(request(), administrator));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatusCode());
        assertTrue(exception.getReason().contains("COMMITTEE_COORDINATOR role"));
        verify(users, never()).saveAndFlush(any());
        verify(memberships, never()).saveAndFlush(any());
    }

    @Test
    void rejectsDuplicateUsernameAndEmailBeforeCreatingCoordinator() {
        when(authorization.requireRole(administrator, "ADMINISTRATOR")).thenReturn(new User());
        when(users.existsByUsernameIgnoreCase("sportslead")).thenReturn(true);

        ResponseStatusException usernameFailure = assertThrows(
                ResponseStatusException.class, () -> service.create(request(), administrator));
        assertEquals(HttpStatus.CONFLICT, usernameFailure.getStatusCode());
        assertEquals("Username is already registered.", usernameFailure.getReason());

        when(users.existsByUsernameIgnoreCase("sportslead")).thenReturn(false);
        when(users.existsByEmailIgnoreCase("sports@example.com")).thenReturn(true);
        ResponseStatusException emailFailure = assertThrows(
                ResponseStatusException.class, () -> service.create(request(), administrator));
        assertEquals(HttpStatus.CONFLICT, emailFailure.getStatusCode());
        assertEquals("Email is already registered.", emailFailure.getReason());

        verify(users, never()).saveAndFlush(any());
        verify(memberships, never()).saveAndFlush(any());
    }

    @Test
    void propagatesMembershipPersistenceFailuresInsteadOfReturningSuccess() {
        when(authorization.requireRole(administrator, "ADMINISTRATOR")).thenReturn(new User());
        when(users.existsByUsernameIgnoreCase("sportslead")).thenReturn(false);
        when(users.existsByEmailIgnoreCase("sports@example.com")).thenReturn(false);
        when(roles.findByCode("COMMITTEE_COORDINATOR")).thenReturn(Optional.of(coordinatorRole));
        when(users.saveAndFlush(any(User.class))).thenAnswer(invocation -> {
            User created = invocation.getArgument(0);
            created.setId(31L);
            return created;
        });
        when(committees.findById(12L)).thenReturn(Optional.of(committee));
        when(memberships.existsByCommitteeIdAndUserIdAndMembershipStatusIgnoreCase(12L, 31L, "ACTIVE"))
                .thenReturn(false);
        doThrow(new DataIntegrityViolationException("membership persistence failed"))
                .when(memberships).saveAndFlush(any(CommitteeMember.class));

        assertThrows(DataIntegrityViolationException.class, () -> service.create(request(), administrator));

        verify(users).saveAndFlush(any(User.class));
        verify(memberships).saveAndFlush(any(CommitteeMember.class));
    }

    @Test
    void existingRegularAccountsCannotBePromotedByTheAssignmentEndpoint() {
        User student = new User();
        student.setId(42L);
        Role studentRole = new Role();
        studentRole.setCode("STUDENT");
        student.setRole(studentRole);
        student.setActive(true);
        when(authorization.requireRole(administrator, "ADMINISTRATOR")).thenReturn(new User());
        when(users.findById(42L)).thenReturn(Optional.of(student));

        assertThrows(ResponseStatusException.class, () -> service.assignExisting(42L, 12L, administrator));

        verify(committees, never()).findById(any());
        verify(memberships, never()).saveAndFlush(any());
    }

    private CoordinatorProvisionRequest request() {
        return new CoordinatorProvisionRequest("sportslead", "sports@example.com",
                "coordinator-password", "Sports", "Lead", 12L);
    }
}
