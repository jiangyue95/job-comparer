package com.yue.jobcomparer.dto;

import com.yue.jobcomparer.entity.JobStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class JobUpdateStatusRequest {

    @NotNull(message = "Status is required")
    private JobStatus status;
}
