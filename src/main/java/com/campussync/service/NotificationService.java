package com.campussync.service;

import com.campussync.entity.Event;
import com.campussync.entity.Notification;
import com.campussync.entity.NotificationRecipient;
import com.campussync.entity.User;
import com.campussync.dto.NotificationRequest;
import com.campussync.dto.NotificationPublicationResponse;
import com.campussync.dto.NotificationResponse;
import com.campussync.repository.CommitteeMemberRepository;
import com.campussync.repository.DepartmentRepository;
import com.campussync.repository.FacultyRepository;
import com.campussync.repository.NotificationRecipientRepository;
import com.campussync.repository.NotificationRepository;
import com.campussync.repository.StudentRepository;
import com.campussync.repository.UserRepository;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {
    private final NotificationRepository notifications;
    private final NotificationRecipientRepository recipients;
    private final UserRepository users;
    private final StudentRepository students;
    private final FacultyRepository faculty;
    private final CommitteeMemberRepository memberships;
    private final AuthorizationService authorization;
    private final DepartmentRepository departments;

    public NotificationService(NotificationRepository notifications, NotificationRecipientRepository recipients,
                               UserRepository users, StudentRepository students, FacultyRepository faculty,
                               CommitteeMemberRepository memberships, AuthorizationService authorization,
                               DepartmentRepository departments) {
        this.notifications = notifications;
        this.recipients = recipients;
        this.users = users; this.students = students; this.faculty = faculty; this.memberships = memberships;
        this.authorization = authorization; this.departments = departments;
    }

    @Transactional
    public void createRegistrationConfirmation(Event event, User student) {
        User sender = event.getCreatedBy() == null ? student : event.getCreatedBy();
        publishToUsers(sender, "Event Reminder", "Your registration for " + event.getTitle() + " is confirmed.",
                "EVENT_REMINDER", List.of(student), event, "STUDENT", null);
    }

    @Transactional
    public void createRegistrationCancellation(Event event, User student) {
        User sender = event.getCreatedBy() == null ? student : event.getCreatedBy();
        publishToUsers(sender, "Registration cancelled", "Your registration for " + event.getTitle() + " was cancelled.",
                "EVENT_REMINDER", List.of(student), event, "STUDENT", null);
    }

    @Transactional
    public void createEventPublished(Event event, User sender) {
        if (notifications.existsByEventNotificationKey("event-published:" + event.getId())) return;
        publishEventOnce(event, sender, "Event published", event.getTitle() + " is now open for registration.",
                "event-published:" + event.getId(), users.findByRoleCodeIgnoreCaseAndActiveTrue("STUDENT"));
    }

    @Transactional
    public void createEventCancellation(Event event, User sender, List<User> participants) {
        if (notifications.existsByEventNotificationKey("event-cancelled:" + event.getId())) return;
        publishEventOnce(event, sender, "Event cancelled: " + event.getTitle(),
                "This event has been cancelled. Reason: " + event.getCancellationReason(),
                "event-cancelled:" + event.getId(), participants);
    }

    @Transactional
    public void createEventReminder(Event event, List<User> participants) {
        String key = "event-reminder:" + event.getId();
        if (event.getCreatedBy() == null || notifications.existsByEventNotificationKey(key)) return;
        publishEventOnce(event, event.getCreatedBy(), "Event reminder: " + event.getTitle(),
                "This event starts in approximately 24 hours.", key, participants);
    }

    @Transactional
    public void createEventDetailsChanged(Event event, User sender, List<User> participants, String changedFields) {
        String key = "event-update:" + event.getId() + ":" + java.util.UUID.randomUUID();
        publishEventOnce(event, sender, "Event details updated: " + event.getTitle(),
                "The event schedule or venue has changed (" + changedFields + "). Please review the event details.",
                key, participants);
    }

    private void publishEventOnce(Event event, User sender, String title, String message, String key, List<User> targetUsers) {
        LinkedHashMap<Long, User> unique = new LinkedHashMap<>();
        targetUsers.stream().filter(User::isActive).forEach(user -> unique.put(user.getId(), user));
        if (unique.isEmpty()) return;
        Notification notification = new Notification(); notification.setTitle(title); notification.setMessage(message);
        notification.setCategory("EVENT_REMINDER"); notification.setCreatedBy(sender);
        notification.setRelatedEvent(event); notification.setEventNotificationKey(key);
        notification = notifications.saveAndFlush(notification);
        Notification saved = notification;
        recipients.saveAll(unique.values().stream().map(user -> {
            NotificationRecipient recipient = new NotificationRecipient();
            recipient.setNotification(saved); recipient.setUser(user); return recipient;
        }).toList());
    }

    @Transactional
    public NotificationPublicationResponse publish(NotificationRequest request, Principal principal) {
        User actor = authorization.authenticatedUser(principal);
        String category = request.category().trim().toUpperCase(Locale.ROOT);
        String audience = request.audience().trim().toUpperCase(Locale.ROOT);
        List<User> targetUsers;
        if ("CIRCULAR".equals(category)) {
            authorization.requireRole(principal, "ADMIN");
            targetUsers = resolveCircularAudience(audience, request);
        } else if ("COMMITTEE_ANNOUNCEMENT".equals(category)) {
            if (!"COMMITTEE".equals(audience) || request.committeeId() == null)
                throw new IllegalArgumentException("Committee announcements require a committee audience.");
            authorization.requireAdministratorOrCommitteeCoordinator(principal, request.committeeId());
            targetUsers = memberships.findByCommitteeIdAndMembershipStatusIgnoreCaseOrderByJoinedOnAsc(
                            request.committeeId(), "ACTIVE").stream()
                    .map(member -> member.getUser()).filter(User::isActive).toList();
        } else if ("ACADEMIC_UPDATE".equals(category)) {
            if (!"DEPARTMENT_SEMESTER".equals(audience) || request.departmentId() == null || request.semester() == null)
                throw new IllegalArgumentException("Academic updates require a department and semester audience.");
            if (actor.getRole() != null && "FACULTY".equalsIgnoreCase(actor.getRole().getCode())) {
                var profile = faculty.findByUserId(actor.getId()).orElseThrow(() -> new NoSuchElementException("Faculty profile not found."));
                if (!profile.getDepartment().getId().equals(request.departmentId()))
                    throw new org.springframework.web.server.ResponseStatusException(HttpStatus.FORBIDDEN,
                            "Faculty may publish academic updates only to their own department.");
            } else if (actor.getRole() == null || !"ADMIN".equalsIgnoreCase(actor.getRole().getCode())) {
                throw new org.springframework.web.server.ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Administrator or faculty role is required.");
            }
            targetUsers = students.findByDepartmentIdAndCurrentSemester(request.departmentId(), request.semester()).stream()
                    .map(student -> student.getUser()).filter(User::isActive).toList();
        } else {
            throw new IllegalArgumentException("Category must be CIRCULAR, COMMITTEE_ANNOUNCEMENT, or ACADEMIC_UPDATE.");
        }
        Notification notification = publishToUsers(actor, request.title().trim(), request.message().trim(), category,
                targetUsers, null, "ROLE".equals(audience) ? request.targetRole().trim().toUpperCase(Locale.ROOT) : null,
                List.of("DEPARTMENT", "DEPARTMENT_SEMESTER").contains(audience) ? request.departmentId() : null);
        return new NotificationPublicationResponse(notification.getId(), notification.getTitle(),
                notification.getCategory(), targetUsers.stream().map(User::getId).distinct().toList().size(), notification.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> forUser(Long userId, Pageable pageable) {
        int page = Math.max(0, pageable.getPageNumber());
        int size = Math.min(50, Math.max(1, pageable.getPageSize()));
        return recipients.findByUserIdOrderByNotificationCreatedAtDesc(userId, PageRequest.of(page, size))
                .map(NotificationResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> forUser(Long userId, Pageable pageable, String category, Boolean unread) {
        int page = Math.max(0, pageable.getPageNumber());
        int size = Math.min(50, Math.max(1, pageable.getPageSize()));
        Pageable safe = PageRequest.of(page, size, pageable.getSort());
        if (category != null && unread != null)
            return recipients.findByUserIdAndReadAndNotificationCategoryIgnoreCaseOrderByNotificationCreatedAtDesc(userId, !unread, category, safe).map(NotificationResponse::from);
        if (category != null)
            return recipients.findByUserIdAndNotificationCategoryIgnoreCaseOrderByNotificationCreatedAtDesc(userId, category, safe).map(NotificationResponse::from);
        if (unread != null)
            return recipients.findByUserIdAndReadOrderByNotificationCreatedAtDesc(userId, !unread, safe).map(NotificationResponse::from);
        return recipients.findByUserIdOrderByNotificationCreatedAtDesc(userId, safe).map(NotificationResponse::from);
    }

    @Transactional
    public NotificationResponse markRead(Long notificationId, Long userId) {
        NotificationRecipient recipient = recipients.findByNotificationIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new NoSuchElementException("Notification not found."));
        if (!recipient.isRead()) {
            recipient.setRead(true);
            recipient.setReadAt(LocalDateTime.now());
        }
        return NotificationResponse.from(recipient);
    }

    @Transactional
    public NotificationResponse open(Long notificationId, Long userId) {
        return markRead(notificationId, userId);
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long userId) { return recipients.countByUserIdAndReadFalse(userId); }

    @Transactional
    public int markAllRead(Long userId) {
        List<NotificationRecipient> unread = recipients.findByUserIdAndReadFalse(userId);
        LocalDateTime now = LocalDateTime.now();
        unread.forEach(recipient -> { recipient.setRead(true); recipient.setReadAt(now); });
        return unread.size();
    }

    private List<User> resolveCircularAudience(String audience, NotificationRequest request) {
        return switch (audience) {
            case "ALL" -> users.findByActiveTrue();
            case "ROLE" -> {
                if (request.targetRole() == null || !List.of("ADMIN", "FACULTY", "STUDENT", "COORDINATOR")
                        .stream().anyMatch(role -> role.equalsIgnoreCase(request.targetRole().trim())))
                    throw new IllegalArgumentException("A valid targetRole is required for role audience.");
                yield users.findByRoleCodeIgnoreCaseAndActiveTrue(request.targetRole().trim());
            }
            case "DEPARTMENT" -> {
                if (request.departmentId() == null) throw new IllegalArgumentException("departmentId is required.");
                if (!departments.existsById(request.departmentId())) throw new NoSuchElementException("Department not found.");
                List<User> result = new ArrayList<>();
                students.findByDepartmentId(request.departmentId()).forEach(s -> result.add(s.getUser()));
                faculty.findByDepartmentId(request.departmentId()).forEach(f -> result.add(f.getUser()));
                memberships.findByCommitteeDepartmentIdAndMembershipStatusIgnoreCase(request.departmentId(), "ACTIVE")
                        .forEach(m -> result.add(m.getUser()));
                yield result.stream().filter(User::isActive).toList();
            }
            case "DEPARTMENT_SEMESTER" -> {
                if (request.departmentId() == null || request.semester() == null)
                    throw new IllegalArgumentException("departmentId and semester are required.");
                if (!departments.existsById(request.departmentId())) throw new NoSuchElementException("Department not found.");
                yield students.findByDepartmentIdAndCurrentSemester(request.departmentId(), request.semester()).stream()
                        .map(s -> s.getUser()).filter(User::isActive).toList();
            }
            default -> throw new IllegalArgumentException("Circular audience must be ALL, ROLE, DEPARTMENT, or DEPARTMENT_SEMESTER.");
        };
    }

    private Notification publishToUsers(User sender, String title, String message, String category,
                                        List<User> targetUsers, Event event, String targetRole, Long departmentId) {
        LinkedHashMap<Long, User> unique = new LinkedHashMap<>();
        targetUsers.stream().filter(User::isActive).forEach(user -> unique.put(user.getId(), user));
        if (unique.isEmpty()) throw new IllegalStateException("The selected audience has no active recipients.");
        Notification notification = new Notification();
        notification.setTitle(title); notification.setMessage(message); notification.setCategory(category);
        notification.setCreatedBy(sender); notification.setTargetRole(targetRole); notification.setRelatedEvent(event);
        if (departmentId != null) notification.setTargetDepartment(departments.findById(departmentId)
                .orElseThrow(() -> new NoSuchElementException("Department not found.")));
        notification = notifications.saveAndFlush(notification);
        Notification savedNotification = notification;
        List<NotificationRecipient> rows = unique.values().stream().map(user -> {
            NotificationRecipient recipient = new NotificationRecipient();
            recipient.setNotification(savedNotification); recipient.setUser(user); return recipient;
        }).toList();
        recipients.saveAll(rows);
        return notification;
    }
}
