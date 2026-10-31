package com.campussync.repository;

import com.campussync.entity.AttendanceChange;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttendanceChangeRepository extends JpaRepository<AttendanceChange, Long> {
    @EntityGraph(attributePaths = {"attendance", "attendance.student", "attendance.student.user", "changedBy", "changedBy.user"})
    List<AttendanceChange> findTop100ByAttendanceClassSessionIdOrderByChangedAtDesc(Long sessionId);
}
