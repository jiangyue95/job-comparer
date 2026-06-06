package com.yue.jobcomparer.dto;

import com.yue.jobcomparer.entity.JobStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class JobCreateRequest {

    @NotBlank(message = "Job title is required")
    @Size(max = 200, message = "Job title must not exceed {max} characters")
    private String jobTitle;

    @NotBlank(message = "Company is required")
    @Size(max = 200, message = "Company must not exceed {max} characters")
    private String company;

    @Size(max = 100_000, message = "Job description must not exceed {max} characters")
    private String jobDescription;

    @Size(max = 500, message = "Job URL must not exceed {max} characters")
    private String jobUrl;

    @NotNull(message = "Status is required")
    private JobStatus status;

    private LocalDateTime appliedAt;

    @Size(max = 10_000, message = "Notes must not exceed {max} characters")
    private String notes;
}
