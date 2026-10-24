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
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(
        name = "`event`",
        indexes = {
                @Index(name = "idx_event_committee_starts_at", columnList = "committee_id, starts_at"),
                @Index(name = "idx_event_starts_at", columnList = "starts_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_id")
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "committee_id", nullable = false)
    private Committee committee;

    @NotBlank
    @Size(max = 200)
    @Column(name = "event_title", nullable = false, length = 200)
    private String title;

    @Size(max = 65535)
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @NotBlank
    @Size(max = 50)
    @Column(name = "category", nullable = false, length = 50)
    private String category;

    @NotNull
    @Column(name = "starts_at", nullable = false)
    private LocalDateTime startsAt;

    @NotNull
    @Column(name = "ends_at", nullable = false)
    private LocalDateTime endsAt;

    @NotNull
    @Column(name = "registration_deadline", nullable = false)
    private LocalDateTime registrationDeadline;

    @NotBlank
    @Size(max = 255)
    @Column(name = "venue", nullable = false, length = 255)
    private String venue;

    @NotNull
    @Positive
    @Column(name = "capacity", nullable = false)
    private Integer capacity;

    @NotBlank
    @Size(max = 50)
    @Column(name = "event_status", nullable = false, length = 50)
    private String status;

    @Size(max = 500)
    @Column(name = "poster_path", length = 500)
    private String posterPath;

    @Column(name = "cancellation_reason", length = 1000)
    private String cancellationReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "event")
    private Set<EventRegistration> registrations = new LinkedHashSet<>();
}
