package com.campussync.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "timetable_entry",
        uniqueConstraints = @UniqueConstraint(name = "uk_timetable_slot",
                columnNames = {"department_id", "semester", "division", "academic_year",
                        "effective_from", "day_of_week", "start_time", "lab_batch"}),
        indexes = {
                @Index(name = "idx_timetable_student_group", columnList = "department_id, semester, division, lab_batch, effective_from"),
                @Index(name = "idx_timetable_faculty_date", columnList = "faculty_id, effective_from")
        })
@Getter
@Setter
@NoArgsConstructor
public class TimetableEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "timetable_entry_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id")
    private Subject subject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "faculty_id")
    private Faculty faculty;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @NotNull
    @Column(name = "semester", nullable = false)
    private Integer semester;

    @NotBlank
    @Size(max = 20)
    @Column(name = "division", nullable = false, length = 20)
    private String division;

    @NotBlank
    @Size(max = 10)
    @Column(name = "lab_batch", nullable = false, length = 10)
    private String labBatch = "ALL";

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false, length = 10)
    private DayOfWeek dayOfWeek;

    @NotNull
    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @NotNull
    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Size(max = 255)
    @Column(name = "venue", length = 255)
    private String venue;

    @NotBlank
    @Size(max = 20)
    @Column(name = "class_type", nullable = false, length = 20)
    private String classType;

    @Size(max = 150)
    @Column(name = "label", length = 150)
    private String label;

    @NotBlank
    @Size(max = 20)
    @Column(name = "academic_year", nullable = false, length = 20)
    private String academicYear;

    @NotNull
    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}
