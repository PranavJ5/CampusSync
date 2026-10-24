package com.campussync.repository;

import com.campussync.entity.Committee;
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CommitteeRepository extends JpaRepository<Committee, Long> {

    List<Committee> findByDepartmentId(Long departmentId);

    Optional<Committee> findByCode(String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Committee c where c.id = :id")
    Optional<Committee> findByIdForUpdate(@Param("id") Long id);
}
