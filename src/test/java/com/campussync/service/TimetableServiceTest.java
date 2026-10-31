package com.campussync.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campussync.dto.TimetableEntryRequest;
import com.campussync.entity.Department;
import com.campussync.entity.Faculty;
import com.campussync.entity.Student;
import com.campussync.entity.Subject;
import com.campussync.entity.TimetableEntry;
import com.campussync.entity.User;
import com.campussync.repository.DepartmentRepository;
import com.campussync.repository.FacultyRepository;
import com.campussync.repository.StudentRepository;
import com.campussync.repository.SubjectRepository;
import com.campussync.repository.TimetableEntryRepository;
import java.security.Principal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class TimetableServiceTest {
    @Mock AuthorizationService authorization;
    @Mock StudentRepository students;
    @Mock FacultyRepository faculty;
    @Mock DepartmentRepository departments;
    @Mock SubjectRepository subjects;
    @Mock TimetableEntryRepository entries;
    private TimetableService service;
    private final Principal studentPrincipal = () -> "student";
    private final Principal facultyPrincipal = () -> "faculty";
    private final Principal adminPrincipal = () -> "admin";
    private Department department;
    private Faculty facultyProfile;
    private Subject subject;

    @BeforeEach
    void setUp() {
        service = new TimetableService(authorization, students, faculty, departments, subjects, entries);
        department = new Department();
        department.setId(3L);
        department.setName("Computer Science");
        User facultyUser = new User();
        facultyUser.setId(10L);
        facultyUser.setFirstName("Ada");
        facultyUser.setLastName("Lecturer");
        facultyProfile = new Faculty();
        facultyProfile.setId(5L);
        facultyProfile.setUser(facultyUser);
        subject = new Subject();
        subject.setId(7L);
        subject.setCode("CS201");
        subject.setName("Algorithms");
        subject.setDepartment(department);
        subject.setSemester(5);
        subject.setDivision("B");
        subject.setFaculty(facultyProfile);
    }

    @Test
    void studentDashboardUsesOnlyAuthenticatedStudentGroup() {
        User account = new User();
        account.setId(21L);
        Student profile = new Student();
        profile.setDepartment(department);
        profile.setCurrentSemester(5);
        profile.setDivision("B");
        when(authorization.requireRole(studentPrincipal, "STUDENT")).thenReturn(account);
        when(students.findByUserId(21L)).thenReturn(Optional.of(profile));
        when(entries.findByDepartmentIdAndSemesterAndDivisionIgnoreCaseAndEffectiveFromLessThanEqual(
                org.mockito.ArgumentMatchers.eq(3L), org.mockito.ArgumentMatchers.eq(5),
                org.mockito.ArgumentMatchers.eq("B"), any())).thenReturn(List.of(entry(11L, DayOfWeek.MONDAY, LocalTime.of(9, 0))));

        var result = service.studentDashboard(studentPrincipal);

        assertTrue(result.timetableAvailable());
        assertEquals(1, result.weekly().size());
        verify(entries).findByDepartmentIdAndSemesterAndDivisionIgnoreCaseAndEffectiveFromLessThanEqual(
                org.mockito.ArgumentMatchers.eq(3L), org.mockito.ArgumentMatchers.eq(5),
                org.mockito.ArgumentMatchers.eq("B"), any());
    }

    @Test
    void facultyDashboardUsesOnlyTheirAssignedFacultyProfile() {
        User account = new User();
        account.setId(10L);
        when(authorization.requireRole(facultyPrincipal, "FACULTY")).thenReturn(account);
        when(faculty.findByUserId(10L)).thenReturn(Optional.of(facultyProfile));
        when(entries.findByFacultyIdAndEffectiveFromLessThanEqual(org.mockito.ArgumentMatchers.eq(5L), any()))
                .thenReturn(List.of(entry(11L, DayOfWeek.MONDAY, LocalTime.of(9, 0))));

        var result = service.facultyDashboard(facultyPrincipal);

        assertEquals(1, result.weekly().size());
        verify(entries).findByFacultyIdAndEffectiveFromLessThanEqual(org.mockito.ArgumentMatchers.eq(5L), any());
    }

    @Test
    void coordinatorCannotAccessFacultyTimetable() {
        when(authorization.requireRole(facultyPrincipal, "FACULTY"))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN));

        assertThrows(ResponseStatusException.class, () -> service.facultyDashboard(facultyPrincipal));

        verify(faculty, never()).findByUserId(any());
        verify(entries, never()).findByFacultyIdAndEffectiveFromLessThanEqual(any(), any());
    }

    @Test
    void administratorCanCreateSubjectSlotUsingAssignedSubjectRelationships() {
        when(authorization.requireRole(adminPrincipal, "ADMINISTRATOR")).thenReturn(new User());
        when(subjects.findById(7L)).thenReturn(Optional.of(subject));
        when(entries.findByDepartmentIdAndSemesterAndDivisionIgnoreCaseAndAcademicYearAndEffectiveFromAndDayOfWeek(
                3L, 5, "B", "2026-27", LocalDate.of(2026, 8, 10), DayOfWeek.MONDAY))
                .thenReturn(List.of());
        when(entries.saveAndFlush(any())).thenAnswer(invocation -> {
            TimetableEntry created = invocation.getArgument(0);
            created.setId(15L);
            return created;
        });
        var request = new TimetableEntryRequest(7L, null, null, null, null, DayOfWeek.MONDAY,
                LocalTime.of(9, 0), LocalTime.of(10, 0), "Room 1", "LECTURE", null,
                "2026-27", LocalDate.of(2026, 8, 10), true);

        var result = service.create(request, adminPrincipal);

        assertEquals(15L, result.id());
        assertEquals(3L, result.departmentId());
        assertEquals(5, result.semester());
        assertEquals("B", result.division());
        assertEquals(5L, result.facultyId());
        assertEquals("Ada Lecturer", result.facultyName());
        assertEquals("CS201", result.subjectCode());
    }

    @Test
    void nonAdministratorCannotManageTimetable() {
        when(authorization.requireRole(studentPrincipal, "ADMINISTRATOR"))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN));

        assertThrows(ResponseStatusException.class, () -> service.managementList(studentPrincipal));

        verify(entries, never()).findAllByOrderByDepartmentNameAscSemesterAscDivisionAscDayOfWeekAscStartTimeAsc();
    }

    @Test
    void rejectsOverlappingSlotsForTheSameDivision() {
        when(authorization.requireRole(adminPrincipal, "ADMINISTRATOR")).thenReturn(new User());
        when(subjects.findById(7L)).thenReturn(Optional.of(subject));
        TimetableEntry existing = entry(16L, DayOfWeek.MONDAY, LocalTime.of(9, 30));
        existing.setEndTime(LocalTime.of(10, 30));
        when(entries.findByDepartmentIdAndSemesterAndDivisionIgnoreCaseAndAcademicYearAndEffectiveFromAndDayOfWeek(
                3L, 5, "B", "2026-27", LocalDate.of(2026, 8, 10), DayOfWeek.MONDAY))
                .thenReturn(List.of(existing));
        var request = new TimetableEntryRequest(7L, null, null, null, null, DayOfWeek.MONDAY,
                LocalTime.of(9, 0), LocalTime.of(10, 0), null, "LAB", null,
                "2026-27", LocalDate.of(2026, 8, 10), true);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.create(request, adminPrincipal));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(entries, never()).saveAndFlush(any());
    }

    @Test
    void allowsOverlappingLabsForDifferentBatches() {
        when(authorization.requireRole(adminPrincipal, "ADMINISTRATOR")).thenReturn(new User());
        when(subjects.findById(7L)).thenReturn(Optional.of(subject));
        TimetableEntry existing = entry(16L, DayOfWeek.MONDAY, LocalTime.of(9, 0));
        existing.setClassType("LAB");
        existing.setLabBatch("B1");
        existing.setEndTime(LocalTime.of(11, 0));
        when(entries.findByDepartmentIdAndSemesterAndDivisionIgnoreCaseAndAcademicYearAndEffectiveFromAndDayOfWeek(
                3L, 5, "B", "2026-27", LocalDate.of(2026, 8, 10), DayOfWeek.MONDAY))
                .thenReturn(List.of(existing));
        when(entries.saveAndFlush(any())).thenAnswer(invocation -> {
            TimetableEntry created = invocation.getArgument(0);
            created.setId(17L);
            return created;
        });
        var request = new TimetableEntryRequest(7L, null, null, null, "B2", DayOfWeek.MONDAY,
                LocalTime.of(9, 0), LocalTime.of(11, 0), "Lab 2", "LAB", null,
                "2026-27", LocalDate.of(2026, 8, 10), true);

        var result = service.create(request, adminPrincipal);

        assertEquals("B2", result.labBatch());
    }

    @Test
    void rejectsOverlappingLectureInTheSameRoom() {
        when(authorization.requireRole(adminPrincipal, "ADMINISTRATOR")).thenReturn(new User());
        when(subjects.findById(7L)).thenReturn(Optional.of(subject));
        TimetableEntry existing = entry(18L, DayOfWeek.MONDAY, LocalTime.of(10, 0));
        existing.setEndTime(LocalTime.of(11, 0));
        existing.setVenue("Room 204");
        when(entries.findActiveOverlappingEntries("2026-27", LocalDate.of(2026, 8, 10), DayOfWeek.MONDAY,
                LocalTime.of(10, 30), LocalTime.of(11, 30))).thenReturn(List.of(existing));
        var request = new TimetableEntryRequest(7L, null, null, null, null, DayOfWeek.MONDAY,
                LocalTime.of(10, 30), LocalTime.of(11, 30), "Room 204", "LECTURE", null,
                "2026-27", LocalDate.of(2026, 8, 10), true);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.create(request, adminPrincipal));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        assertTrue(error.getReason().contains("room"));
        verify(entries, never()).saveAndFlush(any());
    }

    @Test
    void facultyCanScheduleAnAssignedLectureWithRoom() {
        User facultyUser = new User();
        facultyUser.setId(31L);
        when(authorization.requireRole(facultyPrincipal, "FACULTY")).thenReturn(facultyUser);
        facultyProfile.setId(41L);
        when(faculty.findByUserId(31L)).thenReturn(Optional.of(facultyProfile));
        subject.setFaculty(facultyProfile);
        when(subjects.findById(7L)).thenReturn(Optional.of(subject));
        when(entries.saveAndFlush(any())).thenAnswer(invocation -> {
            TimetableEntry created = invocation.getArgument(0);
            created.setId(19L);
            return created;
        });
        var request = new TimetableEntryRequest(7L, null, null, null, null, DayOfWeek.TUESDAY,
                LocalTime.of(12, 0), LocalTime.of(13, 0), "Room 301", "LECTURE", null,
                "2026-27", LocalDate.of(2026, 8, 10), true);

        var result = service.createFacultyLecture(request, facultyPrincipal);

        assertEquals(19L, result.id());
        assertEquals("Room 301", result.venue());
        assertEquals("B", result.division());
        verify(entries).saveAndFlush(any());
    }

    @Test
    void facultyCannotScheduleAnUnassignedSubject() {
        User facultyUser = new User();
        facultyUser.setId(31L);
        Faculty otherFaculty = new Faculty();
        otherFaculty.setId(99L);
        when(authorization.requireRole(facultyPrincipal, "FACULTY")).thenReturn(facultyUser);
        facultyProfile.setId(41L);
        when(faculty.findByUserId(31L)).thenReturn(Optional.of(facultyProfile));
        subject.setFaculty(otherFaculty);
        when(subjects.findById(7L)).thenReturn(Optional.of(subject));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.createFacultyLecture(new TimetableEntryRequest(7L, null, null, null, null,
                        DayOfWeek.TUESDAY, LocalTime.of(12, 0), LocalTime.of(13, 0), "Room 301", "LECTURE",
                        null, "2026-27", LocalDate.of(2026, 8, 10), true), facultyPrincipal));

        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
        verify(entries, never()).saveAndFlush(any());
    }

    @Test
    void studentDashboardShowsOnlyTheStudentsAssignedBatch() {
        User account = new User();
        account.setId(21L);
        Student profile = new Student();
        profile.setDepartment(department);
        profile.setCurrentSemester(5);
        profile.setDivision("B");
        profile.setLabBatch("B2");
        TimetableEntry common = entry(11L, DayOfWeek.MONDAY, LocalTime.of(8, 0));
        common.setVenue("Room 101");
        TimetableEntry batchOne = entry(12L, DayOfWeek.MONDAY, LocalTime.of(9, 0));
        batchOne.setClassType("LAB");
        batchOne.setLabBatch("B1");
        TimetableEntry batchTwo = entry(13L, DayOfWeek.MONDAY, LocalTime.of(9, 0));
        batchTwo.setClassType("LAB");
        batchTwo.setLabBatch("B2");
        when(authorization.requireRole(studentPrincipal, "STUDENT")).thenReturn(account);
        when(students.findByUserId(21L)).thenReturn(Optional.of(profile));
        when(entries.findByDepartmentIdAndSemesterAndDivisionIgnoreCaseAndEffectiveFromLessThanEqual(
                org.mockito.ArgumentMatchers.eq(3L), org.mockito.ArgumentMatchers.eq(5),
                org.mockito.ArgumentMatchers.eq("B"), any())).thenReturn(List.of(common, batchOne, batchTwo));

        var result = service.studentDashboard(studentPrincipal);

        assertEquals(2, result.weekly().size());
        assertEquals("Room 101", result.weekly().getFirst().venue());
        assertEquals("B2", result.weekly().get(1).labBatch());
        assertFalse(result.labBatchAssignmentRequired());
    }

    @Test
    void warnsUnassignedStudentAndDoesNotShowOtherBatches() {
        User account = new User();
        account.setId(21L);
        Student profile = new Student();
        profile.setDepartment(department);
        profile.setCurrentSemester(5);
        profile.setDivision("B");
        TimetableEntry batchOne = entry(12L, DayOfWeek.MONDAY, LocalTime.of(9, 0));
        batchOne.setClassType("LAB");
        batchOne.setLabBatch("B1");
        when(authorization.requireRole(studentPrincipal, "STUDENT")).thenReturn(account);
        when(students.findByUserId(21L)).thenReturn(Optional.of(profile));
        when(entries.findByDepartmentIdAndSemesterAndDivisionIgnoreCaseAndEffectiveFromLessThanEqual(
                org.mockito.ArgumentMatchers.eq(3L), org.mockito.ArgumentMatchers.eq(5),
                org.mockito.ArgumentMatchers.eq("B"), any())).thenReturn(List.of(batchOne));

        var result = service.studentDashboard(studentPrincipal);

        assertTrue(result.labBatchAssignmentRequired());
        assertFalse(result.timetableAvailable());
        assertTrue(result.weekly().isEmpty());
    }

    @Test
    void nextClassUsesCurrentDateAndTimeAndIgnoresBreaks() {
        LocalDateTime now = LocalDateTime.of(2026, 10, 27, 10, 0);
        TimetableEntry earlierToday = entry(1L, DayOfWeek.TUESDAY, LocalTime.of(9, 0));
        TimetableEntry laterToday = entry(2L, DayOfWeek.TUESDAY, LocalTime.of(11, 0));
        TimetableEntry breakEntry = entry(3L, DayOfWeek.TUESDAY, LocalTime.of(10, 30));
        breakEntry.setClassType("BREAK");
        breakEntry.setSubject(null);
        breakEntry.setFaculty(null);
        breakEntry.setLabel("Lunch");
        TimetableEntry tomorrow = entry(4L, DayOfWeek.WEDNESDAY, LocalTime.of(8, 0));

        var result = TimetableService.dashboard(List.of(earlierToday, laterToday, breakEntry, tomorrow), now, false);

        assertEquals(2L, result.nextClass().id());
        assertEquals(LocalDateTime.of(2026, 10, 27, 11, 0), result.nextClassStartsAt());
        assertEquals(3, result.today().size());
        assertTrue(result.timetableAvailable());
    }

    @Test
    void noClassesProducesAVisibleEmptyDashboardShape() {
        var result = TimetableService.dashboard(List.of(), LocalDateTime.now(), false);
        assertNull(result.nextClass());
        assertNull(result.nextClassStartsAt());
        assertFalse(result.timetableAvailable());
        assertTrue(result.today().isEmpty());
        assertTrue(result.weekly().isEmpty());
    }

    private TimetableEntry entry(Long id, DayOfWeek day, LocalTime start) {
        TimetableEntry entry = new TimetableEntry();
        entry.setId(id);
        entry.setSubject(subject);
        entry.setFaculty(facultyProfile);
        entry.setDepartment(department);
        entry.setSemester(5);
        entry.setDivision("B");
        entry.setLabBatch("ALL");
        entry.setDayOfWeek(day);
        entry.setStartTime(start);
        entry.setEndTime(start.plusHours(1));
        entry.setClassType("LECTURE");
        entry.setAcademicYear("2026-27");
        entry.setEffectiveFrom(LocalDate.of(2026, 8, 10));
        entry.setActive(true);
        return entry;
    }
}
