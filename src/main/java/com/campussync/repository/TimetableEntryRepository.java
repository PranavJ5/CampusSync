package com.campussync.repository;

import com.campussync.entity.TimetableEntry;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface TimetableEntryRepository extends JpaRepository<TimetableEntry, Long> {
    @EntityGraph(attributePaths = {"subject", "subject.faculty", "subject.faculty.user",
            "faculty", "faculty.user", "department"})
    List<TimetableEntry> findAllByOrderByDepartmentNameAscSemesterAscDivisionAscDayOfWeekAscStartTimeAsc();

    @EntityGraph(attributePaths = {"subject", "subject.faculty", "subject.faculty.user",
            "faculty", "faculty.user", "department"})
    Optional<TimetableEntry> findById(Long id);

    @EntityGraph(attributePaths = {"subject", "subject.faculty", "subject.faculty.user",
            "faculty", "faculty.user", "department"})
    List<TimetableEntry> findByDepartmentIdAndSemesterAndDivisionIgnoreCaseAndEffectiveFromLessThanEqual(
            Long departmentId, Integer semester, String division, LocalDate effectiveFrom);

    @EntityGraph(attributePaths = {"subject", "subject.faculty", "subject.faculty.user", "department"})
    List<TimetableEntry> findByFacultyIdAndEffectiveFromLessThanEqual(Long facultyId, LocalDate effectiveFrom);

    List<TimetableEntry> findByDepartmentIdAndSemesterAndDivisionIgnoreCaseAndAcademicYearAndEffectiveFromAndDayOfWeek(
            Long departmentId, Integer semester, String division, String academicYear,
            LocalDate effectiveFrom, DayOfWeek dayOfWeek);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from TimetableEntry e where e.active = true "
            + "and e.academicYear = :academicYear and e.effectiveFrom = :effectiveFrom "
            + "and e.dayOfWeek = :dayOfWeek and e.startTime < :endTime and e.endTime > :startTime")
    List<TimetableEntry> findActiveOverlappingEntries(
            @Param("academicYear") String academicYear, @Param("effectiveFrom") LocalDate effectiveFrom,
            @Param("dayOfWeek") DayOfWeek dayOfWeek, @Param("startTime") java.time.LocalTime startTime,
            @Param("endTime") java.time.LocalTime endTime);
}
