package com.campussync.repository;

import com.campussync.entity.CommitteeMember;
import com.campussync.entity.CommitteeMemberId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommitteeMemberRepository extends JpaRepository<CommitteeMember, CommitteeMemberId> {

    List<CommitteeMember> findByCommitteeId(Long committeeId);

    List<CommitteeMember> findByUserId(Long userId);
}
