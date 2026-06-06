package com.yue.jobcomparer.dto;

import com.yue.jobcomparer.entity.JobStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobDetailResponse {

    private Long id;
    private String jobTitle;
    private String company;
    private String jobDescription;
    private String jobUrl;
    private JobStatus status;
    private LocalDateTime appliedAt;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
