package com.campussync.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "committee_member", indexes = @jakarta.persistence.Index(name = "idx_committee_member_committee_status", columnList = "committee_id, membership_status"))
@Getter
@Setter
@NoArgsConstructor
public class CommitteeMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "committee_id", nullable = false)
    private Committee committee;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotBlank
    @Size(max = 100)
    @Column(name = "member_role", nullable = false, length = 100)
    private String memberRole;

    @NotNull
    @PastOrPresent
    @Column(name = "joined_on", nullable = false)
    private LocalDate joinedOn;

    @Column(name = "left_on")
    private LocalDate leftOn;

    @NotBlank
    @Size(max = 50)
    @Column(name = "membership_status", nullable = false, length = 50)
    private String membershipStatus;
}
