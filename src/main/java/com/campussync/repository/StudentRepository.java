package com.campussync.repository;

import com.campussync.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<Student> {

    Optional<Student> findByUserId(Long userId);

    List<Student> findByDepartmentId(Long departmentId);

    List<Student> findByDepartmentIdAndCurrentSemester(Long departmentId, Integer currentSemester);

    List<Student> findByDepartmentIdAndCurrentSemesterAndDivisionIgnoreCaseOrderByEnrollmentNumberAsc(
            Long departmentId, Integer currentSemester, String division);

    List<Student> findByDepartmentIdAndCurrentSemesterAndDivisionIgnoreCaseAndAcademicStatusIgnoreCaseAndUserActiveTrueOrderByEnrollmentNumberAsc(
            Long departmentId, Integer currentSemester, String division, String academicStatus);

    Optional<Student> findByEnrollmentNumber(String enrollmentNumber);

    boolean existsByEnrollmentNumberIgnoreCase(String enrollmentNumber);
}
