package com.yue.jobcomparer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CvUpdateRequest {

    @NotBlank(message = "CV name is required")
    @Size(max = 100, message = "Cv name must not exceed {max} characters")
    private String cvName;

    @Size(max = 100_000, message = "Cv content must not exceed {max} characters")
    private String content;
}
