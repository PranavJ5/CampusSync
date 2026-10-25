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
    public CommitteeResponse get(Long id) { return CommitteeResponse.from(find(id)); }

    @Transactional
    public CommitteeResponse create(CommitteeRequest request) {
        Committee committee = new Committee(); apply(committee, request);
        return CommitteeResponse.from(committees.saveAndFlush(committee));
    }

    @Transactional
    public CommitteeResponse update(Long id, CommitteeRequest request) {
        Committee committee = find(id); apply(committee, request);
        return CommitteeResponse.from(committees.saveAndFlush(committee));
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
        User user = users.findById(request.userId()).orElseThrow(() -> new NoSuchElementException("User not found."));
        if (!user.isActive()) throw new IllegalStateException("Inactive accounts cannot be added to a committee.");
        String role = user.getRole() == null ? "" : user.getRole().getCode();
        if (!List.of("STUDENT", "FACULTY", "COORDINATOR").stream().anyMatch(r -> r.equalsIgnoreCase(role)))
            throw new IllegalArgumentException("Committee members must have a student, faculty, or coordinator account.");
        if ("COORDINATOR".equalsIgnoreCase(request.memberRole())
                && (actor.getRole() == null || !"ADMIN".equalsIgnoreCase(actor.getRole().getCode())))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only an administrator may assign committee coordinator privileges.");
        if (memberships.existsByCommitteeIdAndUserIdAndMembershipStatusIgnoreCase(committeeId, user.getId(), "ACTIVE"))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User is already an active member of this committee.");
        CommitteeMember member = new CommitteeMember();
        member.setCommittee(committee); member.setUser(user); member.setMemberRole(request.memberRole().trim().toUpperCase(Locale.ROOT));
        member.setJoinedOn(LocalDate.now()); member.setMembershipStatus("ACTIVE");
        return CommitteeMemberResponse.from(memberships.saveAndFlush(member));
    }

    @Transactional
    public void removeMember(Long committeeId, Long userId, Principal principal) {
        committees.findByIdForUpdate(committeeId).orElseThrow(() -> new NoSuchElementException("Committee not found."));
        authorization.requireAdministratorOrCommitteeCoordinator(principal, committeeId);
        CommitteeMember member = memberships.findFirstByCommitteeIdAndUserIdAndMembershipStatusIgnoreCase(
                        committeeId, userId, "ACTIVE")
                .orElseThrow(() -> new NoSuchElementException("Active committee membership not found."));
        member.setMembershipStatus("INACTIVE"); member.setLeftOn(LocalDate.now());
    }

    private Committee find(Long id) {
        return committees.findById(id).orElseThrow(() -> new NoSuchElementException("Committee not found."));
    }

    private void apply(Committee committee, CommitteeRequest request) {
        committee.setDepartment(request.departmentId() == null ? null : departments.findById(request.departmentId())
                .orElseThrow(() -> new NoSuchElementException("Department not found.")));
        committee.setCode(request.code().trim());
        committee.setName(request.name().trim());
        committee.setDescription(request.description() == null ? null : request.description().trim());
        committee.setStatus(request.status().trim());
        committee.setCategory(request.category().trim()); committee.setEstablishedYear(request.establishedYear());
        committee.setFacultyCoordinator(request.facultyCoordinatorId() == null ? null : faculty.findById(request.facultyCoordinatorId())
                .orElseThrow(() -> new NoSuchElementException("Faculty coordinator not found.")));
        committee.setLogoPath(request.logoPath());
        if (request.active() != null) committee.setActive(request.active());
    }
}
