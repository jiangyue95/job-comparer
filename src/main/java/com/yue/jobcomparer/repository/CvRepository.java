package com.yue.jobcomparer.repository;

import com.yue.jobcomparer.entity.Cv;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CvRepository extends JpaRepository<Cv, Long> {
    List<Cv> findAllByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long userId);
    long countByUserIdAndDeletedAtIsNull(Long userId);
    Optional<Cv> findByIdAndUserIdAndDeletedAtIsNull(Long id, Long userId);
}
