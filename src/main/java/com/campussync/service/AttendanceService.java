package com.campussync.service;

import com.campussync.dto.AdministratorAttendanceReportResponse;
import com.campussync.dto.AttendanceEntryRequest;
import com.campussync.dto.AttendanceChangeResponse;
import com.campussync.dto.AttendanceRosterEntryResponse;
import com.campussync.dto.ClassAttendanceBatchRequest;
import com.campussync.dto.ClassSessionRequest;
import com.campussync.dto.ClassSessionResponse;
import com.campussync.dto.StudentAttendanceResponse;
import com.campussync.dto.ScheduledClassSessionRequest;
import com.campussync.entity.AttendanceChange;
import com.campussync.entity.AttendanceRecord;
import com.campussync.entity.ClassSession;
import com.campussync.entity.Faculty;
import com.campussync.entity.Student;
import com.campussync.entity.Subject;
import com.campussync.entity.TimetableEntry;
import com.campussync.repository.AttendanceChangeRepository;
import com.campussync.repository.AttendanceRecordRepository;
import com.campussync.repository.ClassSessionRepository;
import com.campussync.repository.FacultyRepository;
import com.campussync.repository.StudentRepository;
import com.campussync.repository.SubjectRepository;
import com.campussync.repository.TimetableEntryRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.Principal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AttendanceService {
    private final AuthorizationService authorization;
    private final SubjectRepository subjects;
    private final FacultyRepository faculty;
    private final StudentRepository students;
    private final ClassSessionRepository sessions;
    private final AttendanceRecordRepository records;
    private final AttendanceChangeRepository changes;
    private final TimetableEntryRepository timetableEntries;
    private final int thresholdPercent;

    public AttendanceService(AuthorizationService authorization, SubjectRepository subjects,
                             FacultyRepository faculty, StudentRepository students,
                             ClassSessionRepository sessions, AttendanceRecordRepository records,
                             AttendanceChangeRepository changes, TimetableEntryRepository timetableEntries,
                             @Value("${campussync.attendance.threshold-percent:75}") int thresholdPercent) {
        if (thresholdPercent < 1 || thresholdPercent > 100) {
            throw new IllegalArgumentException("Attendance threshold must be between 1 and 100.");
        }
        this.authorization = authorization;
        this.subjects = subjects;
        this.faculty = faculty;
        this.students = students;
        this.sessions = sessions;
        this.records = records;
        this.changes = changes;
        this.timetableEntries = timetableEntries;
        this.thresholdPercent = thresholdPercent;
    }

    @Transactional(readOnly = true)
    public List<com.campussync.dto.SubjectResponse> facultySubjects(Principal principal) {
        Faculty assignedFaculty = authenticatedFaculty(principal);
        return subjects.findByFacultyUserIdOrderByNameAsc(assignedFaculty.getUser().getId()).stream()
                .map(com.campussync.dto.SubjectResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<AttendanceRosterEntryResponse> facultyRoster(Long subjectId, Principal principal) {
        Subject subject = authorizedSubject(subjectId, principal);
        return activeRoster(subject).stream()
                .map(student -> AttendanceRosterEntryResponse.from(student, null)).toList();
    }

    @Transactional(readOnly = true)
    public List<ClassSessionResponse> facultySessions(Long subjectId, Principal principal) {
        Subject subject = authorizedSubject(subjectId, principal);
        return sessions.findBySubjectIdAndFacultyIdOrderBySessionDateDescStartTimeDesc(
                        subjectId, subject.getFaculty().getId()).stream()
                .map(ClassSessionResponse::from).toList();
    }

    @Transactional
    public ClassSessionResponse createSession(ClassSessionRequest request, Principal principal) {
        Subject subject = authorizedSubject(request.subjectId(), principal);
        requireDivisionGroup(subject);
        if ((request.startTime() == null) != (request.endTime() == null)
                || (request.startTime() != null && !request.endTime().isAfter(request.startTime()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Provide both session times and ensure the end is after the start.");
        }
        ClassSession session = new ClassSession();
        session.setSubject(subject);
        session.setFaculty(authenticatedFaculty(principal));
        session.setDepartment(subject.getDepartment());
        session.setSemester(subject.getSemester());
        session.setDivision(subject.getDivision().trim());
        session.setSessionDate(request.sessionDate());
        session.setStartTime(request.startTime());
        session.setEndTime(request.endTime());
        session.setStatus("SCHEDULED");
        return ClassSessionResponse.from(sessions.saveAndFlush(session));
    }

    @Transactional
    public ClassSessionResponse createScheduledSession(
            Long timetableEntryId, ScheduledClassSessionRequest request, Principal principal) {
        Faculty assignedFaculty = authenticatedFaculty(principal);
        TimetableEntry entry = timetableEntries.findById(timetableEntryId)
                .orElseThrow(() -> new NoSuchElementException("Timetable entry not found."));
        if (!entry.isActive() || entry.getSubject() == null || entry.getFaculty() == null
                || !assignedFaculty.getId().equals(entry.getFaculty().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Faculty may create sessions only for their active assigned classes.");
        }
        if (request.sessionDate().getDayOfWeek() != entry.getDayOfWeek()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "The session date must match the scheduled day of the week.");
        }
        if (request.sessionDate().isBefore(entry.getEffectiveFrom())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "The session date is before this timetable entry became effective.");
        }
        var existing = sessions.findByTimetableEntryIdAndSessionDate(timetableEntryId, request.sessionDate());
        if (existing.isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A class session already exists for this scheduled class and date.");
        }

        ClassSession session = new ClassSession();
        session.setSubject(entry.getSubject());
        session.setFaculty(assignedFaculty);
        session.setDepartment(entry.getDepartment());
        session.setSemester(entry.getSemester());
        session.setDivision(entry.getDivision());
        session.setLabBatch("ALL".equalsIgnoreCase(entry.getLabBatch()) ? null : entry.getLabBatch());
        session.setSessionDate(request.sessionDate());
        session.setStartTime(entry.getStartTime());
        session.setEndTime(entry.getEndTime());
        session.setTimetableEntry(entry);
        session.setStatus("SCHEDULED");
        return ClassSessionResponse.from(sessions.saveAndFlush(session));
    }

    @Transactional(readOnly = true)
    public List<AttendanceRosterEntryResponse> sessionRoster(Long sessionId, Principal principal) {
        ClassSession session = authorizedSession(sessionId, principal);
        Map<Long, AttendanceRecord> existing = records.findByClassSessionIdOrderByStudentEnrollmentNumberAsc(sessionId)
                .stream().collect(Collectors.toMap(record -> record.getStudent().getId(), Function.identity()));
        return activeRoster(session).stream()
                .sorted(java.util.Comparator.comparing(Student::getEnrollmentNumber))
                .map(student -> AttendanceRosterEntryResponse.from(student, existing.get(student.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AttendanceChangeResponse> sessionChanges(Long sessionId, Principal principal) {
        authorizedSession(sessionId, principal);
        return changes.findTop100ByAttendanceClassSessionIdOrderByChangedAtDesc(sessionId).stream()
                .map(AttendanceChangeResponse::from).toList();
    }

    @Transactional
    public List<AttendanceRosterEntryResponse> recordAttendance(
            Long sessionId, ClassAttendanceBatchRequest request, Principal principal) {
        ClassSession session = authorizedSession(sessionId, principal);
        if (LocalDate.now().isBefore(session.getSessionDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Attendance can be recorded only on or after the session date.");
        }
        if (session.getTimetableEntry() != null && session.getSessionDate().equals(LocalDate.now())
                && session.getEndTime() != null && java.time.LocalTime.now().isBefore(session.getEndTime())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Attendance for a scheduled class can be recorded only after its scheduled end time.");
        }
        if ("CANCELLED".equalsIgnoreCase(session.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Attendance cannot be recorded for a cancelled session.");
        }

        List<AttendanceRecord> existingRecords =
                records.findByClassSessionIdOrderByStudentEnrollmentNumberAsc(sessionId);
        Map<Long, AttendanceRecord> previous = existingRecords.stream()
                .collect(Collectors.toMap(record -> record.getStudent().getId(), Function.identity()));
        Map<Long, Student> rosterById = new HashMap<>();
        activeRoster(session)
                .forEach(student -> rosterById.put(student.getId(), student));
        existingRecords.forEach(record -> rosterById.put(record.getStudent().getId(), record.getStudent()));
        if (rosterById.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Attendance cannot be recorded until the division has active student profiles.");
        }
        List<Student> roster = rosterById.values().stream()
                .sorted(java.util.Comparator.comparing(Student::getEnrollmentNumber))
                .toList();
        Map<Long, AttendanceEntryRequest> submitted = new HashMap<>();
        for (AttendanceEntryRequest entry : request.entries()) {
            if (submitted.putIfAbsent(entry.studentId(), entry) != null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Each student may appear only once in the attendance list.");
            }
            if (!rosterById.containsKey(entry.studentId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Attendance may be recorded only for students in this subject's active division.");
            }
            String status = entry.status().trim().toUpperCase(java.util.Locale.ROOT);
            if (!Set.of("PRESENT", "ABSENT").contains(status)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Attendance status must be PRESENT or ABSENT.");
            }
        }
        if (!submitted.keySet().equals(rosterById.keySet())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Submit one attendance status for every active student in this division.");
        }

        Faculty actor = session.getFaculty();
        List<AttendanceRecord> toSave = new java.util.ArrayList<>();
        List<AttendanceChange> audit = new java.util.ArrayList<>();
        for (Student student : roster) {
            String nextStatus = submitted.get(student.getId()).status().trim().toUpperCase(java.util.Locale.ROOT);
            AttendanceRecord record = previous.get(student.getId());
            String oldStatus = record == null ? null : record.getStatus();
            if (record != null && nextStatus.equalsIgnoreCase(oldStatus)) {
                continue;
            }
            if (record == null) {
                record = new AttendanceRecord();
                record.setClassSession(session);
                record.setStudent(student);
            }
            record.setStatus(nextStatus);
            record.setRecordedBy(actor);
            toSave.add(record);
            AttendanceChange change = new AttendanceChange();
            change.setAttendance(record);
            change.setPreviousStatus(oldStatus);
            change.setNewStatus(nextStatus);
            change.setChangedBy(actor);
            audit.add(change);
        }
        if (!toSave.isEmpty()) {
            records.saveAllAndFlush(toSave);
            changes.saveAll(audit);
        }
        session.setStatus("HELD");
        sessions.save(session);
        return sessionRoster(sessionId, principal);
    }

    @Transactional(readOnly = true)
    public StudentAttendanceResponse studentSummary(Principal principal) {
        var account = authorization.requireRole(principal, "STUDENT");
        Student student = students.findByUserId(account.getId())
                .orElseThrow(() -> new NoSuchElementException("Student profile not found."));
        List<AttendanceRecordRepository.StudentSubjectAttendanceView> views =
                records.summarizeByStudent(student.getId());
        long held = views.stream().mapToLong(row -> row.getHeldClasses() == null ? 0 : row.getHeldClasses()).sum();
        long attended = views.stream().mapToLong(row -> row.getAttendedClasses() == null ? 0 : row.getAttendedClasses()).sum();
        List<StudentAttendanceResponse.SubjectAttendance> subjectRows = views.stream().map(row -> {
            long subjectHeld = safeCount(row.getHeldClasses());
            long subjectAttended = safeCount(row.getAttendedClasses());
            BigDecimal percentage = percentage(subjectAttended, subjectHeld);
            return new StudentAttendanceResponse.SubjectAttendance(row.getSubjectId(), row.getSubjectCode(),
                    row.getSubjectName(), subjectAttended, subjectHeld, percentage,
                    subjectHeld > 0 && percentage.compareTo(BigDecimal.valueOf(thresholdPercent)) < 0);
        }).toList();
        List<StudentAttendanceResponse.AttendanceHistory> history =
                records.findTop100ByStudentUserIdAndClassSessionStatusIgnoreCaseOrderByClassSessionSessionDateDescClassSessionStartTimeDesc(
                                account.getId(), "HELD").stream()
                        .map(record -> new StudentAttendanceResponse.AttendanceHistory(
                                record.getId(), record.getClassSession().getId(),
                                record.getClassSession().getSessionDate(), record.getClassSession().getStartTime(),
                                record.getClassSession().getSubject().getCode(),
                                record.getClassSession().getSubject().getName(), record.getStatus()))
                        .toList();
        BigDecimal overall = percentage(attended, held);
        return new StudentAttendanceResponse(thresholdPercent, attended, held, overall,
                held > 0 && overall.compareTo(BigDecimal.valueOf(thresholdPercent)) < 0,
                subjectRows, history);
    }

    @Transactional(readOnly = true)
    public AdministratorAttendanceReportResponse administratorReport(Principal principal) {
        authorization.requireRole(principal, "ADMINISTRATOR");
        List<AdministratorAttendanceReportResponse.Row> rows = records.summarizeForAdministrators().stream()
                .map(row -> {
                    long recorded = safeCount(row.getRecordedStudents());
                    long attended = safeCount(row.getAttendedRecords());
                    BigDecimal percentage = percentage(attended, recorded);
                    return new AdministratorAttendanceReportResponse.Row(row.getDepartmentId(), row.getDepartmentName(),
                            row.getDivision(), row.getSubjectId(), row.getSubjectCode(), row.getSubjectName(),
                            row.getFacultyId(), row.getFacultyFirstName() + " " + row.getFacultyLastName(),
                            safeCount(row.getHeldClasses()), recorded, attended, percentage,
                            recorded > 0 && percentage.compareTo(BigDecimal.valueOf(thresholdPercent)) < 0);
                }).toList();
        return new AdministratorAttendanceReportResponse(thresholdPercent, rows);
    }

    private Subject authorizedSubject(Long subjectId, Principal principal) {
        Faculty assigned = authenticatedFaculty(principal);
        Subject subject = subjects.findById(subjectId)
                .orElseThrow(() -> new NoSuchElementException("Subject not found."));
        if (subject.getFaculty() == null || !assigned.getId().equals(subject.getFaculty().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Faculty may manage attendance only for their assigned subjects.");
        }
        return subject;
    }

    private ClassSession authorizedSession(Long sessionId, Principal principal) {
        Faculty assigned = authenticatedFaculty(principal);
        ClassSession session = sessions.findById(sessionId)
                .orElseThrow(() -> new NoSuchElementException("Class session not found."));
        if (!assigned.getId().equals(session.getFaculty().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Faculty may manage attendance only for their own class sessions.");
        }
        return session;
    }

    private Faculty authenticatedFaculty(Principal principal) {
        var account = authorization.requireRole(principal, "FACULTY");
        return faculty.findByUserId(account.getId())
                .orElseThrow(() -> new NoSuchElementException("Faculty profile not found."));
    }

    private List<Student> activeRoster(Subject subject) {
        requireDivisionGroup(subject);
        return activeRoster(subject.getDepartment().getId(), subject.getSemester(), subject.getDivision());
    }

    private List<Student> activeRoster(Long departmentId, Integer semester, String division) {
        return students.findByDepartmentIdAndCurrentSemesterAndDivisionIgnoreCaseAndAcademicStatusIgnoreCaseAndUserActiveTrueOrderByEnrollmentNumberAsc(
                departmentId, semester, division.trim(), "ACTIVE");
    }

    private List<Student> activeRoster(ClassSession session) {
        List<Student> divisionRoster = activeRoster(
                session.getDepartment().getId(), session.getSemester(), session.getDivision());
        if (session.getLabBatch() == null || session.getLabBatch().isBlank()) return divisionRoster;
        return divisionRoster.stream()
                .filter(student -> session.getLabBatch().equalsIgnoreCase(student.getLabBatch()))
                .toList();
    }

    private void requireDivisionGroup(Subject subject) {
        if (subject.getSemester() == null || subject.getDivision() == null || subject.getDivision().isBlank()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Attendance requires a subject semester and division configured by an administrator.");
        }
    }

    private static long safeCount(Long count) {
        return count == null ? 0 : count;
    }

    private static BigDecimal percentage(long numerator, long denominator) {
        if (denominator == 0) return BigDecimal.ZERO.setScale(2);
        return BigDecimal.valueOf(numerator).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
    }
}
