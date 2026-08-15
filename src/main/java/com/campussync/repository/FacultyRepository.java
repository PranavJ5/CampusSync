package com.campussync.repository;

import com.campussync.entity.Faculty;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FacultyRepository extends JpaRepository<Faculty, Long> {

    Optional<Faculty> findByUserId(Long userId);

    List<Faculty> findByDepartmentId(Long departmentId);

    Optional<Faculty> findByEmployeeNumber(String employeeNumber);
}
