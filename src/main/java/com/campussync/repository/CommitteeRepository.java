package com.campussync.repository;

import com.campussync.entity.Committee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CommitteeRepository extends JpaRepository<Committee, Long> {

    List<Committee> findByDepartmentId(Long departmentId);

    Optional<Committee> findByCode(String code);
}
