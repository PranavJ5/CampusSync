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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "notification", indexes = @Index(name = "idx_notification_created_at", columnList = "created_at"))
@Getter
@Setter
@NoArgsConstructor
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long id;

    @NotBlank
    @Size(max = 200)
    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @NotBlank
    @Size(max = 2000)
    @Column(name = "message", nullable = false, length = 2000)
    private String message;

    @NotBlank
    @Size(max = 50)
    @Column(name = "category", nullable = false, length = 50)
    private String category;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Size(max = 50)
    @Column(name = "target_role", length = 50)
    private String targetRole;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_department_id")
    private Department targetDepartment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_event_id")
    private Event relatedEvent;

    @Size(max = 120)
    @Column(name = "event_notification_key", unique = true, length = 120)
    private String eventNotificationKey;
}
