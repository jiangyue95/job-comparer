package com.yue.jobcomparer.controller;

import com.yue.jobcomparer.dto.AdminUserResponse;
import com.yue.jobcomparer.dto.AuditLogResponse;
import com.yue.jobcomparer.entity.AuditAction;
import com.yue.jobcomparer.service.AdminService;
import com.yue.jobcomparer.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final AuditLogService auditLogService;

    @GetMapping("/users")
    public Page<AdminUserResponse> list(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable){
        return adminService.list(pageable);
    }

    @GetMapping("/audit-logs")
    public Page<AuditLogResponse> auditLogs(
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @PageableDefault(size=50, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return auditLogService.search(action, email, from, to, pageable);
    }
}
