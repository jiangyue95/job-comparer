package com.yue.jobcomparer.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class AnalysisCreateRequest {
    @NotNull(message = "cvId is required")
    @Positive(message = "cvId must be positive")
    private Long cvId;

    @NotNull(message = "jobId is required")
    @Positive(message = "jobId must be positive")
    private Long jobId;
}
