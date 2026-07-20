package com.yue.jobcomparer.dto;

import com.yue.jobcomparer.entity.AuditAction;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogResponse {
    private Long id;
    private AuditAction action;
    private Long userId;
    private String email;
    private String ipAddress;
    private LocalDateTime createdAt;
}
