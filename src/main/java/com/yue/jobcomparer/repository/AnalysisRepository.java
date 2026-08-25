package com.yue.jobcomparer.repository;

import com.yue.jobcomparer.entity.Analysis;
import com.yue.jobcomparer.entity.AnalysisStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AnalysisRepository extends JpaRepository<Analysis, Long> {

    // User views their analysis history
    List<Analysis> findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long userId);

    // User views a specific analysis by id
    Optional<Analysis> findByIdAndUserIdAndDeletedAtIsNull(Long id, Long userId);

    // Count a user's analyses created since a given time, excluding failures.
    // A failed analysis produced no value, so it must not consume quota.
    long countByUserIdAndCreatedAtAfterAndStatusNot(Long userId, LocalDateTime since, AnalysisStatus status);

    // Count all analyses created since a given time, excluding failures.
    long countByCreatedAtAfterAndStatusNot(LocalDateTime since, AnalysisStatus status);

    // Loads a pending analysis for background execution. Not scoped by
    // deletedAt: ownership was already verified at submission time.
    Optional<Analysis> findByIdAndUserId(Long id, Long userId);
}
