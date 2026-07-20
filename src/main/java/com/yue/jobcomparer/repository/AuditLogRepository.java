package com.yue.jobcomparer.repository;

import com.yue.jobcomparer.entity.AuditAction;
import com.yue.jobcomparer.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Query("""
           SELECT a FROM AuditLog a
           WHERE (:action IS NULL OR a.action = :action)
             AND (:email IS NULL OR a.email = :email)
             AND (:from IS NULL OR a.createdAt >= :from)
             AND (:to is NULL OR a.createdAt <= :to)
           """)
    Page<AuditLog> search(
            @Param("action")AuditAction action,
            @Param("email") String email,
            @Param("from")LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable);
}
