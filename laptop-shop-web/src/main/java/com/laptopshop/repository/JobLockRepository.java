package com.laptopshop.repository;

import com.laptopshop.entity.JobLock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface JobLockRepository extends JpaRepository<JobLock, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT j FROM JobLock j WHERE j.jobName = :name")
    Optional<JobLock> findByNameWithLock(@Param("name") String name);
}
