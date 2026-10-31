package com.campussync.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campussync.entity.AttendanceChange;
import com.campussync.entity.AttendanceRecord;
import com.campussync.dto.AttendanceEntryRequest;
import com.campussync.dto.ClassAttendanceBatchRequest;
import com.campussync.dto.ClassSessionRequest;
import com.campussync.dto.ScheduledClassSessionRequest;
import com.campussync.entity.ClassSession;
import com.campussync.entity.Department;
import com.campussync.entity.Faculty;
import com.campussync.entity.Student;
import com.campussync.entity.Subject;
import com.campussync.entity.TimetableEntry;
import com.campussync.entity.User;
import com.campussync.repository.AttendanceChangeRepository;
import com.campussync.repository.AttendanceRecordRepository;
import com.campussync.repository.ClassSessionRepository;
import com.campussync.repository.FacultyRepository;
import com.campussync.repository.StudentRepository;
import com.campussync.repository.SubjectRepository;
import com.campussync.repository.TimetableEntryRepository;
import java.security.Principal;
import java.time.LocalDate;
import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceTest {
    @Mock AuthorizationService authorization;
    @Mock SubjectRepository subjects;
    @Mock FacultyRepository faculty;
    @Mock StudentRepository students;
    @Mock ClassSessionRepository sessions;
    @Mock AttendanceRecordRepository records;
    @Mock AttendanceChangeRepository changes;
    @Mock TimetableEntryRepository timetableEntries;
    private AttendanceService service;
    private final Principal professor = () -> "professor";
    private User facultyUser;
    private Faculty facultyProfile;
    private Subject subject;
    private Student student;
    private ClassSession session;

    @Test
    void newClassSessionsDefaultToScheduled() {
        assertEquals("SCHEDULED", new ClassSession().getStatus());
    }

    @BeforeEach
    void setUp() {
        service = new AttendanceService(authorization, subjects, faculty, students, sessions, records, changes, timetableEntries, 75);
        facultyUser = new User();
        facultyUser.setId(10L);
        facultyProfile = new Faculty();
        facultyProfile.setId(5L);
        facultyProfile.setUser(facultyUser);
        Department department = new Department();
        department.setId(3L);
        subject = new Subject();
        subject.setId(7L);
        subject.setName("Algorithms");
        subject.setCode("CS201");
        subject.setDepartment(department);
        subject.setSemester(3);
        subject.setDivision("B");
        subject.setFaculty(facultyProfile);
        student = new Student();
        student.setId(13L);
        student.setDepartment(department);
        student.setCurrentSemester(3);
        student.setDivision("B");
        student.setEnrollmentNumber("EN-13");
        User studentUser = new User();
        studentUser.setFirstName("Casey");
        studentUser.setLastName("Student");
        student.setUser(studentUser);
        session = new ClassSession();
        session.setId(15L);
        session.setSubject(subject);
        session.setFaculty(facultyProfile);
        session.setDepartment(subject.getDepartment());
        session.setSemester(3);
        session.setDivision("B");
        session.setSessionDate(LocalDate.now().minusDays(1));
        session.setStatus("SCHEDULED");
    }

    @Test
    void recordsCompleteRosterAndPersistsInitialAuditHistory() {
        authorizeFaculty();
        when(sessions.findById(15L)).thenReturn(Optional.of(session));
        when(students.findByDepartmentIdAndCurrentSemesterAndDivisionIgnoreCaseAndAcademicStatusIgnoreCaseAndUserActiveTrueOrderByEnrollmentNumberAsc(
                3L, 3, "B", "ACTIVE")).thenReturn(List.of(student));
        var savedRecord = new com.campussync.entity.AttendanceRecord();
        savedRecord.setStudent(student);
        savedRecord.setStatus("PRESENT");
        when(records.findByClassSessionIdOrderByStudentEnrollmentNumberAsc(15L))
                .thenReturn(List.of(), List.of(savedRecord));
        when(records.saveAllAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.recordAttendance(15L,
                new ClassAttendanceBatchRequest(List.of(new AttendanceEntryRequest(13L, "PRESENT"))), professor);

        assertEquals(1, result.size());
        assertEquals("PRESENT", result.get(0).status());
        assertEquals("HELD", session.getStatus());
        verify(records).saveAllAndFlush(any());
        verify(changes).saveAll(any());
        verify(sessions).save(session);
    }

    @Test
    void createsScheduledSessionLinkedToAssignedTimetableEntryWithoutMarkingAttendance() {
        authorizeFaculty();
        TimetableEntry timetableEntry = new TimetableEntry();
        timetableEntry.setId(33L);
        timetableEntry.setSubject(subject);
        timetableEntry.setFaculty(facultyProfile);
        timetableEntry.setDepartment(subject.getDepartment());
        timetableEntry.setSemester(3);
        timetableEntry.setDivision("B");
        timetableEntry.setLabBatch("B1");
        timetableEntry.setDayOfWeek(LocalDate.now().getDayOfWeek() == DayOfWeek.SATURDAY
                ? DayOfWeek.MONDAY : LocalDate.now().getDayOfWeek() == DayOfWeek.SUNDAY
                ? DayOfWeek.MONDAY : LocalDate.now().getDayOfWeek());
        timetableEntry.setStartTime(java.time.LocalTime.of(9, 0));
        timetableEntry.setEndTime(java.time.LocalTime.of(10, 0));
        timetableEntry.setEffectiveFrom(LocalDate.of(2020, 1, 1));
        timetableEntry.setActive(true);
        LocalDate sessionDate = LocalDate.now();
        if (sessionDate.getDayOfWeek() == DayOfWeek.SATURDAY) sessionDate = sessionDate.plusDays(2);
        if (sessionDate.getDayOfWeek() == DayOfWeek.SUNDAY) sessionDate = sessionDate.plusDays(1);
        when(timetableEntries.findById(33L)).thenReturn(Optional.of(timetableEntry));
        when(sessions.findByTimetableEntryIdAndSessionDate(33L, sessionDate)).thenReturn(Optional.empty());
        when(sessions.saveAndFlush(any())).thenAnswer(invocation -> {
            ClassSession created = invocation.getArgument(0);
            created.setId(51L);
            return created;
        });

        var result = service.createScheduledSession(33L,
                new ScheduledClassSessionRequest(sessionDate), professor);

        assertEquals(51L, result.id());
        assertEquals(33L, result.timetableEntryId());
        assertEquals("B1", result.labBatch());
        assertEquals("SCHEDULED", result.status());
        verify(records, never()).saveAllAndFlush(any());
    }

    @Test
    void scheduledSessionRosterContainsOnlyItsAssignedLabBatch() {
        authorizeFaculty();
        session.setLabBatch("B1");
        student.setLabBatch("B1");
        Student otherBatchStudent = new Student();
        otherBatchStudent.setId(14L);
        otherBatchStudent.setDepartment(subject.getDepartment());
        otherBatchStudent.setCurrentSemester(3);
        otherBatchStudent.setDivision("B");
        otherBatchStudent.setLabBatch("B2");
        otherBatchStudent.setEnrollmentNumber("EN-14");
        User otherUser = new User();
        otherUser.setFirstName("Taylor");
        otherUser.setLastName("Student");
        otherBatchStudent.setUser(otherUser);
        when(sessions.findById(15L)).thenReturn(Optional.of(session));
        when(records.findByClassSessionIdOrderByStudentEnrollmentNumberAsc(15L)).thenReturn(List.of());
        when(students.findByDepartmentIdAndCurrentSemesterAndDivisionIgnoreCaseAndAcademicStatusIgnoreCaseAndUserActiveTrueOrderByEnrollmentNumberAsc(
                3L, 3, "B", "ACTIVE")).thenReturn(List.of(student, otherBatchStudent));

        var result = service.sessionRoster(15L, professor);

        assertEquals(1, result.size());
        assertEquals(13L, result.get(0).studentId());
    }

    @Test
    void scheduledSessionMustBelongToAuthenticatedFaculty() {
        authorizeFaculty();
        Faculty otherFaculty = new Faculty();
        otherFaculty.setId(99L);
        TimetableEntry timetableEntry = new TimetableEntry();
        timetableEntry.setId(33L);
        timetableEntry.setFaculty(otherFaculty);
        timetableEntry.setActive(true);
        when(timetableEntries.findById(33L)).thenReturn(Optional.of(timetableEntry));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.createScheduledSession(33L,
                        new ScheduledClassSessionRequest(LocalDate.now()), professor));

        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
        verify(sessions, never()).saveAndFlush(any());
    }

    @Test
    void duplicateScheduledSessionForSameEntryAndDateIsRejected() {
        authorizeFaculty();
        LocalDate sessionDate = LocalDate.now();
        while (sessionDate.getDayOfWeek() == DayOfWeek.SATURDAY || sessionDate.getDayOfWeek() == DayOfWeek.SUNDAY) {
            sessionDate = sessionDate.plusDays(1);
        }
        LocalDate scheduledDate = sessionDate;
        TimetableEntry timetableEntry = new TimetableEntry();
        timetableEntry.setId(33L);
        timetableEntry.setSubject(subject);
        timetableEntry.setFaculty(facultyProfile);
        timetableEntry.setDepartment(subject.getDepartment());
        timetableEntry.setSemester(3);
        timetableEntry.setDivision("B");
        timetableEntry.setDayOfWeek(scheduledDate.getDayOfWeek());
        timetableEntry.setEffectiveFrom(LocalDate.of(2020, 1, 1));
        timetableEntry.setActive(true);
        when(timetableEntries.findById(33L)).thenReturn(Optional.of(timetableEntry));
        when(sessions.findByTimetableEntryIdAndSessionDate(33L, scheduledDate)).thenReturn(Optional.of(session));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.createScheduledSession(33L,
                        new ScheduledClassSessionRequest(scheduledDate), professor));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(sessions, never()).saveAndFlush(any());
    }

    @Test
    void linkedScheduledClassCannotBeMarkedBeforeItsScheduledEndTime() {
        authorizeFaculty();
        TimetableEntry timetableEntry = new TimetableEntry();
        session.setTimetableEntry(timetableEntry);
        session.setSessionDate(LocalDate.now());
        session.setEndTime(java.time.LocalTime.MAX);
        when(sessions.findById(15L)).thenReturn(Optional.of(session));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.recordAttendance(15L,
                        new ClassAttendanceBatchRequest(List.of(new AttendanceEntryRequest(13L, "PRESENT"))),
                        professor));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        verify(records, never()).saveAllAndFlush(any());
    }

    @Test
    void rejectsDuplicateStudentEntriesAndDoesNotWriteAttendance() {
        authorizeFaculty();
        when(sessions.findById(15L)).thenReturn(Optional.of(session));
        when(students.findByDepartmentIdAndCurrentSemesterAndDivisionIgnoreCaseAndAcademicStatusIgnoreCaseAndUserActiveTrueOrderByEnrollmentNumberAsc(
                3L, 3, "B", "ACTIVE")).thenReturn(List.of(student));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.recordAttendance(15L,
                        new ClassAttendanceBatchRequest(List.of(
                                new AttendanceEntryRequest(13L, "PRESENT"),
                                new AttendanceEntryRequest(13L, "ABSENT"))), professor));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        verify(records, never()).saveAllAndFlush(any());
    }

    @Test
    void rejectsStudentOutsideAuthorizedDivisionRoster() {
        authorizeFaculty();
        when(sessions.findById(15L)).thenReturn(Optional.of(session));
        when(students.findByDepartmentIdAndCurrentSemesterAndDivisionIgnoreCaseAndAcademicStatusIgnoreCaseAndUserActiveTrueOrderByEnrollmentNumberAsc(
                3L, 3, "B", "ACTIVE")).thenReturn(List.of(student));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.recordAttendance(15L,
                        new ClassAttendanceBatchRequest(List.of(new AttendanceEntryRequest(999L, "PRESENT"))), professor));

        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
        verify(records, never()).saveAllAndFlush(any());
    }

    @Test
    void correctionSavesPreviousAndNewStatusToAuditHistory() {
        authorizeFaculty();
        AttendanceRecord existing = new AttendanceRecord();
        existing.setStudent(student);
        existing.setStatus("ABSENT");
        when(sessions.findById(15L)).thenReturn(Optional.of(session));
        when(students.findByDepartmentIdAndCurrentSemesterAndDivisionIgnoreCaseAndAcademicStatusIgnoreCaseAndUserActiveTrueOrderByEnrollmentNumberAsc(
                3L, 3, "B", "ACTIVE")).thenReturn(List.of(student));
        when(records.findByClassSessionIdOrderByStudentEnrollmentNumberAsc(15L))
                .thenReturn(List.of(existing), List.of(existing));
        when(records.saveAllAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.recordAttendance(15L,
                new ClassAttendanceBatchRequest(List.of(new AttendanceEntryRequest(13L, "PRESENT"))), professor);

        ArgumentCaptor<List<AttendanceChange>> changesCaptor = ArgumentCaptor.forClass(List.class);
        verify(changes).saveAll(changesCaptor.capture());
        assertEquals("ABSENT", changesCaptor.getValue().get(0).getPreviousStatus());
        assertEquals("PRESENT", changesCaptor.getValue().get(0).getNewStatus());
        assertEquals("PRESENT", existing.getStatus());
    }

    @Test
    void facultyCannotAccessAnotherFacultysSessionId() {
        authorizeFaculty();
        Faculty otherFaculty = new Faculty();
        otherFaculty.setId(6L);
        session.setFaculty(otherFaculty);
        when(sessions.findById(15L)).thenReturn(Optional.of(session));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.sessionRoster(15L, professor));

        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
        verify(records, never()).findByClassSessionIdOrderByStudentEnrollmentNumberAsc(any());
    }

    @Test
    void facultyCannotCreateSessionsForAnotherInstructorsSubject() {
        authorizeFaculty();
        Faculty otherFaculty = new Faculty();
        otherFaculty.setId(6L);
        subject.setFaculty(otherFaculty);
        when(subjects.findById(7L)).thenReturn(Optional.of(subject));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.createSession(new ClassSessionRequest(7L, LocalDate.now(), null, null), professor));

        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
        verify(sessions, never()).saveAndFlush(any(ClassSession.class));
    }

    @Test
    void nonFacultyCannotReadAssignedSubjects() {
        when(authorization.requireRole(professor, "FACULTY"))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN));

        assertThrows(ResponseStatusException.class, () -> service.facultySubjects(professor));

        verify(subjects, never()).findByFacultyUserIdOrderByNameAsc(any());
    }

    @Test
    void studentCanOnlyReadTheirOwnAttendanceAndAdminReportIsProtected() {
        Principal administrator = () -> "admin";
        when(authorization.requireRole(administrator, "ADMINISTRATOR"))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN));

        assertThrows(ResponseStatusException.class, () -> service.administratorReport(administrator));

        verify(records, never()).summarizeForAdministrators();
    }

    @Test
    void studentSummaryScopesAggregateQueriesToAuthenticatedProfile() {
        Principal studentPrincipal = () -> "student";
        User account = new User();
        account.setId(21L);
        Student self = new Student();
        self.setId(22L);
        when(authorization.requireRole(studentPrincipal, "STUDENT")).thenReturn(account);
        when(students.findByUserId(21L)).thenReturn(Optional.of(self));
        when(records.summarizeByStudent(22L)).thenReturn(List.of());
        when(records.findTop100ByStudentUserIdAndClassSessionStatusIgnoreCaseOrderByClassSessionSessionDateDescClassSessionStartTimeDesc(
                21L, "HELD")).thenReturn(List.of());

        var result = service.studentSummary(studentPrincipal);

        assertEquals(0L, result.heldClasses());
        assertEquals(0L, result.attendedClasses());
        assertEquals(0, result.overallPercentage().compareTo(java.math.BigDecimal.ZERO.setScale(2)));
        verify(records).summarizeByStudent(22L);
    }

    private void authorizeFaculty() {
        when(authorization.requireRole(professor, "FACULTY")).thenReturn(facultyUser);
        when(faculty.findByUserId(10L)).thenReturn(Optional.of(facultyProfile));
    }

    @Test
    void calculatesRealAttendancePercentAndShortageWithoutDividingByZero() {
        Principal studentPrincipal = () -> "student";
        User account = new User();
        account.setId(21L);
        Student self = new Student();
        self.setId(22L);
        when(authorization.requireRole(studentPrincipal, "STUDENT")).thenReturn(account);
        when(students.findByUserId(21L)).thenReturn(Optional.of(self));
        when(records.summarizeByStudent(22L)).thenReturn(List.of(new AttendanceRecordRepository.StudentSubjectAttendanceView() {
            @Override public Long getSubjectId() { return 7L; }
            @Override public String getSubjectCode() { return "CS201"; }
            @Override public String getSubjectName() { return "Algorithms"; }
            @Override public Long getHeldClasses() { return 4L; }
            @Override public Long getAttendedClasses() { return 2L; }
        }));
        when(records.findTop100ByStudentUserIdAndClassSessionStatusIgnoreCaseOrderByClassSessionSessionDateDescClassSessionStartTimeDesc(
                21L, "HELD")).thenReturn(List.of());

        var result = service.studentSummary(studentPrincipal);

        assertEquals(0, result.overallPercentage().compareTo(java.math.BigDecimal.valueOf(50)));
        assertEquals(4L, result.heldClasses());
        assertEquals(2L, result.attendedClasses());
        assertEquals(true, result.belowThreshold());
        assertEquals(0, result.subjects().get(0).percentage().compareTo(java.math.BigDecimal.valueOf(50)));
        assertEquals(true, result.subjects().get(0).belowThreshold());
    }
}
