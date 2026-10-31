package com.campussync.service;

import com.campussync.dto.CoordinatorProvisionRequest;
import com.campussync.dto.CoordinatorProvisionResponse;
import com.campussync.entity.Committee;
import com.campussync.entity.CommitteeMember;
import com.campussync.entity.Role;
import com.campussync.entity.User;
import com.campussync.repository.CommitteeMemberRepository;
import com.campussync.repository.CommitteeRepository;
import com.campussync.repository.RoleRepository;
import com.campussync.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.time.LocalDate;
import java.util.NoSuchElementException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CoordinatorProvisioningService {
    private final AuthorizationService authorization;
    private final UserRepository users;
    private final RoleRepository roles;
    private final CommitteeRepository committees;
    private final CommitteeMemberRepository memberships;
    private final PasswordEncoder passwordEncoder;

    public CoordinatorProvisioningService(AuthorizationService authorization, UserRepository users,
                                          RoleRepository roles, CommitteeRepository committees,
                                          CommitteeMemberRepository memberships, PasswordEncoder passwordEncoder) {
        this.authorization = authorization;
        this.users = users;
        this.roles = roles;
        this.committees = committees;
        this.memberships = memberships;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public CoordinatorProvisionResponse create(CoordinatorProvisionRequest request, Principal principal) {
        authorization.requireRole(principal, "ADMINISTRATOR");
        String username = request.username().trim();
        String email = request.email().trim().toLowerCase(java.util.Locale.ROOT);
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be at most 72 UTF-8 bytes.");
        }
        if (users.existsByUsernameIgnoreCase(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username is already registered.");
        }
        if (users.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered.");
        }
        Role coordinatorRole = roles.findByCode("COMMITTEE_COORDINATOR")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                        "Coordinator account provisioning is unavailable because the COMMITTEE_COORDINATOR role "
                                + "is not configured. Verify that approved role reference data is provisioned "
                                + "for this environment or contact a CampusSync administrator."));
        User user = new User();
        user.setRole(coordinatorRole);
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setActive(true);
        User savedUser = users.saveAndFlush(user);
        Committee committee = findActiveCommittee(request.committeeId());
        return assign(savedUser, committee);
    }

    @Transactional
    public CoordinatorProvisionResponse assignExisting(Long userId, Long committeeId, Principal principal) {
        authorization.requireRole(principal, "ADMINISTRATOR");
        User user = users.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found."));
        if (!user.isActive() || user.getRole() == null
                || !"COMMITTEE_COORDINATOR".equalsIgnoreCase(user.getRole().getCode())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only active committee-coordinator accounts can be assigned.");
        }
        return assign(user, findActiveCommittee(committeeId));
    }

    private Committee findActiveCommittee(Long committeeId) {
        Committee committee = committees.findById(committeeId)
                .orElseThrow(() -> new NoSuchElementException("Committee not found."));
        if (!committee.isActive() || !"ACTIVE".equalsIgnoreCase(committee.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose an active committee.");
        }
        return committee;
    }

    private CoordinatorProvisionResponse assign(User user, Committee committee) {
        if (memberships.existsByCommitteeIdAndUserIdAndMembershipStatusIgnoreCase(
                committee.getId(), user.getId(), "ACTIVE")) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This coordinator is already active in the selected committee.");
        }
        CommitteeMember membership = new CommitteeMember();
        membership.setCommittee(committee);
        membership.setUser(user);
        membership.setMemberRole("COORDINATOR");
        membership.setJoinedOn(LocalDate.now());
        membership.setMembershipStatus("ACTIVE");
        memberships.saveAndFlush(membership);
        return new CoordinatorProvisionResponse(user.getId(), user.getUsername(), user.getEmail(),
                user.getRole().getCode(), committee.getId(), committee.getName());
    }
}
