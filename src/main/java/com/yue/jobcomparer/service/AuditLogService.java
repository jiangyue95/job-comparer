package com.yue.jobcomparer.service;

import com.yue.jobcomparer.dto.AuditLogResponse;
import com.yue.jobcomparer.entity.AuditAction;
import com.yue.jobcomparer.entity.AuditLog;
import com.yue.jobcomparer.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(AuditAction action, String email, Long userId, String ipAddress) {
        auditLogRepository.save(AuditLog.builder()
                .action(action)
                .email(email)
                .userId(userId)
                .ipAddress(ipAddress)
                .build());
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> search(AuditAction action,
                                         String email,
                                         LocalDateTime from,
                                         LocalDateTime to,
                                         Pageable pageable) {
        return auditLogRepository.search(action, email, from, to, pageable).map(this::toResponse);
    }

    private AuditLogResponse toResponse(AuditLog log) {
        return new AuditLogResponse(
                log.getId(),
                log.getAction(),
                log.getUserId(),
                log.getEmail(),
                log.getIpAddress(),
                log.getCreatedAt()
        );
    }
}
