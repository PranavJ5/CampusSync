package com.campussync.dto;

import com.campussync.entity.CommitteeMember;
import java.time.LocalDate;

public record CommitteeMemberResponse(Long id, Long committeeId, Long userId, String username,
                                      String firstName, String lastName, String memberRole,
                                      LocalDate joinedOn, LocalDate leftOn, boolean active) {
    public static CommitteeMemberResponse from(CommitteeMember member) {
        return new CommitteeMemberResponse(member.getId(), member.getCommittee().getId(), member.getUser().getId(),
                member.getUser().getUsername(), member.getUser().getFirstName(), member.getUser().getLastName(),
                member.getMemberRole(), member.getJoinedOn(), member.getLeftOn(),
                "ACTIVE".equalsIgnoreCase(member.getMembershipStatus()));
    }
}
