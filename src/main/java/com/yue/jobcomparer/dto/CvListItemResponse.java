package com.yue.jobcomparer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CvListItemResponse {

    private Long id;
    private String cvName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
