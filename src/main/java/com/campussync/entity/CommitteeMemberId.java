package com.campussync.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class CommitteeMemberId implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "committee_id")
    private Long committeeId;

    @Column(name = "user_id")
    private Long userId;
}
