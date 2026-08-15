package com.campussync.repository;

import com.campussync.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {

    Optional<Student> findByUserId(Long userId);

    List<Student> findByDepartmentId(Long departmentId);

    Optional<Student> findByEnrollmentNumber(String enrollmentNumber);
}
