package com.yue.jobcomparer.service;

import com.yue.jobcomparer.dto.AuditLogResponse;
import com.yue.jobcomparer.entity.AuditAction;
import com.yue.jobcomparer.entity.AuditLog;
import com.yue.jobcomparer.repository.AuditLogRepository;
import com.yue.jobcomparer.util.SecurityUtils;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final SecurityUtils securityUtils;

    /**
     * Records a security event in its own transaction, so the entry survives a
     * rollback in the caller: a failed login must still be logged even though
     * the request itself fails.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordAuthEvent(AuditAction action, String email, Long userId, String ipAddress) {
        auditLogRepository.save(AuditLog.builder()
                .action(action)
                .email(email)
                .userId(userId)
                .ipAddress(ipAddress)
                .build());
    }

    /**
     * Records a business resource change. Requires an existing transaction and
     * joins it, so the entry is committed only if the change itself is.
     *
     * MANDATORY rather than REQUIRED: REQUIRED would silently start its own
     * transaction when called outside one, committing a log entry for an
     * operation that may still roll back. Failing fast makes the "log and
     * change are atomic" guarantee impossible to bypass by accident.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void recordResourceEvent(AuditAction action, Long resourceId) {
        auditLogRepository.save(AuditLog.builder()
                .action(action)
                .email(securityUtils.getCurrentUserEmail())
                .userId(securityUtils.getCurrentUserId())
                .resourceId(resourceId)
                .ipAddress(currentIpAddress())
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

    /**
     * Returns the client IP, or null when called outside a request thread.
     * getRemoteAddr() is correct behind Nginx because
     * server.forward-headers-strategy=framework rewrites the request with the
     * value from X-Forwarded-For before it reaches the controller.
     */
    private String currentIpAddress() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletRequestAttributes) {
            return servletRequestAttributes.getRequest().getRemoteAddr();
        }
        return null;
    }

    private AuditLogResponse toResponse(AuditLog log) {
        return new AuditLogResponse(
                log.getId(),
                log.getAction(),
                log.getUserId(),
                log.getEmail(),
                log.getResourceId(),
                log.getIpAddress(),
                log.getCreatedAt()
        );
    }
}
