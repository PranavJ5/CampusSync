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
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "student", indexes = @Index(name = "idx_student_department_id", columnList = "department_id"))
@Getter
@Setter
@NoArgsConstructor
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "student_id")
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
    @Column(name = "enrollment_number", nullable = false, unique = true, length = 100)
    private String enrollmentNumber;

    @NotNull
    @PastOrPresent
    @Column(name = "admission_date", nullable = false)
    private LocalDate admissionDate;

    @jakarta.validation.constraints.Min(1)
    @jakarta.validation.constraints.Max(8)
    @Column(name = "current_semester")
    private Integer currentSemester;

    @Min(1900) @Max(2100)
    @Column(name = "admission_year")
    private Integer admissionYear;

    @PastOrPresent
    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Pattern(regexp = "^$|^[+]?[0-9() .-]{7,20}$")
    @Column(name = "contact_number", length = 20)
    private String contactNumber;

    @Email @Size(max = 255)
    @Column(name = "alternate_email", length = 255)
    private String alternateEmail;

    @Size(max = 30)
    @Column(name = "gender", length = 30)
    private String gender;

    @Size(max = 500)
    @Column(name = "photo_path", length = 500)
    private String photoPath;

    @NotBlank
    @Size(max = 50)
    @Column(name = "academic_status", nullable = false, length = 50)
    private String academicStatus;
}
