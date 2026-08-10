package com.yue.jobcomparer.dto;

import com.yue.jobcomparer.ai.AiProvider;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisResponse {

    private Long id;
    private AiProvider aiProvider;
    private Long cvId;
    private Long jobId;
    private Integer matchScore;
    private String matchedSkills;
    private String missingSkills;
    private String actionableFeedback;
    private String cvName;
    private String jobTitle;
    private String company;
    private LocalDateTime createdAt;
}
