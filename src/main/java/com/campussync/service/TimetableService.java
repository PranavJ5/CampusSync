package com.campussync.service;

import com.campussync.dto.TimetableDashboardResponse;
import com.campussync.dto.TimetableEntryRequest;
import com.campussync.dto.TimetableEntryResponse;
import com.campussync.dto.SubjectResponse;
import com.campussync.entity.Department;
import com.campussync.entity.Faculty;
import com.campussync.entity.Student;
import com.campussync.entity.Subject;
import com.campussync.entity.TimetableEntry;
import com.campussync.repository.DepartmentRepository;
import com.campussync.repository.FacultyRepository;
import com.campussync.repository.StudentRepository;
import com.campussync.repository.SubjectRepository;
import com.campussync.repository.TimetableEntryRepository;
import java.security.Principal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TimetableService {
    private static final Set<String> CLASS_TYPES = Set.of("LECTURE", "LAB", "BREAK");
    private static final Set<String> LAB_BATCHES = Set.of("B1", "B2", "B3", "B4");

    private final AuthorizationService authorization;
    private final StudentRepository students;
    private final FacultyRepository faculty;
    private final DepartmentRepository departments;
    private final SubjectRepository subjects;
    private final TimetableEntryRepository entries;

    public TimetableService(AuthorizationService authorization, StudentRepository students,
                            FacultyRepository faculty, DepartmentRepository departments,
                            SubjectRepository subjects, TimetableEntryRepository entries) {
        this.authorization = authorization;
        this.students = students;
        this.faculty = faculty;
        this.departments = departments;
        this.subjects = subjects;
        this.entries = entries;
    }

    @Transactional(readOnly = true)
    public TimetableDashboardResponse studentDashboard(Principal principal) {
        var account = authorization.requireRole(principal, "STUDENT");
        Student student = students.findByUserId(account.getId())
                .orElseThrow(() -> new NoSuchElementException("Student profile not found."));
        if (student.getCurrentSemester() == null || student.getDivision() == null
                || student.getDivision().isBlank()) {
            return dashboard(List.of(), LocalDateTime.now(), false);
        }
        LocalDateTime now = LocalDateTime.now();
        List<TimetableEntry> matching = entries
                .findByDepartmentIdAndSemesterAndDivisionIgnoreCaseAndEffectiveFromLessThanEqual(
                        student.getDepartment().getId(), student.getCurrentSemester(), student.getDivision(),
                        now.toLocalDate());
        List<TimetableEntry> current = latestEntries(matching).stream()
                .filter(TimetableEntry::isActive)
                .toList();
        boolean batchRequired = student.getLabBatch() == null || student.getLabBatch().isBlank();
        boolean hasBatchClasses = current.stream().anyMatch(TimetableService::hasSpecificLabBatch);
        List<TimetableEntry> studentSchedule = current.stream()
                .filter(entry -> !hasSpecificLabBatch(entry)
                        || (!batchRequired && entry.getLabBatch().equalsIgnoreCase(student.getLabBatch())))
                .toList();
        return dashboard(studentSchedule, now, batchRequired && hasBatchClasses);
    }

    @Transactional(readOnly = true)
    public TimetableDashboardResponse facultyDashboard(Principal principal) {
        var account = authorization.requireRole(principal, "FACULTY");
        Faculty assignedFaculty = faculty.findByUserId(account.getId())
                .orElseThrow(() -> new NoSuchElementException("Faculty profile not found."));
        LocalDateTime now = LocalDateTime.now();
        List<TimetableEntry> matching = entries
                .findByFacultyIdAndEffectiveFromLessThanEqual(assignedFaculty.getId(), now.toLocalDate());
        return dashboard(latestEntries(matching).stream().filter(TimetableEntry::isActive).toList(), now, false);
    }

    @Transactional(readOnly = true)
    public List<SubjectResponse> facultySubjects(Principal principal) {
        var account = authorization.requireRole(principal, "FACULTY");
        faculty.findByUserId(account.getId())
                .orElseThrow(() -> new NoSuchElementException("Faculty profile not found."));
        return subjects.findByFacultyUserIdOrderByNameAsc(account.getId()).stream()
                .map(SubjectResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TimetableEntryResponse> managementList(Principal principal) {
        authorization.requireRole(principal, "ADMINISTRATOR");
        return entries.findAllByOrderByDepartmentNameAscSemesterAscDivisionAscDayOfWeekAscStartTimeAsc()
                .stream().map(TimetableEntryResponse::from).toList();
    }

    @Transactional
    public TimetableEntryResponse create(TimetableEntryRequest request, Principal principal) {
        authorization.requireRole(principal, "ADMINISTRATOR");
        return save(new TimetableEntry(), request);
    }

    @Transactional
    public TimetableEntryResponse createFacultyLecture(TimetableEntryRequest request, Principal principal) {
        var account = authorization.requireRole(principal, "FACULTY");
        Faculty assignedFaculty = faculty.findByUserId(account.getId())
                .orElseThrow(() -> new NoSuchElementException("Faculty profile not found."));
        if (!"LECTURE".equalsIgnoreCase(request.classType())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Faculty accounts can schedule lectures only.");
        }
        if (request.subjectId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A lecture must have an assigned subject.");
        }
        Subject subject = subjects.findById(request.subjectId())
                .orElseThrow(() -> new NoSuchElementException("Subject not found."));
        if (subject.getFaculty() == null || !assignedFaculty.getId().equals(subject.getFaculty().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Faculty can schedule only their assigned subjects.");
        }
        return save(new TimetableEntry(), request);
    }

    @Transactional
    public TimetableEntryResponse update(Long id, TimetableEntryRequest request, Principal principal) {
        authorization.requireRole(principal, "ADMINISTRATOR");
        TimetableEntry entry = entries.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Timetable entry not found."));
        return save(entry, request);
    }

    private TimetableEntryResponse save(TimetableEntry entry, TimetableEntryRequest request) {
        String classType = request.classType() == null ? "" : request.classType().trim().toUpperCase(java.util.Locale.ROOT);
        if (!CLASS_TYPES.contains(classType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Class type must be LECTURE, LAB, or BREAK.");
        }
        String labBatch = request.labBatch() == null || request.labBatch().isBlank()
                ? null : request.labBatch().trim().toUpperCase(java.util.Locale.ROOT);
        if (labBatch != null && (!"LAB".equals(classType) || !LAB_BATCHES.contains(labBatch))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A lab batch can be assigned only to labs and must be B1, B2, B3, or B4.");
        }
        if (request.dayOfWeek() == null || request.startTime() == null || request.endTime() == null
                || !request.endTime().isAfter(request.startTime())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Choose a weekday and ensure the end time is after the start time.");
        }
        if (request.dayOfWeek() == DayOfWeek.SATURDAY || request.dayOfWeek() == DayOfWeek.SUNDAY) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Timetable entries must be Monday to Friday.");
        }
        if (request.academicYear() == null || request.academicYear().isBlank() || request.effectiveFrom() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Academic year and effective date are required.");
        }
        if ("LECTURE".equals(classType) && (request.venue() == null || request.venue().isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A lecture must have a room or venue.");
        }

        Department department;
        Subject subject = null;
        Faculty assignedFaculty = null;
        Integer semester;
        String division;
        String label = request.label() == null ? null : request.label().trim();

        if ("BREAK".equals(classType)) {
            if (request.subjectId() != null || request.departmentId() == null
                    || request.semester() == null || request.division() == null || request.division().isBlank()
                    || label == null || label.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "A break needs a department, semester, division, and label, but no subject.");
            }
            department = departments.findById(request.departmentId())
                    .orElseThrow(() -> new NoSuchElementException("Department not found."));
            semester = request.semester();
            division = request.division().trim();
        } else {
            if (request.subjectId() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A lecture or lab must have an assigned subject.");
            }
            subject = subjects.findById(request.subjectId())
                    .orElseThrow(() -> new NoSuchElementException("Subject not found."));
            department = subject.getDepartment();
            semester = subject.getSemester();
            division = subject.getDivision();
            assignedFaculty = subject.getFaculty();
            if (assignedFaculty == null || semester == null || division == null || division.isBlank()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "The subject needs an assigned faculty, semester, and division before it can be scheduled.");
            }
            if ((request.departmentId() != null && !request.departmentId().equals(department.getId()))
                    || (request.semester() != null && !request.semester().equals(semester))
                    || (request.division() != null && !request.division().equalsIgnoreCase(division))) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Timetable department, semester, and division must match the subject assignment.");
            }
            if (label != null && label.isBlank()) label = null;
        }
        if (semester < 1 || semester > 8) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Semester must be between 1 and 8.");
        }

        List<TimetableEntry> sameSchedule = entries
                .findByDepartmentIdAndSemesterAndDivisionIgnoreCaseAndAcademicYearAndEffectiveFromAndDayOfWeek(
                        department.getId(), semester, division, request.academicYear().trim(),
                        request.effectiveFrom(), request.dayOfWeek());
        List<TimetableEntry> lockedOverlaps = entries.findActiveOverlappingEntries(
                request.academicYear().trim(), request.effectiveFrom(), request.dayOfWeek(),
                request.startTime(), request.endTime());
        Map<Long, TimetableEntry> candidates = new LinkedHashMap<>();
        for (TimetableEntry candidate : new ArrayList<>(sameSchedule == null ? List.of() : sameSchedule)) {
            if (candidate.getId() != null) candidates.put(candidate.getId(), candidate);
        }
        for (TimetableEntry candidate : new ArrayList<>(lockedOverlaps == null ? List.of() : lockedOverlaps)) {
            if (candidate.getId() != null) candidates.put(candidate.getId(), candidate);
        }
        if (Boolean.TRUE.equals(request.active() == null ? entry.isActive() : request.active())) {
            for (TimetableEntry other : candidates.values()) {
                if (entry.getId() != null && entry.getId().equals(other.getId())) continue;
                if (conflicts(other, department, semester, division, assignedFaculty, classType, labBatch,
                        request.venue(), request.startTime(), request.endTime())) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, conflictMessage(other, assignedFaculty,
                            classType, request.venue()));
                }
            }
        }

        entry.setSubject(subject);
        entry.setFaculty(assignedFaculty);
        entry.setDepartment(department);
        entry.setSemester(semester);
        entry.setDivision(division.trim());
        entry.setLabBatch(labBatch == null ? "ALL" : labBatch);
        entry.setDayOfWeek(request.dayOfWeek());
        entry.setStartTime(request.startTime());
        entry.setEndTime(request.endTime());
        entry.setVenue(request.venue() == null || request.venue().isBlank() ? null : request.venue().trim());
        entry.setClassType(classType);
        entry.setLabel(label);
        entry.setAcademicYear(request.academicYear().trim());
        entry.setEffectiveFrom(request.effectiveFrom());
        entry.setActive(request.active() == null ? true : request.active());
        return TimetableEntryResponse.from(entries.saveAndFlush(entry));
    }

    public static TimetableDashboardResponse dashboard(
            List<TimetableEntry> timetable, LocalDateTime now, boolean labBatchAssignmentRequired) {
        List<TimetableEntryResponse> weekly = timetable.stream()
                .map(TimetableEntryResponse::from)
                .sorted(Comparator.comparingInt((TimetableEntryResponse item) -> item.dayOfWeek().getValue())
                        .thenComparing(TimetableEntryResponse::startTime))
                .toList();
        List<TimetableEntryResponse> today = weekly.stream()
                .filter(item -> item.dayOfWeek() == now.getDayOfWeek())
                .toList();
        var next = weekly.stream()
                .filter(item -> !"BREAK".equals(item.classType()))
                .map(item -> {
                    int daysAhead = (item.dayOfWeek().getValue() - now.getDayOfWeek().getValue() + 7) % 7;
                    LocalDateTime startsAt = now.toLocalDate().plusDays(daysAhead).atTime(item.startTime());
                    if (!startsAt.isAfter(now)) startsAt = startsAt.plusWeeks(1);
                    return new java.util.AbstractMap.SimpleImmutableEntry<>(item, startsAt);
                })
                .min(Comparator.comparing(java.util.Map.Entry::getValue))
                .orElse(null);
        return new TimetableDashboardResponse(next == null ? null : next.getKey(),
                next == null ? null : next.getValue(), today, weekly, !weekly.isEmpty(),
                labBatchAssignmentRequired);
    }

    private static boolean separateLabBatches(TimetableEntry existing, String classType, String labBatch) {
        return "LAB".equals(classType)
                && "LAB".equalsIgnoreCase(existing.getClassType())
                && hasSpecificLabBatch(existing)
                && labBatch != null
                && !existing.getLabBatch().equalsIgnoreCase(labBatch);
    }

    private static boolean conflicts(TimetableEntry other, Department department, Integer semester, String division,
                                    Faculty assignedFaculty, String classType, String labBatch, String venue,
                                    java.time.LocalTime startTime, java.time.LocalTime endTime) {
        if (!startTime.isBefore(other.getEndTime()) || !other.getStartTime().isBefore(endTime)) return false;
        if (sameVenue(venue, other.getVenue())) return true;
        if (("LECTURE".equals(classType) || "LECTURE".equalsIgnoreCase(other.getClassType()))
                && assignedFaculty != null && other.getFaculty() != null
                && assignedFaculty.getId().equals(other.getFaculty().getId())) return true;
        return department.getId().equals(other.getDepartment().getId())
                && semester.equals(other.getSemester())
                && division.equalsIgnoreCase(other.getDivision())
                && !separateLabBatches(other, classType, labBatch);
    }

    private static boolean sameVenue(String requested, String existing) {
        return requested != null && !requested.isBlank() && existing != null && !existing.isBlank()
                && requested.trim().equalsIgnoreCase(existing.trim());
    }

    private static String conflictMessage(TimetableEntry other, Faculty assignedFaculty, String classType,
                                          String venue) {
        if (sameVenue(venue, other.getVenue())) return "The room is already occupied during this time.";
        if (("LECTURE".equals(classType) || "LECTURE".equalsIgnoreCase(other.getClassType()))
                && assignedFaculty != null && other.getFaculty() != null
                && assignedFaculty.getId().equals(other.getFaculty().getId()))
            return "The assigned faculty member already has a class during this time.";
        return "This class group already has an overlapping timetable entry.";
    }

    private static boolean hasSpecificLabBatch(TimetableEntry entry) {
        return "LAB".equalsIgnoreCase(entry.getClassType())
                && entry.getLabBatch() != null
                && !entry.getLabBatch().isBlank()
                && !"ALL".equalsIgnoreCase(entry.getLabBatch());
    }

    private static List<TimetableEntry> latestEntries(List<TimetableEntry> candidates) {
        if (candidates.isEmpty()) return List.of();
        var latestEffectiveDateByGroup = candidates.stream().collect(Collectors.toMap(
                TimetableService::groupKey, TimetableEntry::getEffectiveFrom,
                (left, right) -> left.isAfter(right) ? left : right));
        return candidates.stream()
                .filter(entry -> entry.getEffectiveFrom().equals(latestEffectiveDateByGroup.get(groupKey(entry))))
                .sorted(Comparator.comparing(TimetableEntry::getDayOfWeek)
                        .thenComparing(TimetableEntry::getStartTime))
                .toList();
    }

    private static String groupKey(TimetableEntry entry) {
        return entry.getDepartment().getId() + ":" + entry.getSemester() + ":" + entry.getDivision().toUpperCase(java.util.Locale.ROOT);
    }
}
