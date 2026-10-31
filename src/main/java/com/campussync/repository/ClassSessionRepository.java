package com.campussync.repository;

import com.campussync.entity.ClassSession;
import java.util.List;
import java.util.Optional;
import java.time.LocalDate;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClassSessionRepository extends JpaRepository<ClassSession, Long> {
    @EntityGraph(attributePaths = {"subject", "subject.department", "faculty", "faculty.user", "department"})
    List<ClassSession> findBySubjectIdAndFacultyIdOrderBySessionDateDescStartTimeDesc(Long subjectId, Long facultyId);

    @EntityGraph(attributePaths = {"subject", "subject.department", "faculty", "faculty.user", "department"})
    Optional<ClassSession> findById(Long id);

    @EntityGraph(attributePaths = {"subject", "subject.department", "faculty", "faculty.user",
            "department", "timetableEntry"})
    Optional<ClassSession> findByTimetableEntryIdAndSessionDate(Long timetableEntryId, LocalDate sessionDate);
}
