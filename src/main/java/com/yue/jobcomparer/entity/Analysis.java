package com.yue.jobcomparer.entity;

import com.yue.jobcomparer.ai.AiProvider;
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
public class Analysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "ai_provider", nullable = false, length = 32)
    private AiProvider aiProvider;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    // Logical Reference to CV
    @Column(name = "cv_id", nullable = false)
    private Long cvId;

    // Logical Reference to Job
    @Column(name = "job_id", nullable = false)
    private Long jobId;

    // === Below is the "business payload" carried by this association table (combined with AI output) ===

    @Column
    private Integer matchScore;

    @Column(columnDefinition = "TEXT")
    private String matchedSkills;

    @Column(columnDefinition = "TEXT")
    private String missingSkills;

    @Column(columnDefinition = "TEXT")
    private String actionableFeedback;

    // === Snapshot of CV/Job identity at analysis time ===
    @Column(name = "cv_name")
    private String cvName;

    @Column(name = "job_title")
    private String jobTitle;

    @Column(name = "company")
    private String company;

    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    // === Async processing state ===
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private AnalysisStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "failure_reason", length = 32)
    private AnalysisFailureReason failureReason;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    private LocalDateTime deletedAt;
}
