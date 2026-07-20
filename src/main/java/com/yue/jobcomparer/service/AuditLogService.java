package com.yue.jobcomparer.service;

import com.yue.jobcomparer.dto.AuditLogResponse;
import com.yue.jobcomparer.entity.AuditAction;
import com.yue.jobcomparer.entity.AuditLog;
import com.yue.jobcomparer.repository.AuditLogRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
        Specification<AuditLog> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (action != null) {
                predicates.add(cb.equal(root.get("action"), action));
            }
            if (email != null) {
                predicates.add(cb.equal(root.get("email"), email));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), to));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return auditLogRepository.findAll(spec, pageable).map(this::toResponse);
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
