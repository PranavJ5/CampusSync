package com.campussync.service;

import com.campussync.entity.CommitteeMember;
import com.campussync.entity.User;
import com.campussync.repository.CommitteeMemberRepository;
import com.campussync.repository.UserRepository;
import com.campussync.security.ApprovedRole;
import java.security.Principal;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthorizationService {
    private static final Set<String> SCOPED_COORDINATOR_ROLES =
            Set.of("COMMITTEE_COORDINATOR", "FACULTY", "STUDENT");

    private final UserRepository users;
    private final CommitteeMemberRepository memberships;

    public AuthorizationService(UserRepository users, CommitteeMemberRepository memberships) {
        this.users = users;
        this.memberships = memberships;
    }

    public User authenticatedUser(Principal principal) {
        if (principal == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required.");
        User user = users.findByUsername(principal.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated account was not found."));
        if (!user.isActive()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account is inactive.");
        return user;
    }

    public User requireRole(Principal principal, String roleCode) {
        User user = authenticatedUser(principal);
        if (user.getRole() == null || !roleCode.equalsIgnoreCase(user.getRole().getCode()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This operation requires the " + roleCode + " role.");
        return user;
    }

    public User requireAdministratorOrCommitteeCoordinator(Principal principal, Long committeeId) {
        User user = authenticatedUser(principal);
        String role = user.getRole() == null ? "" : user.getRole().getCode().toUpperCase(Locale.ROOT);
        if (ApprovedRole.ADMINISTRATOR.name().equals(role)) return user;
        if (!SCOPED_COORDINATOR_ROLES.contains(role))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Administrator or assigned committee coordinator access is required.");
        boolean assigned = memberships.findByCommitteeId(committeeId).stream()
                .filter(member -> member.getUser().getId().equals(user.getId()))
                .anyMatch(AuthorizationService::isActiveCoordinatorMembership);
        if (!assigned) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Coordinator access is limited to assigned committees.");
        return user;
    }

    public List<Long> coordinatorCommitteeIds(Principal principal) {
        User user = authenticatedUser(principal);
        String role = user.getRole() == null ? "" : user.getRole().getCode().toUpperCase(Locale.ROOT);
        if (!SCOPED_COORDINATOR_ROLES.contains(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Assigned committee coordinator access is required.");
        }
        return memberships.findByUserIdAndMembershipStatusIgnoreCaseOrderByJoinedOnDesc(
                        user.getId(), "ACTIVE").stream()
                .filter(AuthorizationService::isActiveCoordinatorMembership)
                .map(member -> member.getCommittee().getId())
                .distinct()
                .toList();
    }

    private static boolean isActiveCoordinatorMembership(CommitteeMember member) {
        return "ACTIVE".equalsIgnoreCase(member.getMembershipStatus())
                && member.getCommittee() != null
                && member.getCommittee().isActive()
                && "ACTIVE".equalsIgnoreCase(member.getCommittee().getStatus())
                && "COORDINATOR".equalsIgnoreCase(member.getMemberRole());
    }
}
