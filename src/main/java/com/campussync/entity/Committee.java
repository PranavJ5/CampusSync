package com.campussync.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "committee", indexes = @Index(name = "idx_committee_department_id", columnList = "department_id"))
@Getter
@Setter
@NoArgsConstructor
public class Committee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "committee_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @NotBlank
    @Size(max = 50)
    @Column(name = "committee_code", nullable = false, unique = true, length = 50)
    private String code;

    @NotBlank
    @Size(max = 150)
    @Column(name = "committee_name", nullable = false, length = 150)
    private String name;

    @Size(max = 65535)
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @NotBlank @Size(max = 50)
    @Column(name = "category", nullable = false, length = 50)
    private String category;

    @jakarta.validation.constraints.Min(1800) @jakarta.validation.constraints.Max(9999)
    @Column(name = "established_year")
    private Integer establishedYear;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "faculty_coordinator_id")
    private Faculty facultyCoordinator;

    @Size(max = 500)
    @Column(name = "logo_path", length = 500)
    private String logoPath;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @NotBlank
    @Size(max = 50)
    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @OneToMany(mappedBy = "committee")
    private Set<CommitteeMember> members = new LinkedHashSet<>();

    @OneToMany(mappedBy = "committee")
    private Set<Event> events = new LinkedHashSet<>();
}
