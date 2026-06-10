package com.yue.jobcomparer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiAnalysisResult {
    private Integer matchScore;
    private String matchedSkills;
    private String missingSkills;
    private String actionableFeedback;
}
