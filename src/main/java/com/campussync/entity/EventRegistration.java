package com.campussync.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "event_registration",
        indexes = {
                @Index(name = "idx_event_registration_user_id", columnList = "user_id"),
                @Index(name = "idx_event_registration_event_status", columnList = "event_id, registration_status")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class EventRegistration {

    @EmbeddedId
    @Valid
    private EventRegistrationId id;

    @NotNull
    @MapsId("eventId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @NotNull
    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @CreationTimestamp
    @Column(name = "registered_at", nullable = false, updatable = false)
    private LocalDateTime registeredAt;

    @NotBlank
    @Size(max = 50)
    @Column(name = "registration_status", nullable = false, length = 50)
    private String registrationStatus;

    @NotBlank
    @Size(max = 50)
    @Column(name = "attendance_status", nullable = false, length = 50)
    private String attendanceStatus;
}
