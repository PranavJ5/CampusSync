package com.campussync.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.campussync.dto.CommitteeRequest;
import com.campussync.entity.Committee;
import com.campussync.entity.Department;
import com.campussync.entity.CommitteeMember;
import com.campussync.entity.Faculty;
import com.campussync.entity.Role;
import com.campussync.entity.User;
import com.campussync.entity.Student;
import com.campussync.dto.CommitteeMemberRequest;
import com.campussync.repository.CommitteeRepository;
import com.campussync.repository.CommitteeMemberRepository;
import com.campussync.repository.DepartmentRepository;
import com.campussync.repository.UserRepository;
import com.campussync.repository.FacultyRepository;
import java.util.Optional;
import java.util.List;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CommitteeServiceTest {
    @Mock CommitteeRepository committees;
    @Mock DepartmentRepository departments;
    @Mock CommitteeMemberRepository memberships;
    @Mock UserRepository users;
    @Mock AuthorizationService authorization;
    @Mock FacultyRepository faculty;

    @Test void createsCommitteeWithDepartmentAndNormalizedValues() {
        Department department = new Department(); department.setId(3L);
        Faculty coordinator = facultyProfile(6L, 60L);
        when(departments.findById(3L)).thenReturn(Optional.of(department));
        when(faculty.findById(6L)).thenReturn(Optional.of(coordinator));
        when(memberships.findFirstByCommitteeIdAndUserIdAndMembershipStatusIgnoreCase(
                org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyLong(), eq("ACTIVE")))
                .thenReturn(Optional.empty());
        when(committees.saveAndFlush(any(Committee.class))).thenAnswer(call -> {
            Committee committee = call.getArgument(0); committee.setId(9L); return committee;
        });
        when(memberships.saveAndFlush(any(CommitteeMember.class))).thenAnswer(call -> call.getArgument(0));
        var request = new CommitteeRequest(
                3L, " TECH ", " Technical ", " Events ", " ACTIVE ", "TECHNICAL", null, 6L, null, true);

        var result = service().create(request);

        assertEquals(9L, result.id());
        assertEquals("TECH", result.code());
        assertEquals("Technical", result.name());
        assertEquals("Events", result.description());
        verify(memberships).saveAndFlush(any(CommitteeMember.class));
    }

    @Test void manageableCommitteeOptionsAreScopedToActiveCoordinatorAssignments() {
        User coordinator = new User();
        coordinator.setId(20L);
        Role role = new Role();
        role.setCode("COMMITTEE_COORDINATOR");
        coordinator.setRole(role);
        Committee assigned = new Committee();
        assigned.setId(5L);
        assigned.setName("Sports Committee");
        when(authorization.authenticatedUser(any())).thenReturn(coordinator);
        when(authorization.coordinatorCommitteeIds(any())).thenReturn(List.of(5L));
        when(committees.findByIdInAndActiveTrueAndStatusIgnoreCaseOrderByNameAsc(List.of(5L), "ACTIVE"))
                .thenReturn(List.of(assigned));

        var result = service().manageable(() -> "coordinator");

        assertEquals(List.of(5L), result.stream().map(item -> item.id()).toList());
        verify(committees).findByIdInAndActiveTrueAndStatusIgnoreCaseOrderByNameAsc(List.of(5L), "ACTIVE");
        verify(committees, never()).findAll();
    }

    @Test void administratorCommitteeOptionsIncludeOnlyActiveCommittees() {
        User administrator = new User();
        Role role = new Role();
        role.setCode("ADMINISTRATOR");
        administrator.setRole(role);
        Committee active = new Committee();
        active.setId(6L);
        active.setName("Active Committee");
        active.setActive(true);
        when(authorization.authenticatedUser(any())).thenReturn(administrator);
        when(committees.findByActiveTrueAndStatusIgnoreCaseOrderByNameAsc("ACTIVE"))
                .thenReturn(List.of(active));

        var result = service().manageable(() -> "administrator");

        assertEquals(List.of(6L), result.stream().map(committeeResponse -> committeeResponse.id()).toList());
        verify(committees).findByActiveTrueAndStatusIgnoreCaseOrderByNameAsc("ACTIVE");
        verify(committees, never()).findAll();
    }

    @Test void removingMemberClosesHistoryRowInsteadOfDeletingIt() {
        Committee committee = new Committee(); committee.setId(5L);
        User user = new User(); user.setId(7L);
        CommitteeMember membership = new CommitteeMember(); membership.setId(19L); membership.setCommittee(committee);
        membership.setUser(user); membership.setJoinedOn(LocalDate.now().minusDays(4)); membership.setMembershipStatus("ACTIVE");
        when(committees.findByIdForUpdate(5L)).thenReturn(Optional.of(committee));
        when(memberships.findFirstByCommitteeIdAndUserIdAndMembershipStatusIgnoreCase(5L, 7L, "ACTIVE"))
                .thenReturn(Optional.of(membership));

        service()
                .removeMember(5L, 7L, () -> "coordinator");

        assertEquals("INACTIVE", membership.getMembershipStatus());
        assertEquals(LocalDate.now(), membership.getLeftOn());
        assertEquals(19L, membership.getId());
        verify(memberships, never()).delete(any());
    }

    @Test void rejectsDuplicateActiveMemberBeforeInsert() {
        Committee committee = activeCommittee(5L);
        User user = studentUser(7L);
        when(committees.findByIdForUpdate(5L)).thenReturn(Optional.of(committee));
        when(users.findById(7L)).thenReturn(Optional.of(user));
        when(memberships.existsByCommitteeIdAndUserIdAndMembershipStatusIgnoreCase(5L, 7L, "ACTIVE")).thenReturn(true);

        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> service().addMember(5L, new CommitteeMemberRequest(7L, "MEMBER"), () -> "coordinator"));
        verify(memberships, never()).saveAndFlush(any());
    }

    @Test void rejoiningCreatesAnotherMembershipPeriodAfterThePriorOneWasClosed() {
        Committee committee = activeCommittee(5L);
        User user = studentUser(7L);
        when(committees.findByIdForUpdate(5L)).thenReturn(Optional.of(committee));
        when(users.findById(7L)).thenReturn(Optional.of(user));
        when(memberships.existsByCommitteeIdAndUserIdAndMembershipStatusIgnoreCase(5L, 7L, "ACTIVE")).thenReturn(false);
        when(memberships.saveAndFlush(any(CommitteeMember.class))).thenAnswer(call -> {
            CommitteeMember newPeriod = call.getArgument(0); newPeriod.setId(27L); return newPeriod;
        });

        var response = service().addMember(5L, new CommitteeMemberRequest(7L, "MEMBER"), () -> "coordinator");

        assertEquals(27L, response.id());
        assertEquals(true, response.active());
        verify(memberships).saveAndFlush(any(CommitteeMember.class));
    }

    @Test void scopedCoordinatorCannotGrantCoordinatorPrivileges() {
        Committee committee = activeCommittee(5L);
        User actor = new User(); actor.setId(11L); Role actorRole = new Role(); actorRole.setCode("STUDENT"); actor.setRole(actorRole);
        User target = studentUser(7L);
        when(committees.findByIdForUpdate(5L)).thenReturn(Optional.of(committee));
        when(authorization.requireAdministratorOrCommitteeCoordinator(any(), eq(5L))).thenReturn(actor);
        when(users.findById(7L)).thenReturn(Optional.of(target));

        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> service().addMember(5L, new CommitteeMemberRequest(7L, "COORDINATOR"), () -> "coordinator"));
        verify(memberships, never()).saveAndFlush(any(CommitteeMember.class));
    }

    @Test void administratorCanAssignExistingStudentAsScopedCoordinatorWithoutChangingTheirAccountRole() {
        Committee committee = activeCommittee(5L);
        User actor = new User(); actor.setId(11L); Role adminRole = new Role(); adminRole.setCode("ADMINISTRATOR"); actor.setRole(adminRole);
        User student = studentUser(7L);
        when(committees.findByIdForUpdate(5L)).thenReturn(Optional.of(committee));
        when(authorization.requireAdministratorOrCommitteeCoordinator(any(), eq(5L))).thenReturn(actor);
        when(users.findById(7L)).thenReturn(Optional.of(student));
        when(memberships.existsByCommitteeIdAndUserIdAndMembershipStatusIgnoreCase(5L, 7L, "ACTIVE"))
                .thenReturn(false);
        when(memberships.saveAndFlush(any(CommitteeMember.class))).thenAnswer(call -> {
            CommitteeMember membership = call.getArgument(0); membership.setId(30L); return membership;
        });

        var response = service().addMember(
                5L, new CommitteeMemberRequest(7L, "COORDINATOR"), () -> "admin");

        assertEquals("STUDENT", student.getRole().getCode());
        assertEquals("COORDINATOR", response.memberRole());
        assertEquals(true, response.active());
    }

    @Test void rejectsAddingMembersToInactiveCommittees() {
        Committee committee = new Committee();
        committee.setId(5L);
        committee.setActive(false);
        committee.setStatus("INACTIVE");
        when(committees.findByIdForUpdate(5L)).thenReturn(Optional.of(committee));
        when(authorization.requireAdministratorOrCommitteeCoordinator(any(), eq(5L)))
                .thenReturn(new User());

        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> service().addMember(5L, new CommitteeMemberRequest(7L, "MEMBER"), () -> "admin"));
        verify(users, never()).findById(7L);
    }

    @Test void rejectsInactiveStudentProfilesFromCommitteeMembership() {
        Committee committee = activeCommittee(5L);
        User student = studentUser(7L);
        student.getStudent().setAcademicStatus("INACTIVE");
        when(committees.findByIdForUpdate(5L)).thenReturn(Optional.of(committee));
        when(users.findById(7L)).thenReturn(Optional.of(student));

        assertThrows(IllegalArgumentException.class,
                () -> service().addMember(5L, new CommitteeMemberRequest(7L, "MEMBER"), () -> "admin"));
        verify(memberships, never()).saveAndFlush(any());
    }

    @Test void departmentFilterIncludesFacultyMembersFromTheirDepartment() {
        Committee committee = new Committee(); committee.setId(5L);
        Department department = new Department(); department.setId(3L);
        Faculty facultyProfile = new Faculty(); facultyProfile.setDepartment(department);
        User user = new User(); user.setId(7L); user.setUsername("faculty"); user.setFaculty(facultyProfile);
        CommitteeMember member = new CommitteeMember(); member.setId(18L); member.setCommittee(committee);
        member.setUser(user); member.setJoinedOn(LocalDate.now()); member.setMembershipStatus("ACTIVE");
        when(committees.findById(5L)).thenReturn(Optional.of(committee));
        when(memberships.findByCommitteeIdAndMembershipStatusIgnoreCaseOrderByJoinedOnAsc(5L, "ACTIVE"))
                .thenReturn(java.util.List.of(member));

        var result = service().members(5L, () -> "coordinator", 3L, null);

        assertEquals(1, result.size());
        assertEquals(18L, result.getFirst().id());
    }

    private CommitteeService service() {
        return new CommitteeService(committees, departments, memberships, users, authorization, faculty);
    }

    private Faculty facultyProfile(long facultyId, long userId) {
        Role role = new Role(); role.setCode("FACULTY");
        User user = new User(); user.setId(userId); user.setActive(true); user.setRole(role);
        Faculty profile = new Faculty(); profile.setId(facultyId); profile.setUser(user); profile.setEmploymentStatus("ACTIVE");
        return profile;
    }

    private Committee activeCommittee(long id) {
        Committee committee = new Committee();
        committee.setId(id);
        committee.setActive(true);
        committee.setStatus("ACTIVE");
        return committee;
    }

    private User studentUser(long id) {
        Role role = new Role(); role.setCode("STUDENT");
        User user = new User(); user.setId(id); user.setRole(role); user.setActive(true);
        Student profile = new Student(); profile.setUser(user); profile.setAcademicStatus("ACTIVE"); user.setStudent(profile);
        return user;
    }
}
