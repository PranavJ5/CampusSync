package com.campussync.repository;

import com.campussync.entity.AttendanceRecord;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {
    @EntityGraph(attributePaths = {"student", "student.user"})
    List<AttendanceRecord> findByClassSessionIdOrderByStudentEnrollmentNumberAsc(Long sessionId);

    @EntityGraph(attributePaths = {"classSession", "classSession.subject", "classSession.subject.department"})
    List<AttendanceRecord> findTop100ByStudentUserIdAndClassSessionStatusIgnoreCaseOrderByClassSessionSessionDateDescClassSessionStartTimeDesc(
            Long userId, String sessionStatus);

    @Query("select subject.id as subjectId, subject.code as subjectCode, subject.name as subjectName, "
            + "count(distinct session.id) as heldClasses, "
            + "sum(case when upper(record.status) = 'PRESENT' then 1 else 0 end) as attendedClasses "
            + "from AttendanceRecord record join record.classSession session join session.subject subject "
            + "where record.student.id = :studentId and upper(session.status) = 'HELD' "
            + "group by subject.id, subject.code, subject.name order by subject.name")
    List<StudentSubjectAttendanceView> summarizeByStudent(@Param("studentId") Long studentId);

    @Query("select department.id as departmentId, department.name as departmentName, session.division as division, "
            + "subject.id as subjectId, subject.code as subjectCode, subject.name as subjectName, "
            + "faculty.id as facultyId, "
            + "faculty.user.firstName as facultyFirstName, faculty.user.lastName as facultyLastName, "
            + "count(distinct session.id) as heldClasses, count(record.id) as recordedStudents, "
            + "sum(case when upper(record.status) = 'PRESENT' then 1 else 0 end) as attendedRecords "
            + "from ClassSession session join session.subject subject join session.department department "
            + "join session.faculty faculty join AttendanceRecord record on record.classSession = session "
            + "where upper(session.status) = 'HELD' "
            + "group by department.id, department.name, session.division, subject.id, subject.code, subject.name, "
            + "faculty.id, faculty.user.firstName, faculty.user.lastName "
            + "order by department.name, session.division, subject.name")
    List<DepartmentAttendanceView> summarizeForAdministrators();

    interface StudentSubjectAttendanceView {
        Long getSubjectId();
        String getSubjectCode();
        String getSubjectName();
        Long getHeldClasses();
        Long getAttendedClasses();
    }

    interface DepartmentAttendanceView {
        Long getDepartmentId();
        String getDepartmentName();
        String getDivision();
        Long getSubjectId();
        String getSubjectCode();
        String getSubjectName();
        Long getFacultyId();
        String getFacultyFirstName();
        String getFacultyLastName();
        Long getHeldClasses();
        Long getRecordedStudents();
        Long getAttendedRecords();
    }
}
