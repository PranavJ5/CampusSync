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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.PastOrPresent;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "faculty", indexes = @Index(name = "idx_faculty_department_id", columnList = "department_id"))
@Getter
@Setter
@NoArgsConstructor
public class Faculty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "faculty_id")
    private Long id;

    @NotNull
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @NotBlank
    @Size(max = 100)
    @Column(name = "employee_number", nullable = false, unique = true, length = 100)
    private String employeeNumber;

    @NotBlank
    @Size(max = 100)
    @Column(name = "designation", nullable = false, length = 100)
    private String designation;

    @NotBlank
    @Size(max = 50)
    @Column(name = "employment_status", nullable = false, length = 50)
    private String employmentStatus;

    @Size(max = 150)
    @Column(name = "qualification", length = 150)
    private String qualification;

    @Size(max = 20)
    @Column(name = "contact_number", length = 20)
    private String contactNumber;

    @PastOrPresent
    @Column(name = "date_of_joining")
    private java.time.LocalDate dateOfJoining;
}
