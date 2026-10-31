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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "class_session",
        uniqueConstraints = @UniqueConstraint(name = "uk_class_session_timetable_date",
                columnNames = {"timetable_entry_id", "session_date"}),
        indexes = {
        @Index(name = "idx_class_session_subject_date", columnList = "subject_id, session_date"),
        @Index(name = "idx_class_session_faculty_date", columnList = "faculty_id, session_date"),
        @Index(name = "idx_class_session_group", columnList = "department_id, semester, division")
})
@Getter
@Setter
@NoArgsConstructor
public class ClassSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "session_id")
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "faculty_id", nullable = false)
    private Faculty faculty;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "timetable_entry_id")
    private TimetableEntry timetableEntry;

    @NotNull
    @Column(name = "semester", nullable = false)
    private Integer semester;

    @NotBlank
    @Size(max = 20)
    @Column(name = "division", nullable = false, length = 20)
    private String division;

    @Size(max = 10)
    @Column(name = "lab_batch", length = 10)
    private String labBatch;

    @NotNull
    @Column(name = "session_date", nullable = false)
    private LocalDate sessionDate;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @NotBlank
    @Size(max = 20)
    @Column(name = "session_status", nullable = false, length = 20)
    private String status = "SCHEDULED";
}
