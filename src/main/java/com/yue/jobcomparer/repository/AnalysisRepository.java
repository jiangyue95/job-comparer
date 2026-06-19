package com.yue.jobcomparer.repository;

import com.yue.jobcomparer.entity.Analysis;
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

    // Count a user's analyses created since a given time (for per-user daily limit)
    long countByUserIdAndCreatedAtAfter(Long userId, LocalDateTime since);

    // Count all analyses created since a given time (for global daily limit)
    long countByCreatedAtAfter(LocalDateTime since);
}
