package com.yue.jobcomparer.repository;

import com.yue.jobcomparer.entity.Job;
import com.yue.jobcomparer.entity.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JobRepository extends JpaRepository<Job, Long> {

    List<Job> findAllByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long userId);

    List<Job> findAllByUserIdAndStatusAndDeletedAtIsNullOrderByCreatedAtDesc(Long userId, JobStatus status);

    long countByUserIdAndDeletedAtIsNull(Long userId);

    Optional<Job> findByIdAndUserIdAndDeletedAtIsNull(Long id, Long userId);
}
