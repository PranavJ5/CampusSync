package com.campussync.service;

import com.campussync.dto.CommitteeRequest;
import com.campussync.dto.CommitteeResponse;
import com.campussync.dto.CommitteeMemberRequest;
import com.campussync.dto.CommitteeMemberResponse;
import com.campussync.entity.Committee;
import com.campussync.entity.CommitteeMember;
import com.campussync.entity.User;
import com.campussync.entity.Faculty;
import com.campussync.repository.CommitteeRepository;
import com.campussync.repository.CommitteeMemberRepository;
import com.campussync.repository.DepartmentRepository;
import com.campussync.repository.UserRepository;
import com.campussync.repository.FacultyRepository;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CommitteeService {
    private final CommitteeRepository committees;
    private final DepartmentRepository departments;
    private final CommitteeMemberRepository memberships;
    private final UserRepository users;
    private final AuthorizationService authorization;
    private final FacultyRepository faculty;
    public CommitteeService(CommitteeRepository committees, DepartmentRepository departments,
                            CommitteeMemberRepository memberships, UserRepository users,
                            AuthorizationService authorization, FacultyRepository faculty) {
        this.committees = committees; this.departments = departments; this.memberships = memberships;
        this.users = users; this.authorization = authorization; this.faculty = faculty;
    }

    @Transactional(readOnly = true)
    public List<CommitteeResponse> list() { return committees.findAll().stream().map(CommitteeResponse::from).toList(); }

    @Transactional(readOnly = true)
    public List<CommitteeResponse> manageable(Principal principal) {
        User actor = authorization.authenticatedUser(principal);
        if (actor.getRole() != null && "ADMINISTRATOR".equalsIgnoreCase(actor.getRole().getCode())) {
            return committees.findByActiveTrueAndStatusIgnoreCaseOrderByNameAsc("ACTIVE").stream()
                    .map(CommitteeResponse::from).toList();
        }
        List<Long> committeeIds = authorization.coordinatorCommitteeIds(principal);
        if (committeeIds.isEmpty()) return List.of();
        return committees.findByIdInAndActiveTrueAndStatusIgnoreCaseOrderByNameAsc(committeeIds, "ACTIVE").stream()
                .map(CommitteeResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public CommitteeResponse get(Long id) { return CommitteeResponse.from(find(id)); }

    @Transactional
    public CommitteeResponse create(CommitteeRequest request) {
        requireUniqueCode(request.code(), null);
        Committee committee = new Committee();
        apply(committee, request);
        Committee saved;
        try {
            saved = committees.saveAndFlush(committee);
        } catch (DataIntegrityViolationException exception) {
            throw duplicateCommitteeCode(exception);
        }
        ensureCoordinatorMembership(saved, saved.getFacultyCoordinator().getUser());
        return CommitteeResponse.from(saved);
    }

    @Transactional
    public CommitteeResponse update(Long id, CommitteeRequest request) {
        Committee committee = find(id);
        requireUniqueCode(request.code(), id);
        Long previousCoordinatorUserId = committee.getFacultyCoordinator() == null
                ? null : committee.getFacultyCoordinator().getUser().getId();
        apply(committee, request);
        Committee saved;
        try {
            saved = committees.saveAndFlush(committee);
        } catch (DataIntegrityViolationException exception) {
            throw duplicateCommitteeCode(exception);
        }
        Long currentCoordinatorUserId = saved.getFacultyCoordinator().getUser().getId();
        if (previousCoordinatorUserId != null && !previousCoordinatorUserId.equals(currentCoordinatorUserId)) {
            closeCoordinatorMembership(saved.getId(), previousCoordinatorUserId);
        }
        ensureCoordinatorMembership(saved, saved.getFacultyCoordinator().getUser());
        return CommitteeResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<CommitteeMemberResponse> members(Long committeeId, Principal principal, Long departmentId, Integer semester) {
        find(committeeId);
        authorization.requireAdministratorOrCommitteeCoordinator(principal, committeeId);
        return memberships.findByCommitteeIdAndMembershipStatusIgnoreCaseOrderByJoinedOnAsc(committeeId, "ACTIVE")
                .stream().filter(member -> {
                    var student = member.getUser().getStudent();
                    var facultyProfile = member.getUser().getFaculty();
                    Long memberDepartmentId = student != null ? student.getDepartment().getId()
                            : facultyProfile != null ? facultyProfile.getDepartment().getId() : null;
                    if (departmentId != null && !departmentId.equals(memberDepartmentId)) return false;
                    return semester == null || (student != null && semester.equals(student.getCurrentSemester()));
                }).map(CommitteeMemberResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<CommitteeMemberResponse> memberHistory(Long committeeId, Principal principal) {
        find(committeeId);
        authorization.requireAdministratorOrCommitteeCoordinator(principal, committeeId);
        return memberships.findByCommitteeIdOrderByJoinedOnAsc(committeeId).stream()
                .map(CommitteeMemberResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<CommitteeMemberResponse> myMemberships(Principal principal) {
        User actor = authorization.requireRole(principal, "STUDENT");
        return memberships.findByUserIdAndMembershipStatusIgnoreCaseOrderByJoinedOnDesc(actor.getId(), "ACTIVE")
                .stream().map(CommitteeMemberResponse::from).toList();
    }

    @Transactional
    public CommitteeMemberResponse addMember(Long committeeId, CommitteeMemberRequest request, Principal principal) {
        Committee committee = committees.findByIdForUpdate(committeeId)
                .orElseThrow(() -> new NoSuchElementException("Committee not found."));
        User actor = authorization.requireAdministratorOrCommitteeCoordinator(principal, committeeId);
        if (!committee.isActive() || !"ACTIVE".equalsIgnoreCase(committee.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Members can only be added to an active committee.");
        }
        User user = users.findById(request.userId()).orElseThrow(() -> new NoSuchElementException("User not found."));
        if (!user.isActive()) throw new IllegalStateException("Inactive accounts cannot be added to a committee.");
        String role = user.getRole() == null ? "" : user.getRole().getCode();
        boolean coordinator = "COORDINATOR".equalsIgnoreCase(request.memberRole());
        if (!List.of("STUDENT", "FACULTY", "COMMITTEE_COORDINATOR").stream()
                .anyMatch(r -> r.equalsIgnoreCase(role)))
            throw new IllegalArgumentException("Committee members must have a student, faculty, or coordinator account.");
        if ("STUDENT".equalsIgnoreCase(role)
                && (user.getStudent() == null
                || !"ACTIVE".equalsIgnoreCase(user.getStudent().getAcademicStatus()))) {
            throw new IllegalArgumentException("Committee students must have an active student profile.");
        }
        if ("FACULTY".equalsIgnoreCase(role)
                && (user.getFaculty() == null
                || !"ACTIVE".equalsIgnoreCase(user.getFaculty().getEmploymentStatus()))) {
            throw new IllegalArgumentException("Committee faculty must have an active faculty profile.");
        }
        if (coordinator) {
            if (actor.getRole() == null || !"ADMINISTRATOR".equalsIgnoreCase(actor.getRole().getCode())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Only an administrator may assign committee coordinator privileges.");
            }
            if (!"STUDENT".equalsIgnoreCase(role) && !"COMMITTEE_COORDINATOR".equalsIgnoreCase(role)) {
                throw new IllegalArgumentException(
                        "A student coordinator must be a student account; faculty coordinators are assigned separately.");
            }
        }
        if (memberships.existsByCommitteeIdAndUserIdAndMembershipStatusIgnoreCase(committeeId, user.getId(), "ACTIVE"))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User is already an active member of this committee.");
        CommitteeMember member = new CommitteeMember();
        member.setCommittee(committee); member.setUser(user);
        member.setMemberRole(request.memberRole().trim().toUpperCase(Locale.ROOT));
        member.setJoinedOn(LocalDate.now()); member.setMembershipStatus("ACTIVE");
        try {
            return CommitteeMemberResponse.from(memberships.saveAndFlush(member));
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "User already has an active membership in this committee.", exception);
        }
    }

    @Transactional
    public void removeMember(Long committeeId, Long userId, Principal principal) {
        Committee committee = committees.findByIdForUpdate(committeeId)
                .orElseThrow(() -> new NoSuchElementException("Committee not found."));
        authorization.requireAdministratorOrCommitteeCoordinator(principal, committeeId);
        CommitteeMember member = memberships.findFirstByCommitteeIdAndUserIdAndMembershipStatusIgnoreCase(
                        committeeId, userId, "ACTIVE")
                .orElseThrow(() -> new NoSuchElementException("Active committee membership not found."));
        if (committee.getFacultyCoordinator() != null
                && committee.getFacultyCoordinator().getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Assign a different faculty coordinator before removing this membership.");
        }
        member.setMembershipStatus("INACTIVE"); member.setLeftOn(LocalDate.now());
    }

    private Committee find(Long id) {
        return committees.findById(id).orElseThrow(() -> new NoSuchElementException("Committee not found."));
    }

    private void apply(Committee committee, CommitteeRequest request) {
        if (request.facultyCoordinatorId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A faculty coordinator is required.");
        }
        String status = request.status().trim().toUpperCase(Locale.ROOT);
        if (!"ACTIVE".equals(status) && !"INACTIVE".equals(status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Committee status must be ACTIVE or INACTIVE.");
        }
        boolean active = request.active() == null ? "ACTIVE".equals(status) : request.active();
        if (active != "ACTIVE".equals(status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Committee active state and status must agree.");
        }
        committee.setDepartment(request.departmentId() == null ? null : departments.findById(request.departmentId())
                .orElseThrow(() -> new NoSuchElementException("Department not found.")));
        committee.setCode(request.code().trim());
        committee.setName(request.name().trim());
        committee.setDescription(request.description() == null ? null : request.description().trim());
        committee.setStatus(status);
        committee.setCategory(request.category().trim()); committee.setEstablishedYear(request.establishedYear());
        Faculty coordinator = faculty.findById(request.facultyCoordinatorId())
                .orElseThrow(() -> new NoSuchElementException("Faculty coordinator not found."));
        if (!coordinator.getUser().isActive()
                || coordinator.getUser().getRole() == null
                || !"FACULTY".equalsIgnoreCase(coordinator.getUser().getRole().getCode())
                || !"ACTIVE".equalsIgnoreCase(coordinator.getEmploymentStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "The faculty coordinator must have an active faculty profile and account.");
        }
        committee.setFacultyCoordinator(coordinator);
        committee.setLogoPath(request.logoPath());
        committee.setActive(active);
    }

    private void ensureCoordinatorMembership(Committee committee, User user) {
        CommitteeMember membership = memberships.findFirstByCommitteeIdAndUserIdAndMembershipStatusIgnoreCase(
                committee.getId(), user.getId(), "ACTIVE").orElse(null);
        if (membership == null) {
            membership = new CommitteeMember();
            membership.setCommittee(committee);
            membership.setUser(user);
            membership.setJoinedOn(LocalDate.now());
            membership.setMembershipStatus("ACTIVE");
        }
        membership.setMemberRole("COORDINATOR");
        try {
            memberships.saveAndFlush(membership);
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Coordinator already has an active membership in this committee.", exception);
        }
    }

    private void closeCoordinatorMembership(Long committeeId, Long userId) {
        memberships.findFirstByCommitteeIdAndUserIdAndMembershipStatusIgnoreCase(
                        committeeId, userId, "ACTIVE")
                .filter(member -> "COORDINATOR".equalsIgnoreCase(member.getMemberRole()))
                .ifPresent(member -> {
                    member.setMembershipStatus("INACTIVE");
                    member.setLeftOn(LocalDate.now());
                });
    }

    private ResponseStatusException duplicateCommitteeCode(DataIntegrityViolationException cause) {
        return new ResponseStatusException(HttpStatus.CONFLICT,
                "A committee with this code already exists.", cause);
    }

    private void requireUniqueCode(String code, Long excludedId) {
        String normalizedCode = code.trim();
        boolean exists = excludedId == null
                ? committees.existsByCodeIgnoreCase(normalizedCode)
                : committees.existsByCodeIgnoreCaseAndIdNot(normalizedCode, excludedId);
        if (exists) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A committee with this code already exists.");
        }
    }
}
