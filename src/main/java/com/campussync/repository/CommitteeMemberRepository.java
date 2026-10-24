package com.campussync.repository;

import com.campussync.entity.CommitteeMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommitteeMemberRepository extends JpaRepository<CommitteeMember, Long> {

    List<CommitteeMember> findByCommitteeId(Long committeeId);

    List<CommitteeMember> findByCommitteeIdOrderByJoinedOnAsc(Long committeeId);

    List<CommitteeMember> findByCommitteeDepartmentIdAndMembershipStatusIgnoreCase(Long departmentId, String status);

    List<CommitteeMember> findByCommitteeIdAndMembershipStatusIgnoreCaseOrderByJoinedOnAsc(Long committeeId, String status);

    List<CommitteeMember> findByUserIdAndMembershipStatusIgnoreCaseOrderByJoinedOnDesc(Long userId, String status);

    boolean existsByCommitteeIdAndUserIdAndMembershipStatusIgnoreCase(Long committeeId, Long userId, String status);

    java.util.Optional<CommitteeMember> findFirstByCommitteeIdAndUserIdAndMembershipStatusIgnoreCase(Long committeeId, Long userId, String status);

    List<CommitteeMember> findByUserId(Long userId);
}
