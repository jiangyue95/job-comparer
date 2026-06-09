package com.yue.jobcomparer.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "analyses",
        indexes = {
                @Index(name = "idx_analysis_cv_id", columnList = "cv_id"),
                @Index(name = "idx_analysis_job_id", columnList = "job_id")
        }

)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobCvAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    // Logical Reference to CV
    @Column(name = "cv_id", nullable = false)
    private Long cvId;

    // Logical Reference to Job
    @Column(name = "job_id", nullable = false)
    private Long jobId;

    // === Below is the "business payload" carried by this association table (combined with AI output) ===

    @Column(nullable = false)
    private Integer matchScore;

    @Column(columnDefinition = "TEXT")
    private String matchedSkills;

    @Column(columnDefinition = "TEXT")
    private String missingSkills;

    @Column(columnDefinition = "TEXT")
    private String actionableFeedback;

    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    private LocalDateTime deletedAt;
}
